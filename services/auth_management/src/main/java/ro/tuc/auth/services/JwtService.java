package ro.tuc.auth.services;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ro.tuc.auth.entities.Credential;
import ro.tuc.auth.entities.Role;
import ro.tuc.common.handlers.exceptions.model.UnauthorizedException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private static final String ROLE_CLAIM = "role";
    private static final String USER_ID_CLAIM = "userId";

    private final SecretKey key;
    private final Duration expiration;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.expiration-minutes}") long expirationMinutes) {
        // throws WeakKeyException at startup if the secret is shorter than 32 bytes
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = Duration.ofMinutes(expirationMinutes);
    }

    public String createToken(Credential credential) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(credential.getUsername())
                .claim(ROLE_CLAIM, credential.getRole().name())
                .claim(USER_ID_CLAIM, credential.getUserId().toString())
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expiration.toMillis()))
                .signWith(key)
                .compact();
    }

    /** Verifies signature and expiry of a raw "Bearer ..." header value. */
    public TokenClaims parseBearer(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Token");
        }
        try {
            Claims claims = Jwts.parserBuilder().setSigningKey(key).build()
                    .parseClaimsJws(authorizationHeader.substring("Bearer ".length()))
                    .getBody();
            return new TokenClaims(Role.valueOf(claims.get(ROLE_CLAIM, String.class)),
                    UUID.fromString(claims.get(USER_ID_CLAIM, String.class)));
        } catch (JwtException | IllegalArgumentException | NullPointerException e) {
            throw new UnauthorizedException("Token");
        }
    }

    public static class TokenClaims {
        private final Role role;
        private final UUID userId;

        public TokenClaims(Role role, UUID userId) {
            this.role = role;
            this.userId = userId;
        }

        public Role getRole() {
            return role;
        }

        public UUID getUserId() {
            return userId;
        }
    }
}
