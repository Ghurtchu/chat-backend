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
        SELECT c.id AS conversation_id, m.text
        FROM (
          SELECT DISTINCT ON (c.id) c.id AS conversation_id, m.id AS message_id
          FROM "message" m
          JOIN "user" u ON m."fromUserId" = u.id OR m."toUserId" = u.id
          JOIN "conversation" c ON m."conversationId" = c.id
          WHERE u.id = $userId
          ORDER BY c.id, m."writtenAt" DESC
        ) AS last_messages
        JOIN "conversation" c ON last_messages.conversation_id = c.id
        JOIN "message" m ON last_messages.message_id = m.id
        ORDER BY m."writtenAt" DESC
        LIMIT $lastN;""".query[PartialConversation]

      query.stream.compile.toList
        .transact(transactor)
        .flatTap(IO.println)
    }
  }
}
