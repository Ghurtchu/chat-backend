package domain

import doobie.Read

/**
 * this data class will be used to render information on UI before user navigates to specific conversation
 */
final case class PartialConversation(id: String, lastMessage: String)

object PartialConversation {
  implicit val conversationRead: Read[PartialConversation] = Read[(String, String)].map {
    case (conversationId, lastMessage) =>
      PartialConversation(conversationId, lastMessage)
  }
}
