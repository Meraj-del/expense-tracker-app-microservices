package authSerivce.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.UUID;
import java.util.function.Function;

@Service
public class JwtService {

    private static final String SECRET_KEY = "THIS_IS_A_VERY_STRONG_SECRET_KEY_32_CHARS_MIN";

    private Key getSignKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
    }

    public Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSignKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            return null;
        }
    }

    public String extractUsername(String token) {
        Claims claims = extractAllClaims(token);
        return claims != null ? claims.getSubject() : null;
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = extractAllClaims(token);
        return claims != null ? resolver.apply(claims) : null;
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username != null
                && username.equals(userDetails.getUsername())
                && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        Date expiration = extractClaim(token, Claims::getExpiration);
        return expiration == null || expiration.before(new Date());
    }

    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setId(UUID.randomUUID().toString())  // unique ID for token
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24)) // 24 hours
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }
}
