package com.chatauth.services.implementation;

import com.chatauth.services.ValidatePasswordService;
import edu.vt.middleware.password.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ValidatePasswordServiceImpl implements ValidatePasswordService {

  @Override
  public List<String> checkPassword(String password) {
    final var passwordData = new PasswordData(new Password(password));
    final var charRule = new CharacterCharacteristicsRule();
    charRule.setNumberOfCharacteristics(3);
    charRule.setRules(List.of(
        new DigitCharacterRule(1),
        new NonAlphanumericCharacterRule(1),
        new UppercaseCharacterRule(1)));
    final var ruleList = List.of(new LengthRule(8, 16), new WhitespaceRule(), charRule);
    final var validator = new PasswordValidator(ruleList);
    final var result = validator.validate(passwordData);
    return result.isValid() ? List.of() : validator.getMessages(result);
  }
}
