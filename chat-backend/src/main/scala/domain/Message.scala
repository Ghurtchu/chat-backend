package domain

import java.time.Instant

final case class Message(
  id: String,
  text: String,
  conversationId: String,
  fromUserId: String,
  toUserId: String,
  writtenAt: Instant,
)
