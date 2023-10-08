package com.chatauth.messages;

public class UserAlreadyExists {

  // created only once and used by every other object
  // lazy instantiation
  private static final UserAlreadyExists INSTANCE = new UserAlreadyExists();

  private UserAlreadyExists() {}

  public static UserAlreadyExists getInstance() {
    return INSTANCE;
  }
}
