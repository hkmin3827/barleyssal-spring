package com.hakyung.barleyssal_spring.global.jwt;

import com.hakyung.barleyssal_spring.domain.user.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtProvider {
    private final JwtProperties jwtProperties;
    private final Key key;

    public JwtProvider(JwtProperties jwtProperties){
        this.jwtProperties = jwtProperties;
        this.key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }


    public String createAccessToken(Long userId, Role role) {
        long expiration = jwtProperties.getExpiration();
        long now = System.currentTimeMillis();

        var builder = Jwts.builder()
                .setId(java.util.UUID.randomUUID().toString())
                .setSubject(String.valueOf(userId))
                .claim("role", role.name())
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + expiration));

        return builder
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    private Claims parseClaims(String accessToken){
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(accessToken)
                .getBody();
    }

    public Long getId(String accessToken) {
        return Long.valueOf(parseClaims(accessToken).getSubject());
    }


    public Role getRole(String accessToken) {
        return Role.valueOf(parseClaims(accessToken).get("role", String.class));
    }
}
