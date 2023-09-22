package domain

final case class User(id: String, name: String, email: String, conversations: Vector[Conversation])

