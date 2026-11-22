package com.Shawn.dream_shops.security.jwt;

import com.Shawn.dream_shops.security.user.ShopUserDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

@Component
public class JwtUtils {

    @Value("${auth.token.jwtSecret}")
    private String jwtSecret;

    @Value("${auth.token.expirationInMils}")
    private long expirationTime;

    private SecretKey signingKey;
    private JwtParser parser;

    @PostConstruct
    void initSigningKey() {
        signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
        parser = Jwts.parser()
                .verifyWith(signingKey)
                .sig().clear().add(Jwts.SIG.HS256).and()
                .build();
    }

    public String generateTokenForUser(Authentication authentication) {
        ShopUserDetails user = (ShopUserDetails) authentication.getPrincipal();
        List<String> roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        Date issuedAt = new Date();
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("id", user.getId())
                .claim("roles", roles)
                .issuedAt(issuedAt)
                .expiration(new Date(issuedAt.getTime() + expirationTime))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public Claims parseClaims(String token) {
        return parser.parseSignedClaims(token).getPayload();
    }
}
