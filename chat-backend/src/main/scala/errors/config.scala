package errors

object config {

  object InvalidBackendConfiguration extends Throwable {
    override def getMessage: String = "Invalid host or port, please fix config"
  }

}
