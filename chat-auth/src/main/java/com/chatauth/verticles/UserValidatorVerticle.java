package com.chatauth.verticles;

import com.chatauth.messages.*;
import edu.vt.middleware.password.*;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.jdbc.JDBCClient;
import org.hibernate.tool.schema.internal.exec.ScriptTargetOutputToFile;

import java.util.ArrayList;
import java.util.List;

public class UserValidatorVerticle extends AbstractVerticle {
  private final JDBCClient jdbcClient;

  public UserValidatorVerticle(JDBCClient client) {
    this.jdbcClient = client;
  }

  @Override
  public void start() {
    var bus = vertx.eventBus();
    bus.consumer(VerticlePathConstants.VALIDATE_USER, msg -> {
        final var body = msg.body();
        // message from SignupVerticle
        if (body instanceof CheckUserExistenceRequest req) {
          // check if user exists in db
          // if not send request to ADD_USER_REPO,
          // else ???
          final var createUser = req.createUser();
          final var username = createUser.username();
          final var password = createUser.password();
          final String passwordCheck = checkPassword(password);
          if (passwordCheck.equals("valid")) {
            jdbcClient.getConnection(asyncConnection -> {
              asyncConnection.map(connection ->
                connection.queryWithParams(
                  "SELECT COUNT(*) FROM \"user\" WHERE username = ?",
                  new JsonArray().add(username),
                  asyncQueryResult -> {
                    // if query was successful
                    if (asyncQueryResult.succeeded()) {
                      // if 0 was returned = username does not exist
                      System.out.println(asyncQueryResult.result().getResults().get(0));
                      var result = asyncQueryResult.result().getResults().get(0);
                      var expected = new JsonArray().add(0, 0L);
                      if (result.equals(expected)) {
                        // proceed, user does not exist
                        // bus.send()
                        bus.send(
                          VerticlePathConstants.ADD_USER_REPO,
                          new AddUserToDatabase(createUser)
                        );
                      } else {
                        // user already exists, responds to HttpServerVerticle
                        bus.send(
                          VerticlePathConstants.HTTP_REPLY,
                          UserAlreadyExists.getInstance()
                        );
                      }
                    } else {
                      final var failureReason = asyncQueryResult.cause();
                      // database operation failed
                      // TODO: create singleton object for DB operation failure
                      // public record DbOpsFailed(String message) { }
                      bus.send(
                        VerticlePathConstants.HTTP_REPLY,
                        "db operation failed"
                      );
                    }
                  }
                ));
            });
          }
          else
            bus.send(VerticlePathConstants.HTTP_REPLY, new PasswordCheckFailedMessage(passwordCheck));
        }
      });

  }
  public String checkPassword(String password) {
    PasswordData passwordData = new PasswordData(new Password(password));

    LengthRule lengthRule = new LengthRule(8, 16);

    WhitespaceRule whitespaceRule = new WhitespaceRule();

    CharacterCharacteristicsRule charRule = new CharacterCharacteristicsRule();
    charRule.getRules().add(new DigitCharacterRule(1));
    charRule.getRules().add(new NonAlphanumericCharacterRule(1));
    charRule.getRules().add(new UppercaseCharacterRule(1));
    charRule.getRules().add(new LowercaseCharacterRule(1));
    charRule.setNumberOfCharacteristics(3);

    List<Rule> ruleList = new ArrayList<Rule>();
    ruleList.add(lengthRule);
    ruleList.add(whitespaceRule);
    ruleList.add(charRule);

    PasswordValidator validator = new PasswordValidator(ruleList);

    RuleResult result = validator.validate(passwordData);
    if (result.isValid()) {
      return "valid";
    } else {
      StringBuilder failureMessage = new StringBuilder("Invalid password:");
      for (String msg : validator.getMessages(result)) {
        failureMessage.append("\n").append(msg);
      }
      return failureMessage.toString();
    }
  }
}
