import cats.effect.std.Queue
import cats.effect.{Clock, ExitCode, IO, IOApp, Ref}
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
import scala.concurrent.duration.{DurationInt, FiniteDuration}

object Main extends IOApp {

  // we have two users, user A and user B and each user has list of chats

  // case class User(id: String, name: String, conversations: Vector[Conversation])

  // case class Conversation(id: String, messages: Vector[Message])

  // case class Message(id: String, text: String, fromUserId: String, writtenAt: Instant)

  // req ~/chat/1 =>
  // - read user from redis User(1, "anzori", conversations) for each conversation load only last 10 messages
  // -

  case class User(id: String, name: String, conversations: Vector[Conversation])
  case class Conversation(id: String, messages: Vector[Message])
  case class Message(id: String, text: String, fromUserId: String, writtenAt: Instant)

  trait UserRedis {

    /** load user, load last 10 conversations, for each conversation load only last message
      */
    def loadUser(id: String): IO[User]
  }

  val redis: UserRedis = new UserRedis {

    val messages = Vector(
      Message("msg2", "bye nika", "gio-id", Instant.now()),
    )

    val repo = List(
      User(
        "nika-id",
        "nika",
        Vector(Conversation("nika-gio", messages)),
      ),
      User(
        "gio-id",
        "gio",
        Vector(Conversation("nika-gio", messages)),
      ),
    )

    override def loadUser(id: String): IO[User] = IO.pure {
      repo.find(_.id == id).get
    }
  }

  // login
  // req @ /chat/username
  val readUser: Pipe[IO, String, User] = _.evalMap(redis.loadUser)

  // request specific chat
  // req @ /chat/username/chatId

  trait ConversationRedis {

    /** load conversation load only last 50 messages in convo
      */
    def loadConvo(id: String): IO[Conversation]
  }

  val convoRedis = new ConversationRedis {

    /** load conversation load only last 50 messages in convo
      */
    override def loadConvo(id: String): IO[Conversation] = IO(Conversation("1", Vector.empty))
  }

  val readChat: Stream[IO, Conversation] = Stream.eval {
    convoRedis.loadConvo("nika-gio")
  }

  trait WriteMessageRedis {

    /** writes message to redis
      */
    def write(text: String, fromUserId: String): IO[Unit]
  }

  val write = new WriteMessageRedis {
    override def write(text: String, fromUserId: String): IO[Unit] = ???
  }

  // req POST @ /chat/username/chatId
  val writeStream: Pipe[IO, String, Unit] = _.evalMap { msg =>
    write.write(msg, "id")
  }
  final case class TopicsRef(ref: Ref[IO, Map[String, Topic[IO, String]]])
  // userId -> topicId
  final case class UserChats(ref: Ref[IO, Map[String, String]])

  override def run(args: List[String]): IO[ExitCode] =
    for {
      topic     <- Topic[IO, String]
      userChats <- IO
        .ref {
          Map(
            "nika" -> "topic1",
            "gio"  -> "topic1",
          )
        }
        .map(UserChats)
      topicsRef <- IO
        .ref {
          Map(
            "topic1" -> topic,
          )
        }
        .map(TopicsRef)
      _         <- EmberServerBuilder
        .default[IO]
        .withHost(host"localhost")
        .withPort(port"9001")
        .withHttpWebSocketApp(httpApp(topic, _, topicsRef, userChats))
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
    topic: Topic[IO, String],
    wsb: WebSocketBuilder2[IO],
    topicsRef: TopicsRef,
    userChats: UserChats,
  ): HttpApp[IO] =
    (echo(wsb) <+>
      chat(topic, wsb) <+>
      chatWithUsername(topic, wsb) <+>
      chatWithDynamicUsername(topic, wsb) <+>
      chats(topic, wsb) <+>
      chat2(topicsRef, userChats, wsb)).orNotFound

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

  // Topic[F[_], A] provides an implementation of pub-sub pattern with an X number of publishers and Y number of subscribers
  // so basically a good fit for one-to-one or multi-user chat server implementations.
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

  var a = false

  trait LoadConvos {
    def load(userId: String, from: Int, to: Int): Stream[IO, String]
  }

  val loadConvos = new LoadConvos {

    val data = List(
      "Conversation 1",
      "Conversation 2",
      "Conversation 3",
      "Conversation 4",
      "Conversation 5",
      "Conversation 6",
      "Conversation 7",
      "Conversation 8",
      "Conversation 9",
    )

    override def load(userId: String, from: Int, to: Int): Stream[IO, String] =
      if (!a) Stream.emits(data)
      else
        Stream.emits {
          collection.mutable.ListBuffer(
            "Conversation 1",
            "Conversation 4",
            "Conversation 2",
            "Conversation 3",
            "Conversation 7",
            "Conversation 5",
            "Conversation 100",
            "Conversation 8",
            "Conversation 9",
          )
        }
  }

  val scheduleStream: Stream[IO, FiniteDuration] = Stream.awakeEvery[IO](2.seconds)

