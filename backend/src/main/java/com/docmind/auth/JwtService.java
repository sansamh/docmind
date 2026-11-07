package com.docmind.auth;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import com.docmind.config.AppProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(AppProperties appProperties) {
        this.key = Keys.hmacShaKeyFor(appProperties.getJwt().getSecret().getBytes());
        this.expirationMs = appProperties.getJwt().getExpirationMs();
    }

    public String generateToken(User user) {
        Date now = new Date();
        return Jwts.builder()
            .subject(user.getEmail())
            .claim("uid", user.getId())
            .issuedAt(now)
            .expiration(new Date(now.getTime() + expirationMs))
            .signWith(key)
            .compact();
    }

    /**
     * @return the claims if valid, null otherwise
     */
    public Claims parse(String token) {
        try {
            return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}
