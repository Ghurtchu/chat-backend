package com.chatauth.services.implementation;

import com.chatauth.services.ValidatePasswordService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class ValidatePasswordServiceImplTest {

  ValidatePasswordService impl = new ValidatePasswordServiceImpl();

  @Test
  public void testLackingUppercaseChararacter() {
    final var pass = "mate1#";
    final var failureReasons = impl.checkPassword(pass);
    final var expected = "Password must be at least 8 characters in length.";

    Assertions.assertEquals(expected, failureReasons.get(0));
  }

  @Test
  public void testLackingDigitCharacter() {
    final var pass = "onetwothree#";
    final var failureReasons = impl.checkPassword(pass);
    final var expected = "Password must contain at least 1 digit characters.";

    Assertions.assertEquals(expected, failureReasons.get(0));
  }

  @Test
  public void testLackingNonAlphanumericCharacter() {
    final var pass = "onetwothree1";
    final var failureReasons = impl.checkPassword(pass);
    final var expected = "Password must contain at least 1 non-alphanumeric characters.";

    Assertions.assertEquals(expected, failureReasons.get(0));
  }

  @Test
  public void testSuccessCase() {
    final var pass = "Onetwothree#1";
    final var failureReasons = impl.checkPassword(pass);
    final var expected = List.of();

    Assertions.assertEquals(expected, failureReasons);
  }

  @Test
  public void test() {
    final var pass = "One";
    final var failureReasons = impl.checkPassword(pass);
    final var expected = List.of();

    var res = failureReasons
            .stream()
            .reduce("", (each, acc) -> acc.concat(":").concat(each));

    System.out.printf(res);
    Assertions.assertEquals(expected, failureReasons);
  }
}
