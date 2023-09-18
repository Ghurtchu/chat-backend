import cats.effect.std.Queue
import cats.effect.{Clock, ExitCode, IO, IOApp}
import cats.implicits.toSemigroupKOps
import com.comcast.ip4s.IpLiteralSyntax
import fs2.concurrent.Topic
import fs2.{Pipe, Pull, Stream}
import org.http4s.{HttpApp, HttpRoutes}
import org.http4s.dsl.Http4sDsl
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.websocket.WebSocketBuilder2
import org.http4s.websocket.WebSocketFrame
import doobie._
import doobie.implicits._

import java.time.Instant

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
  case class User(id: String, name: String, conversations: Vector[Conversation])
  case class Conversation(id: String, name: String, messages: Vector[Message] = Vector.empty)

  implicit val conversationRead: Read[Conversation] = Read[(String, String)].map {
    case (conversationId, name) =>
      Conversation(conversationId, name)
  }
  case class Message(id: String, text: String, fromUserId: String, writtenAt: Instant)

  val xa: Transactor[IO] = Transactor.fromDriverManager[IO](
    driver = "org.postgresql.Driver",
    url = "jdbc:postgresql://localhost:5432/chat",
    user = "aghurtchumelia",
    password = "password",
    logHandler = None,
  )

  override def run(args: List[String]): IO[ExitCode] =
    for {
      topic <- Topic[IO, Msg]
      _     <- EmberServerBuilder
        .default[IO]
        .withHost(host"localhost")
        .withPort(port"8080")
        .withHttpWebSocketApp(httpApp(topic, _))
        .build
        .useForever
    } yield ExitCode.Success

  private def httpApp(
    topic: Topic[IO, Msg],
    wsb: WebSocketBuilder2[IO],
  ): HttpApp[IO] =
    (chats(topic, wsb) <+> chat(topic, wsb)).orNotFound

  trait LoadConvos {
    def load(userId: Int, lastN: Int): IO[List[Conversation]]
  }

  val loadConvos = new LoadConvos {

    override def load(userId: Int, lastN: Int): IO[List[Conversation]] = {

      val query =
        sql"""
          SELECT DISTINCT ON (c.conversation_id)
            c.conversation_id,
            c.conversation_name
        FROM
            "user" u
        JOIN
            user_conversation uc ON u.user_id = uc.user_id
        JOIN
            conversation c ON uc.conversation_id = c.conversation_id
        LEFT JOIN
            (
                SELECT
                    conversation_id,
                    MAX(written_at) AS max_written_at
                FROM
                    message
                GROUP BY
                    conversation_id
            ) max_message
            ON c.conversation_id = max_message.conversation_id
        LEFT JOIN
            message m
            ON max_message.conversation_id = m.conversation_id
        WHERE
            u.user_id = $userId
        ORDER BY
            c.conversation_id,
            m.written_at DESC
        LIMIT
            $lastN;
           """.query[Conversation]

      val action = query.stream.compile.toList.transact(xa).flatTap(IO.println)

      action
    }
  }

  sealed trait Msg

  object Msg {
    case class ChatMessage(fromUserId: String, toUserId: String, text: String) extends Msg
    case class LoadConversations(n: Int)                                       extends Msg
  }

  private def chats(topic: Topic[IO, Msg], wsb: WebSocketBuilder2[IO]): HttpRoutes[IO] = {
    val dsl = new Http4sDsl[IO] {}
    import dsl._

    // set up ws connection = ws://host:port/chat/{userId}
    // and send messages which will indicate the range
    // 1,2 (first and second conversation for userId)
    // 2,10 (second to tenth conversation for userId)
    // and so on
    HttpRoutes.of[IO] { case GET -> Root / "chats" / userId =>
      val sendStream: Stream[IO, WebSocketFrame.Text] = topic
        .subscribe(maxQueued = 10)
        .evalMap { case Msg.LoadConversations(n) =>
          loadConvos
            .load(userId.trim.toInt, n)
            .map(convos => WebSocketFrame.Text(convos.mkString("[", ",", "]")))

//          case Msg.LoadConversations(from, to) =>
//            loadConvos
//              .load(userId, from, to)
//              .map { convo =>
//                WebSocketFrame.Text {
//                  s"""{
//                     |  "id": ${convo.id},
//                     |  "convos": ${convo.messages.mkString("[", ",", "]")}
//                     |}""".stripMargin
//                }
//              }

        }

      wsb.build(
        // Outgoing stream of WebSocket messages to send to the client
        send = sendStream,

        // Sink, where the incoming WebSocket messages from the client are pushed to
        receive = topic.publish.compose[Stream[IO, WebSocketFrame]](_.collect {
          case WebSocketFrame.Text(msg, _) =>
            val amount = msg.trim.toInt

            Msg.LoadConversations(amount)
        }),
      )
    }
  }

  private def chat(topic: Topic[IO, Msg], wsb: WebSocketBuilder2[IO]): HttpRoutes[IO] = {
    val dsl = new Http4sDsl[IO] {}
    import dsl._

    HttpRoutes.of[IO] { case GET -> Root / "chats" / fromUserId / "chat" / toUserId =>
      val sendStream: Stream[IO, WebSocketFrame.Text] = topic
        .subscribe(maxQueued = 10)
        .collect { case Msg.ChatMessage(from, to, txt) =>
          // write to redis
          // send to websocket
          txt
        }
        .map(WebSocketFrame.Text(_))

      wsb.build(
        // Outgoing stream of WebSocket messages to send to the client
        send = sendStream,

        // Sink, where the incoming WebSocket messages from the client are pushed to
        receive = topic.publish.compose[Stream[IO, WebSocketFrame]](_.collect {
          case WebSocketFrame.Text(msg, _) => Msg.ChatMessage(fromUserId, toUserId, msg)
        }),
      )
    }
  }

}
