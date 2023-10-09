package com.chatauth.codecs;

import com.chatauth.domain.CreateUser;
import com.chatauth.messages.CreateUserRequest;
import com.chatauth.messages.LoginRequest;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;
import io.vertx.core.json.JsonObject;

public class LoginRequestCodec implements MessageCodec<LoginRequest, LoginRequest> {

  @Override
  public void encodeToWire(Buffer buffer, LoginRequest loginRequest) {
    // Convert CreateUserRequest to JSON and write it to the buffer
    JsonObject json = JsonObject.mapFrom(loginRequest);
    String jsonStr = json.encode();
    buffer.appendInt(jsonStr.length());
    buffer.appendString(jsonStr);
  }

  @Override
  public LoginRequest decodeFromWire(int pos, Buffer buffer) {
    // Read JSON from the buffer and convert it back to CreateUserRequest object
    return new LoginRequest(CreateUser.fromJson(new JsonObject(buffer.getString(pos, buffer.length()))));
  }

  @Override
  public LoginRequest transform(LoginRequest loginRequest) {
    // If the same instance is passed, no need to transform
    return loginRequest;
  }

  @Override
  public String name() {
    // Unique codec name
    return "create-user-request-codec";
  }

  @Override
  public byte systemCodecID() {
    // Always -1
    return -1;
  }
}


