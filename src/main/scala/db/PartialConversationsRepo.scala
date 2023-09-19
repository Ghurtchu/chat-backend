package db

import cats.effect.IO
import domain.{Conversation, PartialConversation}
import doobie._
import doobie.implicits._
trait PartialConversationsRepo {
  def load(userId: Int, lastN: Int): IO[List[PartialConversation]]
}

object PartialConversationsRepo {
  def impl(xa: Transactor[IO]): PartialConversationsRepo = new PartialConversationsRepo {
    override def load(userId: Int, lastN: Int): IO[List[PartialConversation]] = {
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
           """.query[PartialConversation]

      query.stream.compile.toList
        .transact(xa)
        .flatTap(IO.println)
    }
  }
}
