package topic

import java.time.Instant

sealed trait TopicMessage

object TopicMessage {

  case class ChatMessageFromClient(messageId: Int, fromUserId: String, toUserId: String, text: String, timestamp: Instant)
      extends TopicMessage

  case class LoadConversations(amount: Int) extends TopicMessage

}
