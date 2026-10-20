package ro.tuc.auth.services;

import org.junit.jupiter.api.Test;
import ro.tuc.auth.entities.Credential;
import ro.tuc.auth.entities.Role;
import ro.tuc.common.handlers.exceptions.model.UnauthorizedException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-123";

    private final JwtService jwtService = new JwtService(SECRET, 60);

    @Test
    void tokenRoundTripKeepsRoleAndUserId() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.createToken(new Credential("ana", "hash", Role.CLIENT, userId));

        JwtService.TokenClaims claims = jwtService.parseBearer("Bearer " + token);

        assertEquals(Role.CLIENT, claims.getRole());
        assertEquals(userId, claims.getUserId());
    }

    @Test
    void rejectsMissingTamperedForeignAndExpiredTokens() {
        String token = jwtService.createToken(new Credential("ana", "hash", Role.CLIENT, UUID.randomUUID()));
        String foreign = new JwtService("another-secret-another-secret-12345", 60)
                .createToken(new Credential("eve", "hash", Role.ADMIN, UUID.randomUUID()));
        String expired = new JwtService(SECRET, -1)
                .createToken(new Credential("ana", "hash", Role.CLIENT, UUID.randomUUID()));

        assertThrows(UnauthorizedException.class, () -> jwtService.parseBearer(null));
        assertThrows(UnauthorizedException.class, () -> jwtService.parseBearer(token));
        assertThrows(UnauthorizedException.class, () -> jwtService.parseBearer("Bearer " + token + "x"));
        assertThrows(UnauthorizedException.class, () -> jwtService.parseBearer("Bearer " + foreign));
        assertThrows(UnauthorizedException.class, () -> jwtService.parseBearer("Bearer " + expired));
    }
}
