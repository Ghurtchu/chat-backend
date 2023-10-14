package com.chatauth.services;

import java.util.List;

public interface ValidatePasswordService {
  List<String> checkPassword(String password);
}
