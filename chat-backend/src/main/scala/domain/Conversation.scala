package domain

import doobie.Read

final case class Conversation(id: String, name: String, messages: Vector[Message] = Vector.empty)
