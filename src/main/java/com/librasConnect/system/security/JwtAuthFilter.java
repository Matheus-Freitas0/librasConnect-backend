package com.librasConnect.system.security;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.librasConnect.system.exception.ApiErrorBody;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtAuthFilter extends OncePerRequestFilter implements Ordered {

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;
    private final int maxBodyBytes;

    public JwtAuthFilter(
            JwtService jwtService,
            ObjectMapper objectMapper,
            @Value("${app.api.max-body-bytes:524288}") int maxBodyBytes) {
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
        this.maxBodyBytes = maxBodyBytes;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (rejectOversizedV1Body(request, response)) {
            return;
        }

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7).trim();
        if (token.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!jwtService.isTokenValid(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(response.getOutputStream(),
                    new ApiErrorBody("Token inválido ou expirado. Faça login novamente.", "INVALID_TOKEN"));
            return;
        }

        String email = jwtService.extractEmail(token);
        Collection<String> rules = jwtService.extractRules(token);

        List<SimpleGrantedAuthority> authorities = rules.stream()
                .map(rule -> new SimpleGrantedAuthority("ROLE_" + rule))
                .toList();

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(email, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
        filterChain.doFilter(request, response);
    }

    private boolean rejectOversizedV1Body(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        if (!"POST".equalsIgnoreCase(request.getMethod()) && !"PATCH".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String uri = request.getRequestURI();
        if (uri == null || !uri.contains("/api/v1/")) {
            return false;
        }
        long len = request.getContentLengthLong();
        if (len > 0 && len > maxBodyBytes) {
            response.setStatus(413);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(response.getOutputStream(),
                    new ApiErrorBody("Corpo da requisição excede o limite permitido", null));
            return true;
        }
        return false;
    }
}
