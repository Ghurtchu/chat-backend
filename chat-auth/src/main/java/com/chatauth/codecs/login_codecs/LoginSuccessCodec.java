package com.chatauth.codecs.login_codecs;

import com.chatauth.domain.User;
import com.chatauth.messages.login_messages.LoginSuccess;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;
import io.vertx.core.json.JsonObject;

public class LoginSuccessCodec implements MessageCodec<LoginSuccess, LoginSuccess> {

  @Override
  public void encodeToWire(Buffer buffer, LoginSuccess loginSuccess) {
    JsonObject json = new JsonObject()
      .put("user", JsonObject.mapFrom(loginSuccess.user()));
    String jsonString = json.encode();
    buffer.appendInt(jsonString.length());
    buffer.appendString(jsonString);
  }

  @Override
  public LoginSuccess decodeFromWire(int pos, Buffer buffer) {
    int length = buffer.getInt(pos);
    String jsonString = buffer.getString(pos += 4, pos += length);
    JsonObject json = new JsonObject(jsonString);
    User user = json.mapTo(User.class);
    return new LoginSuccess(user);
  }

  @Override
  public LoginSuccess transform(LoginSuccess loginSuccess) {
    return loginSuccess;
  }

  @Override
  public String name() {
    return "login-success-codec";
  }

  @Override
  public byte systemCodecID() {
    return -1;
  }
}

