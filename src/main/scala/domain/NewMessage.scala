package domain

import java.time.Instant

/**
 * message without id
 * will be used to store new records in db
 */
final case class NewMessage(
  text: String,
  conversationId: Long,
  fromUserId: Long,
  toUserId: Long,
  writtenAt: Instant,
)
