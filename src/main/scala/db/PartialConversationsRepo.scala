package db

import cats.effect.IO
import domain.{Conversation, PartialConversation}
import doobie._
import doobie.implicits._
trait PartialConversationsRepo {

  /** implements lazy loading of conversations for high performance
    *
    * load lastN amount of conversation and for each conversation read only last message
    */
  def load(userId: Int, lastN: Int): IO[List[PartialConversation]]
}

object PartialConversationsRepo {
  def impl(implicit transactor: Transactor[IO]): PartialConversationsRepo = new PartialConversationsRepo {
    override def load(userId: Int, lastN: Int): IO[List[PartialConversation]] = {
      val query =
        sql"""
          SELECT c.conversation_id, m.message_content
        FROM (
          SELECT DISTINCT ON (conversation_id) conversation_id, message_id
          FROM message
          WHERE fromuserid = $userId OR touserid = $userId
          ORDER BY conversation_id, written_at DESC
        ) AS last_messages
        JOIN conversation c ON last_messages.conversation_id = c.conversation_id
        JOIN message m ON last_messages.message_id = m.message_id
        ORDER BY m.written_at DESC
        LIMIT $lastN;""".query[PartialConversation]

      query.stream.compile.toList
        .transact(transactor)
        .flatTap(IO.println)
    }
  }
}
