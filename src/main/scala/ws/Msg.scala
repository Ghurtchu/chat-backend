package ws

import java.time.Instant

sealed trait Msg

object Msg {

    final case class ChatMessage(
      id: Int,
      fromUserId: String,
      toUserId: String,
      text: String,
      timestamp: Instant,
    ) extends Msg

    final case class LoadConversations(amount: Int) extends Msg

}
