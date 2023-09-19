package domain

import doobie.Read

final case class Conversation(id: String, name: String, messages: Vector[Message] = Vector.empty)

object Conversation {
  implicit val conversationRead: Read[Conversation] = Read[(String, String)].map {
    case (conversationId, name) =>
      Conversation(conversationId, name)
  }
}
