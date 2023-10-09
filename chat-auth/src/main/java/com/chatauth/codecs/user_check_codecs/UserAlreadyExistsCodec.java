package com.chatauth.codecs.user_check_codecs;

import com.chatauth.messages.UserAlreadyExists;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;

public class UserAlreadyExistsCodec implements MessageCodec<UserAlreadyExists, UserAlreadyExists> {

  @Override
  public void encodeToWire(Buffer buffer, UserAlreadyExists userAlreadyExists) {
    // No need to serialize anything since it's a singleton
  }
  @Override
  public UserAlreadyExists decodeFromWire(int pos, Buffer buffer) {
    // No need to deserialize anything since it's a singleton
    return UserAlreadyExists.getInstance();
  }

  @Override
  public UserAlreadyExists transform(UserAlreadyExists userAlreadyExists) {
    // Since it's a singleton, just return the instance
    return UserAlreadyExists.getInstance();
  }

  @Override
  public String name() {
    return UserAlreadyExistsCodec.class.getName();
  }

  @Override
  public byte systemCodecID() {
    return -1; // Custom codecs have negative IDs
  }
}
