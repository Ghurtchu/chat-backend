package domain

final case class Config(
  containerName: String,
  dbUser: String,
  dbPassword: String,
  dbName: String,
  dbPort: Int,
  dbHost: String,
  dbDriver: String,
  dbUrlPrefix: String
)
