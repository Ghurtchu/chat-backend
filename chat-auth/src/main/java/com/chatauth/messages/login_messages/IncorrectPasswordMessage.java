package com.chatauth.messages.login_messages;

public class IncorrectPasswordMessage {
  private static final IncorrectPasswordMessage INSTANCE = new IncorrectPasswordMessage();

  private IncorrectPasswordMessage() {}

  public static IncorrectPasswordMessage getInstance() {
    return INSTANCE;
  }
}
