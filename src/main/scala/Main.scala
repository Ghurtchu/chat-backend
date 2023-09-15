import cats.effect.std.Queue
import cats.effect.{Clock, ExitCode, IO, IOApp, Ref}
import cats.implicits.toSemigroupKOps
import com.comcast.ip4s.IpLiteralSyntax
import fs2.concurrent.Topic
import fs2.{Pipe, Stream}
import org.http4s.{HttpApp, HttpRoutes}
import org.http4s.dsl.Http4sDsl
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.websocket.WebSocketBuilder2
import org.http4s.websocket.WebSocketFrame

import scala.concurrent.duration.DurationInt

object Main extends IOApp {

  final case class User(id: String, name: String)
  object User {
    def fromUserName: String => User = new User(java.util.UUID.randomUUID().toString, _)
  }

  final case class Users(users: List[User]) extends AnyVal {
    def addUser(user: User): Users =
      if (users contains user) this else copy(user :: users)
  }

  object Users {
    def initial: Users  = Users(Nil)
    def initial2: Users = Users(List(User("1", "nika"), User("2", "ada")))
  }

  final case class UsersRef(ref: Ref[IO, Users]) extends AnyVal

  override def run(args: List[String]): IO[ExitCode] =
    for {
      usersRef <- IO.ref(Users.initial2)
      topic    <- Topic[IO, String]
      _        <- EmberServerBuilder
        .default[IO]
        .withHost(host"localhost")
        .withPort(port"9000")
        .withHttpWebSocketApp(httpApp(topic, _))
        .build
        .useForever
    } yield ExitCode.Success

  val pipe: Pipe[IO, WebSocketFrame, WebSocketFrame] = _.collect { case WebSocketFrame.Text(msg, _) =>
    WebSocketFrame.Text(msg)
  }.evalMap {
    case WebSocketFrame.Text(msg, _) if msg.trim == "time" =>
      Clock[IO].realTimeInstant.map(t => WebSocketFrame.Text(t.toString))
    case other                                             => IO.pure(other)
  }.merge {
    Stream
      .awakeEvery[IO](5.seconds)
      .map(duration => WebSocketFrame.Text(s"Connected for $duration"))
  }

  private def httpApp(topic: Topic[IO, String], wsb: WebSocketBuilder2[IO]): HttpApp[IO] =
    (echo(wsb) <+> chat(topic, wsb) <+> chatWithUsername(topic, wsb)).orNotFound

  private def echo(wsb: WebSocketBuilder2[IO]): HttpRoutes[IO] = {
    val dsl = new Http4sDsl[IO] {}
    import dsl._

    HttpRoutes.of[IO] {
      // for testing
      // websocat "ws://localhost:9000/echo"
      case GET -> Root / "echo" =>
        for {
          // Unbounded queue to store websocket messages from the client, which are pending to be processed.
          // For production use bounded queue seems a better choice. Unbounded queue may result in OOM (out of memory error)
          // if the client is sending messages quicker than the server can process them.
          queue    <- Queue.unbounded[IO, WebSocketFrame]
          response <- wsb.build(
            send = Stream.repeatEval(queue.take).through(pipe),
            receive = _.evalMap(queue.offer),
          )
        } yield response
    }
  }

  // Topic[F[_], A] provides an implementation of pub-sub pattern with an X number of publishers and Y number of subscribers.
  private def chat(topic: Topic[IO, String], wsb: WebSocketBuilder2[IO]): HttpRoutes[IO] = {
    val dsl = new Http4sDsl[IO] {}
    import dsl._

    // test: websocat ws://localhost:9000/chat
    HttpRoutes.of[IO] { case GET -> Root / "chat" =>
      wsb.build(
        // Outgoing stream of WebSocket messages to send to the client
        send = topic.subscribe(maxQueued = 10).map(WebSocketFrame.Text(_)),

        // Sink, where the incoming WebSocket messages from the client are pushed to
        receive = topic.publish.compose[Stream[IO, WebSocketFrame]](_.collect {
          case WebSocketFrame.Text(msg, _) => msg
        }),
      )
    }
  }

  private def chatWithUsername(topic: Topic[IO, String], wsb: WebSocketBuilder2[IO]): HttpRoutes[IO] = {
    val dsl = new Http4sDsl[IO] {}
    import dsl._

    // test: websocat ws://localhost:9000/chat
    HttpRoutes.of[IO] { case GET -> Root / "chat" / username =>
      wsb.build(
        // Outgoing stream of WebSocket messages to send to the client
        send = topic.subscribe(maxQueued = 10).collect {
          // case msg if !msg.split(":").headOption.contains(username) => WebSocketFrame.Text(msg)
          WebSocketFrame.Text(_)
        },

        // Sink, where the incoming WebSocket messages from the client are pushed to
        receive = topic.publish.compose[Stream[IO, WebSocketFrame]](_.collect {
          case WebSocketFrame.Text(msg, _) => s"$username: $msg"
        }),
      )
    }
  }

}
