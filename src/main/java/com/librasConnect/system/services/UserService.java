package com.librasConnect.system.services;

import com.librasConnect.system.models.User;

public interface UserService {

    User validateLogin(String email, String password);
}
