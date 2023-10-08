package com.chatauth.codecs;
import com.chatauth.domain.CreateUser;
import com.chatauth.messages.AddUserToDatabase;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;
import io.vertx.core.json.JsonObject;

public class AddUserToDatabaseCodec implements MessageCodec<AddUserToDatabase, AddUserToDatabase> {

  @Override
  public void encodeToWire(Buffer buffer, AddUserToDatabase addUserToDatabase) {
    JsonObject json = new JsonObject();
    json.put("createUser", JsonObject.mapFrom(addUserToDatabase.createUser()));
    buffer.appendBytes(json.encode().getBytes());
  }

  @Override
  public AddUserToDatabase decodeFromWire(int position, Buffer buffer) {
    JsonObject json = new JsonObject(buffer.getString(position, buffer.length()));
    return new AddUserToDatabase(json.mapTo(CreateUser.class));
  }

  @Override
  public AddUserToDatabase transform(AddUserToDatabase addUserToDatabase) {
    return new AddUserToDatabase(addUserToDatabase.createUser());
  }

  @Override
  public String name() {
    return AddUserToDatabaseCodec.class.getName();
  }

  @Override
  public byte systemCodecID() {
    return -1;
  }
}

