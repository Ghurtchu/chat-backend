package db

import cats.effect.IO
import domain.NewMessage
import doobie.Transactor
import doobie.implicits._

import java.time.Instant

trait NewMessageRepo {

  /** writes the new record in "message" table
    */
  def add(msg: NewMessage): IO[Int]
}

object NewMessageRepo {

  def impl(implicit xa: Transactor[IO]): NewMessageRepo = new NewMessageRepo {
    override def add(msg: NewMessage): IO[Int] = {
      val dbAction = sql"""
       WITH inserted_message AS (
          INSERT INTO message (text, "conversationId", "fromUserId", "toUserId", "writtenAt")
          VALUES ('hey', 1, 1, 2, '2023-09-22 14:46:00.000 +0200')
          RETURNING id
        )
        SELECT id FROM inserted_message;""".query[Int].unique

      dbAction.transact(xa)
    }
  }
}
