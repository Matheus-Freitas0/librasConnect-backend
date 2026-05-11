package com.librasConnect.system.security;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.librasConnect.system.exception.ApiErrorBody;

@Component
public class JsonAuthFailureHandler implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JsonAuthFailureHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(jakarta.servlet.http.HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), new ApiErrorBody("Não autenticado ou token inválido", "UNAUTHORIZED"));
    }
}
