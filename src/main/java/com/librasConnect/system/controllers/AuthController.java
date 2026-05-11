package com.librasConnect.system.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.librasConnect.system.dtos.request.LoginRequestDto;
import com.librasConnect.system.dtos.response.ResponseToken;
import com.librasConnect.system.models.User;
import com.librasConnect.system.security.JwtService;
import com.librasConnect.system.services.UserService;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public ResponseEntity<ResponseToken> login(@RequestBody LoginRequestDto dto) {
        User user = userService.validateLogin(dto.getEmail(), dto.getPassword());
        String token = jwtService.generateToken(user.getEmail(), user.getName(), user.getRules());
        return ResponseEntity.ok(new ResponseToken(token));
    }
}
