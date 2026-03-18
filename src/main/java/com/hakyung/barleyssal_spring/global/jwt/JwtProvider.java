package com.hakyung.barleyssal_spring.global.jwt;

import com.hakyung.barleyssal_spring.domain.user.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtProvider {

    private final JwtProperties jwtProperties;
    private final SecretKey key;
    private final SecureRandom secureRandom = new SecureRandom();

    public JwtProvider(JwtProperties jwtProperties){
        this.jwtProperties = jwtProperties;
        this.key = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(Long userId, Role role) {
        long expiration = jwtProperties.expiration();
        long now = System.currentTimeMillis();

        return Jwts.builder()
                .header().add("typ", "JWT").and()
                .id(generateCompactId())
                .subject(String.valueOf(userId))
                .claim("role", role.name())
                .issuedAt(new Date(now))
                .expiration(new Date(now + expiration))
                .signWith(key)
                .compact();
    }

    private String generateCompactId() {
        byte[] randomBytes = new byte[9]; // Base64 변환 시 12글자
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private Claims parseClaims(String accessToken) {
        // parserBuilder() -> parser()로 통합 및 verifyWith 적용
        return Jwts.parser()
                .verifyWith(key) // setSigningKey -> verifyWith
                .build()
                .parseSignedClaims(accessToken) // parseClaimsJws -> parseSignedClaims
                .getPayload(); // getBody -> getPayload
    }

    public Long getId(String accessToken) {
        return Long.valueOf(parseClaims(accessToken).getSubject());
    }


    public Role getRole(String accessToken) {
        return Role.valueOf(parseClaims(accessToken).get("role", String.class));
    }
}
