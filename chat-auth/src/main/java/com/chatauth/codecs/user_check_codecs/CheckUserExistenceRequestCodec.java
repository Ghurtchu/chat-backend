package com.chatauth.codecs.user_check_codecs;

import com.chatauth.domain.CreateUser;
import com.chatauth.messages.CheckUserExistenceRequest;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;
import io.vertx.core.json.JsonObject;

public class CheckUserExistenceRequestCodec implements MessageCodec<CheckUserExistenceRequest, CheckUserExistenceRequest> {

  @Override
  public void encodeToWire(Buffer buffer, CheckUserExistenceRequest checkUserExistenceRequest) {
    JsonObject json = JsonObject.mapFrom(checkUserExistenceRequest);
    String str = json.encode();
    buffer.appendInt(str.length());
    buffer.appendString(str);
  }

  @Override
  public CheckUserExistenceRequest decodeFromWire(int pos, Buffer buffer) {
    int length = buffer.getInt(pos);
    pos += 4;
    String jsonStr = buffer.getString(pos, pos + length);
    JsonObject json = new JsonObject(jsonStr);
    return new CheckUserExistenceRequest(json.mapTo(CreateUser.class));
  }
  @Override
  public CheckUserExistenceRequest transform(CheckUserExistenceRequest checkUserExistenceRequest) {
    return checkUserExistenceRequest;
  }

  @Override
  public String name() {
    return this.getClass().getSimpleName();
  }

  @Override
  public byte systemCodecID() {
    return -1;
  }
}

