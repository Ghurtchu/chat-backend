import cats.effect.{ExitCode, IO, IOApp, Ref}
import cats.implicits.toSemigroupKOps
import com.comcast.ip4s.IpLiteralSyntax
import db.{MessageWithoutIdRepo, PartialConversationsRepo}
import domain.MessageWithoutId
import fs2.concurrent.Topic
import fs2.Stream
import org.http4s.{HttpApp, HttpRoutes}
import org.http4s.dsl.Http4sDsl
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.websocket.WebSocketBuilder2
import org.http4s.websocket.WebSocketFrame
import doobie._
import ws.Msg

import java.time.Instant
import scala.collection.concurrent.TrieMap

// psql -h localhost -p 5432 -U aghurtchumelia -d chat
// db-name: chat
// user: aghurtchumelia
// pass: password

/** WITH LastMessageConversations AS ( SELECT uc.user_id, c.conversation_id, c.conversation_name,
  * MAX(m.written_at) AS last_message_written_at FROM user_conversation uc JOIN conversation c ON
  * uc.conversation_id = c.conversation_id JOIN message m ON c.conversation_id = m.conversation_id WHERE
  * uc.user_id = 1 -- Replace :user_id with the actual user's ID GROUP BY uc.user_id, c.conversation_id,
  * c.conversation_name ) SELECT LMC.user_id, LMC.conversation_id, LMC.conversation_name, M.message_content AS
  * last_message_content, LMC.last_message_written_at AS last_message_written_at FROM LastMessageConversations
  * LMC JOIN message M ON LMC.conversation_id = M.conversation_id AND LMC.last_message_written_at =
  * M.written_at ORDER BY LMC.last_message_written_at DESC LIMIT 1;
  */

object Main extends IOApp {

  val transactor: Transactor[IO] = Transactor.fromDriverManager[IO](
    driver = "org.postgresql.Driver",
    url = "jdbc:postgresql://localhost:5432/chat",
    user = "aghurtchumelia",
    password = "password",
    logHandler = None,
  )

  val loadConvos = PartialConversationsRepo.of(transactor)
  val writeMsg = MessageWithoutIdRepo.of(transactor)

  override def run(args: List[String]): IO[ExitCode] =
    for {
      topic                      <- Topic[IO, Msg]
      loadedConversationsPerUser <- IO.ref(TrieMap.empty[String, Int]) // map(user id -> number of loaded conversations)
      _                          <- EmberServerBuilder
        .default[IO]
        .withHost(host"localhost")
        .withPort(port"8080")
        .withHttpWebSocketApp(httpApp(topic, _, loadedConversationsPerUser))
        .build
        .useForever
    } yield ExitCode.Success

  private def httpApp(
    topic: Topic[IO, Msg],
    wsb: WebSocketBuilder2[IO],
    loadedConversationsPerUser: Ref[IO, TrieMap[String, Int]],
  ): HttpApp[IO] =
    (conversations(topic, wsb, loadedConversationsPerUser) <+> conversation(topic, wsb)).orNotFound

  private def conversations(
    topic: Topic[IO, Msg],
    wsb: WebSocketBuilder2[IO],
    loadedConversationsPerUser: Ref[IO, TrieMap[String, Int]],
  ): HttpRoutes[IO] = {
    val dsl = new Http4sDsl[IO] {}
    import dsl._

    // set up ws connection = ws://host:port/chat/{userId}
    // and load n amount of conversations
    // 2 = last 2 convos
    // 10 = last 10 convos
    // and so on
    HttpRoutes.of[IO] { case GET -> Root / "conversations" / userId =>
      val send = topic
        .subscribe(maxQueued = 10)
        .evalMap {
          case Msg.LoadConversations(n) =>
            for {
              _             <- loadedConversationsPerUser.update(_ + (userId -> n))
              conversations <- loadConvos
                .load(userId.trim.toInt, n)
                .map(convos => WebSocketFrame.Text(convos.mkString("[", ",", "]"))) // TODO: serialize to Json later
            } yield conversations

          case Msg.ChatMessage(id, from, to, msg, timestamp) =>
            for {
              count  <- loadedConversationsPerUser.get.map(_.getOrElse(userId, 10))
              convos <- loadConvos
                .load(userId.trim.toInt, count)
                .map(convos => WebSocketFrame.Text(convos.mkString("[", ",", "]"))) // TODO: serialize to Json later
            } yield convos
        }
      val receive = topic
        .publish
        .compose[Stream[IO, WebSocketFrame]](_.collect {
        case WebSocketFrame.Text(msg, _) =>
          val amount = msg.trim.toInt

          Msg.LoadConversations(amount)
      })

      wsb.build(
        // Outgoing stream of WebSocket messages to send to the client
        send = send,
        // Incoming stream of Websocket messages from the client
        receive = receive,
      )
    }
  }

  private def conversation(
    topic: Topic[IO, Msg],
    wsb: WebSocketBuilder2[IO]
  ): HttpRoutes[IO] = {
    val dsl = new Http4sDsl[IO] {}
    import dsl._
    HttpRoutes.of[IO] {
      case GET -> Root / "from" / fromUserId / "to" / toUserId / "conversation" / conversationId =>
        val send = topic
          .subscribe(maxQueued = 10)
          .collect {
            case Msg.ChatMessage(_, `fromUserId`, `toUserId`, txt, _) =>
              WebSocketFrame.Text(txt)
            case Msg.ChatMessage(_, `toUserId`, `fromUserId`, txt, _) =>
              WebSocketFrame.Text(txt)
          }
        val receive = topic
          .publish
          .compose[Stream[IO, WebSocketFrame]](_.evalMap { case WebSocketFrame.Text(msg, _) =>

        for {
          newMessageId <- writeMsg.write(MessageWithoutId(msg.stripLineEnd, conversationId, fromUserId, toUserId, Instant.now()))
        } yield Msg.ChatMessage(newMessageId, fromUserId, toUserId, msg.stripLineEnd, Instant.now())
      })

      wsb.build(
        // Outgoing stream of WebSocket messages to send to the client
        send = send,
        // Incoming stream of Websocket messages from the client
        receive = receive
      )
    }
  }
}
