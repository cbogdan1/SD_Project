package ro.tuc.auth.dtos;

import ro.tuc.auth.entities.Role;

import java.util.UUID;

public class TokenResponse {

    private final String token;
    private final Role role;
    private final UUID userId;

    public TokenResponse(String token, Role role, UUID userId) {
        this.token = token;
        this.role = role;
        this.userId = userId;
    }

    public String getToken() {
        return token;
    }

    public Role getRole() {
        return role;
    }

    public UUID getUserId() {
        return userId;
    }
}
