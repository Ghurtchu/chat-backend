package com.chatauth.codecs.signup_codecs;

import com.chatauth.domain.User;
import com.chatauth.messages.UserCreated;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;
import io.vertx.core.json.JsonObject;

public class UserCreatedCodec implements MessageCodec<UserCreated, UserCreated> {

  @Override
  public void encodeToWire(Buffer buffer, UserCreated userCreated) {
    JsonObject json = new JsonObject()
      .put("user", JsonObject.mapFrom(userCreated.user()));
    String jsonString = json.encode();
    buffer.appendInt(jsonString.length());
    buffer.appendString(jsonString);
  }

  @Override
  public UserCreated decodeFromWire(int pos, Buffer buffer) {
    int length = buffer.getInt(pos);
    String jsonString = buffer.getString(pos += 4, pos += length);
    JsonObject json = new JsonObject(jsonString);
    User user = json.mapTo(User.class);
    return new UserCreated(user);
  }

  @Override
  public UserCreated transform(UserCreated userCreated) {
    return userCreated;
  }

  @Override
  public String name() {
    return "userCreated";
  }

  @Override
  public byte systemCodecID() {
    return -1;
  }
}

