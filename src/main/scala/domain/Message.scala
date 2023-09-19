package domain

import java.time.Instant

final case class Message(id: String, text: String, fromUserId: String, writtenAt: Instant)

