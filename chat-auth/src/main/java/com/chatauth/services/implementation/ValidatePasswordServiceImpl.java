package com.chatauth.services.implementation;

import com.chatauth.services.ValidatePasswordService;
import edu.vt.middleware.password.*;

import java.util.ArrayList;
import java.util.List;

public class ValidatePasswordServiceImpl implements ValidatePasswordService {
  public static String checkPassword(String password) {
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
