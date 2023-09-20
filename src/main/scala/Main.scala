import cats.effect.{ExitCode, IO, IOApp, Ref}
import cats.implicits.toSemigroupKOps
import com.comcast.ip4s.IpLiteralSyntax
import db.{NewMessageRepo, PartialConversationsRepo}
import domain.NewMessage
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

object Main extends IOApp {

  // TODO: parse params from config
  implicit val transactor: Transactor[IO] = Transactor.fromDriverManager[IO](
    driver = "org.postgresql.Driver",
    url = "jdbc:postgresql://localhost:5432/chat",
    user = "aghurtchumelia",
    password = "password",
    logHandler = None,
  )

  // TODO: inject these into future services later
  val loadPartialConvos = PartialConversationsRepo.impl
  val writeMsg = NewMessageRepo.impl

  override def run(args: List[String]): IO[ExitCode] =
    for {
      // global topic where users will publish and subscribe Msg subtypes
      topic                      <- Topic[IO, Msg]

      // stores how many conversations are loaded for each user
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
    // and load n amount of partial conversations for UI
    // 2 = last 2 partial convos
    // 10 = last 10 partial convos
    // and so on
    HttpRoutes.of[IO] { case GET -> Root / "conversations" / userId =>
      val send = topic
        .subscribe(maxQueued = 10)
        .evalMap {
          // initial command for loading n amount of conversations
          case Msg.LoadConversations(n) =>
            for {
              _             <- loadedConversationsPerUser.update(_ + (userId -> n))
              convos <- loadPartialConvos
                .load(userId.trim.toInt, n)
                .map(convos => WebSocketFrame.Text(convos.mkString("[", ",", "]"))) // TODO: serialize to Json later
            } yield convos

          // new message has been sent from somebody, so we need to update loaded conversations
          case _: Msg.ChatMessage =>
            for {
              count  <- loadedConversationsPerUser.get.map(_.getOrElse(userId, 10))
              convos <- loadPartialConvos
                .load(userId.trim.toInt, count)
                .map(convos => WebSocketFrame.Text(convos.mkString("[", ",", "]"))) // TODO: serialize to Json later
            } yield convos
        }
      val receive = topic
        .publish
        .compose[Stream[IO, WebSocketFrame]](_.collect {

          // from FE we will receive messages about how many convos should we load, based on user scrolling behaviour
          case WebSocketFrame.Text(msg, _) =>
          // TODO: do safe parsing later
            val amount = msg.trim.toInt

            // publish to the Topic
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
            // collect only messages for which `fromUserId` and `toUserId` can be exchanged, meaning that
            // if 1 texts msg to 3 it must only be forwarded to 3 and not 2 for example.
            // P.S haven't thought about multi user chat functionality yet.
            case Msg.ChatMessage(_, `fromUserId` | `toUserId`, `toUserId` | `fromUserId`, txt, _) =>
              WebSocketFrame.Text(txt)
          }
        val receive = topic
          .publish
          .compose[Stream[IO, WebSocketFrame]](_.evalMap { case WebSocketFrame.Text(msg, _) =>

        // upon receiving a new message from user, we create a record in database and publish ChatMessage in Topic
        for {
          newMessageId <- writeMsg.add(NewMessage(msg.stripLineEnd, conversationId, fromUserId, toUserId, Instant.now()))
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
