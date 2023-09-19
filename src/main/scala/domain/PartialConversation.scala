package domain

import doobie.Read

final case class PartialConversation(id: String, name: String)

object PartialConversation {
  implicit val conversationRead: Read[PartialConversation] = Read[(String, String)].map {
    case (conversationId, name) =>
      PartialConversation(conversationId, name)
  }
}
