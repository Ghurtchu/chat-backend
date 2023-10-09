package com.chatauth.codecs.login_codecs;

import com.chatauth.messages.UserAlreadyExists;
import com.chatauth.messages.login_messages.IncorrectPasswordMessage;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;

public class IncorrectPasswordMessageCodec implements MessageCodec<IncorrectPasswordMessage, IncorrectPasswordMessage> {

  @Override
  public void encodeToWire(Buffer buffer, IncorrectPasswordMessage incorrectPasswordMessage) {
    // No need to serialize anything since it's a singleton
  }
  @Override
  public IncorrectPasswordMessage decodeFromWire(int pos, Buffer buffer) {
    // No need to deserialize anything since it's a singleton
    return IncorrectPasswordMessage.getInstance();
  }

  @Override
  public IncorrectPasswordMessage transform(IncorrectPasswordMessage incorrectPasswordMessage) {
    // Since it's a singleton, just return the instance
    return IncorrectPasswordMessage.getInstance();
  }

  @Override
  public String name() {
    return IncorrectPasswordMessage.class.getName();
  }

  @Override
  public byte systemCodecID() {
    return -1; // Custom codecs have negative IDs
  }
}
