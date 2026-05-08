package com.librasConnect.system.security;

import java.util.Collection;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.librasConnect.system.enums.Rule;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    private static final String CLAIM_NAME = "name";
    private static final String CLAIM_RULES = "rules";

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(String email, String name, Collection<Rule> rules) {
        return Jwts.builder()
                .subject(email)
                .claim(CLAIM_NAME, name)
                .claim(CLAIM_RULES, rules.stream().map(Enum::name).toList())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24))
                .signWith(getKey())
                .compact();
    }

    public String extractEmail(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public String extractName(String token) {
        Object name = Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get(CLAIM_NAME);
        return name == null ? null : name.toString();
    }

    public Collection<String> extractRules(String token) {
        Object rules = Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get(CLAIM_RULES);
        if (rules instanceof Collection<?> collection) {
            return collection.stream().map(String::valueOf).toList();
        }
        return java.util.List.of();
    }

    public boolean isTokenValid(String token) {
        try {
            extractEmail(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
