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

import java.time.Instant
import scala.concurrent.duration.DurationInt

/**
 *
 * WITH LastMessageConversations AS (
    SELECT
        uc.user_id,
        c.conversation_id,
        c.conversation_name,
        MAX(m.written_at) AS last_message_written_at
    FROM
        user_conversation uc
    JOIN
        conversation c ON uc.conversation_id = c.conversation_id
    JOIN
        message m ON c.conversation_id = m.conversation_id
    WHERE
        uc.user_id = 1 -- Replace :user_id with the actual user's ID
    GROUP BY
        uc.user_id, c.conversation_id, c.conversation_name
)
SELECT
    LMC.user_id,
    LMC.conversation_id,
    LMC.conversation_name,
    M.message_content AS last_message_content,
    LMC.last_message_written_at AS last_message_written_at
FROM
    LastMessageConversations LMC
JOIN
    message M ON LMC.conversation_id = M.conversation_id
    AND LMC.last_message_written_at = M.written_at
ORDER BY
    LMC.last_message_written_at DESC
LIMIT 1;

 *
 */

object Main extends IOApp {
  case class User(id: String, name: String, conversations: Vector[Conversation])
  case class Conversation(id: String, messages: Vector[Message])
  case class Message(id: String, text: String, fromUserId: String, writtenAt: Instant)

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

  private def httpApp(
    topic: Topic[IO, Msg],
    wsb: WebSocketBuilder2[IO],
  ): HttpApp[IO] =
    (echo(wsb) <+> chats(topic, wsb) <+> chat(topic, wsb)).orNotFound

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

  trait LoadConvos {
    def load(userId: String, from: Int, to: Int): Stream[IO, Conversation]
  }

  val loadConvos = new LoadConvos {

    val data = List(
      Conversation("1", Vector.empty),
      Conversation("2", Vector.empty),
      Conversation("3", Vector.empty),
      Conversation("4", Vector.empty),
      Conversation("5", Vector.empty),
      Conversation("6", Vector.empty),
      Conversation("7", Vector.empty),
      Conversation("8", Vector.empty),
      Conversation("9", Vector.empty),
      Conversation("10", Vector.empty),
      Conversation("11", Vector.empty),
    )

    override def load(userId: String, from: Int, to: Int): Stream[IO, Conversation] =
      Stream.emits(data.slice(from, to))
  }

  sealed trait Msg

  object Msg {
    case class ChatMessage(fromUserId: String, toUserId: String, text: String) extends Msg
    case class LoadConversations(from: Int, to: Int)                           extends Msg
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
      val sendStream = topic
        .subscribe(maxQueued = 10)
        .flatMap {

          case Msg.ChatMessage(from, to, txt) =>
            loadConvos
              .load(userId, 0, 5)
              .map { convo =>
                WebSocketFrame.Text {
                  s"""{
                     |  "id": ${convo.id},
                     |  "convos": ${convo.messages.mkString("[", ",", "]")}
                     |}""".stripMargin
                }
              }

          case Msg.LoadConversations(from, to) =>
            loadConvos
              .load(userId, from, to)
              .map { convo =>
                WebSocketFrame.Text {
                  s"""{
                     |  "id": ${convo.id},
                     |  "convos": ${convo.messages.mkString("[", ",", "]")}
                     |}""".stripMargin
                }
              }

        }

      wsb.build(
        // Outgoing stream of WebSocket messages to send to the client
        send = sendStream,

        // Sink, where the incoming WebSocket messages from the client are pushed to
        receive = topic.publish.compose[Stream[IO, WebSocketFrame]](_.collect {
          case WebSocketFrame.Text(msg, _) =>
            val fromAndTo  = msg.trim.split(",").map(_.toInt)
            val (from, to) = (fromAndTo.head, fromAndTo.last)

            Msg.LoadConversations(from, to)
        }),
      )
    }
  }

  private def chat(topic: Topic[IO, Msg], wsb: WebSocketBuilder2[IO]): HttpRoutes[IO] = {
    val dsl = new Http4sDsl[IO] {}
    import dsl._

    HttpRoutes.of[IO] { case GET -> Root / "chats" / fromUserId / "chat" / toUserId =>
      val sendStream = topic
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
