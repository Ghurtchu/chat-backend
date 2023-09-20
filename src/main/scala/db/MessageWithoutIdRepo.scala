package db

import cats.effect.IO
import domain.MessageWithoutId
import doobie.Transactor
import doobie.implicits._

import java.time.Instant

trait MessageWithoutIdRepo {
  def write(msg: MessageWithoutId): IO[Int]
}

object MessageWithoutIdRepo {

  def of(xa: Transactor[IO]): MessageWithoutIdRepo = new MessageWithoutIdRepo {
    override def write(msg: MessageWithoutId): IO[Int] = {
      val dbAction = sql"""
             WITH inserted_message AS (
          INSERT INTO message (message_content, conversation_id, fromuserid, touserid, written_at)
          VALUES (${msg.text}, ${msg.conversationId.toInt}, ${msg.fromUserId.toInt}, ${msg.toUserId.toInt}, ${msg.writtenAt.toString}::timestamp)
          RETURNING message_id
        )
        SELECT message_id FROM inserted_message;""".query[Int].unique

      dbAction.transact(xa)
    }
  }
}
