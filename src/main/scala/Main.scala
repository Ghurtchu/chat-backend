import cats.effect.std.Queue
import cats.effect.{Clock, ExitCode, IO, IOApp, Ref}
import com.comcast.ip4s.IpLiteralSyntax
import fs2.{Pipe, Stream}
import org.http4s.HttpRoutes
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
      _        <- EmberServerBuilder
        .default[IO]
        .withHost(host"localhost")
        .withPort(port"9000")
        .withHttpWebSocketApp(chat(_).orNotFound)
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
      .map(d => WebSocketFrame.Text(s"Connected for $d"))
  }

  private def chat(wsb: WebSocketBuilder2[IO]): HttpRoutes[IO] = {
    val dsl = new Http4sDsl[IO] {}
    import dsl._

    HttpRoutes.of[IO] {
      // for testing
      // websocat "ws://localhost:9002/echo"
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

}
