package domain

final case class User(id: String, name: String, conversations: Vector[Conversation])

