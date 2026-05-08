package com.librasConnect.system.services;

import com.librasConnect.system.models.User;

public interface UserService {

    User createUser(String name, String email, String password);

    User validateLogin(String email, String password);
}
