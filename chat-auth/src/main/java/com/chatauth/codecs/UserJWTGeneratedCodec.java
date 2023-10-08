package com.chatauth.codecs;

import com.chatauth.messages.UserJWTGenerated;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;

public class UserJWTGeneratedCodec implements MessageCodec<UserJWTGenerated, UserJWTGenerated> {

  @Override
  public void encodeToWire(Buffer buffer, UserJWTGenerated userJWTGenerated) {
    // Encode the data to the buffer
    buffer.appendLong(userJWTGenerated.userId());
    buffer.appendString(userJWTGenerated.jwt());
  }

  @Override
  public UserJWTGenerated decodeFromWire(int pos, Buffer buffer) {
    // Decode the data from the buffer
    long userId = buffer.getLong(pos);
    String jwt = buffer.getString(pos, pos + Integer.BYTES);
    return new UserJWTGenerated(userId, jwt);
  }

  @Override
  public UserJWTGenerated transform(UserJWTGenerated userJWTGenerated) {
    // You can transform the message if needed, in this case, it's the same object
    return userJWTGenerated;
  }

  @Override
  public String name() {
    // Unique name for the codec
    return "user-jwt-generated";
  }

  @Override
  public byte systemCodecID() {
    // Always -1
    return -1;
  }
}