  private def chats(topic: Topic[IO, String], wsb: WebSocketBuilder2[IO]): HttpRoutes[IO] = {
    val dsl = new Http4sDsl[IO] {}
    import dsl._

    // set up ws connection = ws://host:port/chat/{userId}
    // and send messages which will indicate the range
    // 1,2 (first and second conversation for userId)
    // 2,10 (second to tenth conversation for userId)
    // and so on
    HttpRoutes.of[IO] { case GET -> Root / "chats" / userId =>
      val stream = topic
        .subscribe(maxQueued = 10)
        .flatMap { fromAndToString =>

          val fromAndTo  = fromAndToString.trim.split(",").map(_.toInt)
          val (from, to) = (fromAndTo.head, fromAndTo.last)

          if (from == 1) a = true else ()

          println("here")
          loadConvos
            .load(userId, from, to)
            .map(_.mkString(","))
            .map(WebSocketFrame.Text(_))
        }
        .merge(
          scheduleStream
            .flatMap(_ => loadConvos.load(userId, 0, 10))
            .map(_.mkString(","))
            .map(WebSocketFrame.Text(_)),
        )

      wsb.build(
        // Outgoing stream of WebSocket messages to send to the client
        send = stream,

        // Sink, where the incoming WebSocket messages from the client are pushed to
        receive = topic.publish.compose[Stream[IO, WebSocketFrame]](_.collect {
          case WebSocketFrame.Text(msg, _) => msg
        }),
      )
    }
  }

  // topicId -> topic = topicsRef
  // userId  -> topicId = chatsRef
  private def chat2(
    topicsRef: TopicsRef,
    userChats: UserChats,
    wsb: WebSocketBuilder2[IO],
  ): HttpRoutes[IO] = {
    val dsl = new Http4sDsl[IO] {}
    import dsl._

    // test: websocat ws://localhost:9000/chat
    HttpRoutes.of[IO] { case GET -> Root / "chat2" / username =>
      for {
        topics <- topicsRef.ref.get
        chats  <- userChats.ref.get
        res    <- {
          val top = for {
            topicId <- chats.get(username)
            topic   <- topics.get(topicId)
          } yield topic

          top match {
            case Some(topic) =>
              wsb.build(
                // Outgoing stream of WebSocket messages to send to the client
                send = topic.subscribe(maxQueued = 10).map(WebSocketFrame.Text(_)),

                // Sink, where the incoming WebSocket messages from the client are pushed to
                receive = topic.publish.compose[Stream[IO, WebSocketFrame]](_.collect {
                  case WebSocketFrame.Text(msg, _) => msg
                }),
              )
            case None        => Ok("XD")
          }
        }
      } yield res
    }
  }

  private def chatWithUsername(
    topic: Topic[IO, String],
    wsb: WebSocketBuilder2[IO],
  ): HttpRoutes[IO] = {
    val dsl = new Http4sDsl[IO] {}
    import dsl._

    // req =>
    // req => topic => consumer from topic

    // test: websocat ws://localhost:9000/chat
    HttpRoutes.of[IO] { case GET -> Root / "chat" / username =>
      wsb.build(
        // Outgoing stream of WebSocket messages to send to the client
        send = topic.subscribe(maxQueued = 10).collect {
          // case msg if !msg.split(":").headOption.contains(username) => WebSocketFrame.Text(msg)
          WebSocketFrame.Text(_)
        },

        // Sink, where the incoming WebSocket messages from the client are pushed to
        receive = topic.publish.compose[Stream[IO, WebSocketFrame]](
          _.collect { case WebSocketFrame.Text(msg, _) =>
            s"$username: $msg"
          },
        ),
      )
    }
  }

  private def chatWithDynamicUsername(
    topic: Topic[IO, String],
    wsb: WebSocketBuilder2[IO],
  ): HttpRoutes[IO] = {
    val dsl = new Http4sDsl[IO] {}
    import dsl._

    // test: websocat ws://localhost:9000/dynamic
    HttpRoutes.of[IO] { case GET -> Root / "dynamic" =>
      wsb.build(
        // Outgoing stream of WebSocket messages to send to the client
        send = (Stream.emit("Type username:") ++ topic.subscribe(maxQueued = 10)).collect {
          // case msg if !msg.split(":").headOption.contains(username) => WebSocketFrame.Text(msg)
          WebSocketFrame.Text(_)
        },

        // Sink, where the incoming WebSocket messages from the client are pushed to
        receive = topic.publish.compose[Stream[IO, WebSocketFrame]](
          _.collect {
            // case WebSocketFrame.Text(msg, _) => s"$username: $msg"
            case WebSocketFrame.Text(msg, _) => msg
          }.pull.uncons1.flatMap {
            case Some((head, tail)) => tail.map(head.trim concat ":" concat _).pull.echo
            case None               => Pull.done
          }.stream,
        ),
      )
    }
  }

}
