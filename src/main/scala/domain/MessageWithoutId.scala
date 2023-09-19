package domain

import java.time.Instant

case class MessageWithoutId(
  text: String,
  conversationId: String,
  fromUserId: String,
  toUserId: String,
  writtenAt: Instant,
)
