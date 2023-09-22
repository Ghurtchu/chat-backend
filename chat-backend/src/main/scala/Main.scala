import cats.effect.{ExitCode, IO, IOApp, Ref}
import cats.implicits.{catsSyntaxTuple2Semigroupal, toSemigroupKOps}
import com.comcast.ip4s.{Host, IpLiteralSyntax, Port}
import db.{NewMessageRepo, PartialConversationsRepo}
import domain.{Config, NewMessage}
import fs2.concurrent.Topic
import fs2.Stream
import org.http4s.{HttpApp, HttpRoutes}
import org.http4s.dsl.Http4sDsl
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.websocket.WebSocketBuilder2
import org.http4s.websocket.WebSocketFrame
import doobie.implicits._
import doobie.util.transactor.Transactor
import errors.config.InvalidBackendConfiguration
import pureconfig._
import pureconfig.generic.auto._
import ws.Msg

import java.time.Instant
import scala.collection.concurrent.TrieMap

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    for {
      cfg <- IO(ConfigSource.default.loadOrThrow[Config])
      transactor = Transactor.fromDriverManager[IO](
          driver = cfg.dbDriver,
          url = cfg.dbUrl,
          user = cfg.dbUser,
          password = cfg.dbPassword,
          logHandler = None,
        )
      _ <- pingDatabase(transactor)
      loadPartialConvos = PartialConversationsRepo.impl(transactor)
      writeMsg = NewMessageRepo.impl(transactor)

      // global topic where users will publish and subscribe Msg subtypes
      topic <- Topic[IO, Msg]

      // stores how many conversations are loaded for each user
      loadedConvosPerUser <- IO.ref(TrieMap.empty[String, Int]) // map(user id -> number of loaded conversations)

      hp <- IO.fromEither {
        (
          Host.fromString(cfg.backendHost),
          Port.fromInt(cfg.backendPort)
        ).tupled
          .toRight(new RuntimeException())
      }

      (host, port) = (hp._1, hp._2)

      _ <- EmberServerBuilder
        .default[IO]
        .withHost(host)
        .withPort(port)
        .withHttpWebSocketApp(httpApp(topic, _, loadedConvosPerUser, loadPartialConvos, writeMsg))
        .build
        .useForever
    } yield ExitCode.Success

  private def pingDatabase(transactor: Transactor[IO]): IO[Unit] =
    sql"SELECT 1"
      .query[Int]
      .unique
      .transact(transactor)
      .flatMap(_ => IO.println("Successfully connected to database"))

  private def httpApp(
    topic: Topic[IO, Msg],
    wsb: WebSocketBuilder2[IO],
    loadedConvosPerUser: Ref[IO, TrieMap[String, Int]],
    loadPartialConvos: PartialConversationsRepo,
    writeMsg: NewMessageRepo
  ): HttpApp[IO] =
    (conversations(topic, wsb, loadedConvosPerUser, loadPartialConvos) <+> conversation(topic, wsb, writeMsg)).orNotFound

  private def conversations(
    topic: Topic[IO, Msg],
    wsb: WebSocketBuilder2[IO],
    loadedConvosPerUser: Ref[IO, TrieMap[String, Int]],
    loadPartialConvos: PartialConversationsRepo,
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
              _             <- loadedConvosPerUser.update(_ + (userId -> n))
              convos <- loadPartialConvos
                .load(userId.trim.toInt, n)
                .map(convos => WebSocketFrame.Text(convos.mkString("[", ",", "]"))) // TODO: serialize to Json later
            } yield convos

          // new message has been sent from somebody, so we need to update loaded conversations
          case _: Msg.ChatMessage =>
            for {
              count  <- loadedConvosPerUser.get.map(_.getOrElse(userId, 10))
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

      wsb
        .build(
        // Outgoing stream of WebSocket messages to send to the client
        send = send,
        // Incoming stream of Websocket messages from the client
        receive = receive,
      )
    }
  }

  private def conversation(
    topic: Topic[IO, Msg],
    wsb: WebSocketBuilder2[IO],
    writeMsg: NewMessageRepo,
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
          newMessageId <- writeMsg.add(NewMessage(msg.stripLineEnd, conversationId.toLong, fromUserId.toLong, toUserId.toLong, Instant.now()))
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
