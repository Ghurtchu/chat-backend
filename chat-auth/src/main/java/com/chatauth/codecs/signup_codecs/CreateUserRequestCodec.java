package com.chatauth.codecs.signup_codecs;

import com.chatauth.domain.CreateUser;
import com.chatauth.messages.CreateUserRequest;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;
import io.vertx.core.json.JsonObject;

public class CreateUserRequestCodec implements MessageCodec<CreateUserRequest, CreateUserRequest> {

  @Override
  public void encodeToWire(Buffer buffer, CreateUserRequest createUserRequest) {
    // Convert CreateUserRequest to JSON and write it to the buffer
    JsonObject json = JsonObject.mapFrom(createUserRequest);
    String jsonStr = json.encode();
    buffer.appendInt(jsonStr.length());
    buffer.appendString(jsonStr);
  }

  @Override
  public CreateUserRequest decodeFromWire(int pos, Buffer buffer) {
    // Read JSON from the buffer and convert it back to CreateUserRequest object
    return new CreateUserRequest(CreateUser.fromJson(new JsonObject(buffer.getString(pos, buffer.length()))));
  }

  @Override
  public CreateUserRequest transform(CreateUserRequest createUserRequest) {
    // If the same instance is passed, no need to transform
    return createUserRequest;
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

