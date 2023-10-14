package com.chatauth.codecs.signup_codecs;

import com.chatauth.domain.CreateUser;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;
import io.vertx.core.json.JsonObject;

public class CreateUserCodec implements MessageCodec<CreateUser, CreateUser> {

  @Override
  public void encodeToWire(Buffer buffer, CreateUser createUser) {
    JsonObject json = JsonObject.mapFrom(createUser);
    String jsonStr = json.encode();
    buffer.appendInt(jsonStr.length());
    buffer.appendString(jsonStr);
  }

  @Override
  public CreateUser decodeFromWire(int pos, Buffer buffer) {
    int length = buffer.getInt(pos);
    pos += 4;
    String jsonStr = buffer.getString(pos, pos + length);
    JsonObject json = new JsonObject(jsonStr);
    return json.mapTo(CreateUser.class);
  }

  @Override
  public CreateUser transform(CreateUser createUser) {
    return createUser;
  }

  @Override
  public String name() {
    return "create-user-codec";
  }

  @Override
  public byte systemCodecID() {
    return -1;
  }
}
