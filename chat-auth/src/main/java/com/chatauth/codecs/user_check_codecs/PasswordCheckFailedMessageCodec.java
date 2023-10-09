package com.chatauth.codecs.user_check_codecs;

import com.chatauth.messages.PasswordCheckFailedMessage;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;
import io.vertx.core.json.JsonObject;

public class PasswordCheckFailedMessageCodec implements MessageCodec<PasswordCheckFailedMessage, PasswordCheckFailedMessage> {

  @Override
  public void encodeToWire(Buffer buffer, PasswordCheckFailedMessage message) {
    // Convert the reasonForFailure to bytes and write it to the buffer
    JsonObject json = JsonObject.mapFrom(message);
    String jsonStr = json.encode();
    buffer.appendInt(jsonStr.length());
    buffer.appendString(jsonStr);
    buffer.appendString(message.reasonForFailure());
  }

  @Override
  public PasswordCheckFailedMessage decodeFromWire(int pos, Buffer buffer) {
    // Read the reasonForFailure from the buffer and create a new PasswordCheckFailedMessage instance
    String reasonForFailure = buffer.getString(pos, buffer.length());
    return new PasswordCheckFailedMessage(reasonForFailure);
  }

  @Override
  public PasswordCheckFailedMessage transform(PasswordCheckFailedMessage message) {
    // In this case, the message is the same before and after transformation
    return message;
  }

  @Override
  public String name() {
    // Unique name for the codec
    return "passwordCheckFailedMessageCodec";
  }

  @Override
  public byte systemCodecID() {
    // Always -1
    return -1;
  }
}
