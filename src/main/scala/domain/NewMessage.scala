package domain

import java.time.Instant

/**
 * message without id
 * will be used to store new records in db
 */
case class NewMessage(
  text: String,
  conversationId: String,
  fromUserId: String,
  toUserId: String,
  writtenAt: Instant,
)
