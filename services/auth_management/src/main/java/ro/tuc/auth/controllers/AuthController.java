package ro.tuc.auth.controllers;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.tuc.auth.dtos.LoginRequest;
import ro.tuc.auth.dtos.RegisterRequest;
import ro.tuc.auth.dtos.TokenResponse;
import ro.tuc.auth.services.AccessPolicy;
import ro.tuc.auth.services.AuthService;
import ro.tuc.auth.services.JwtService;
import ro.tuc.auth.services.JwtService.TokenClaims;
import ro.tuc.common.EndpointPaths;
import ro.tuc.common.handlers.exceptions.model.ForbiddenException;

import javax.validation.Valid;
import java.util.UUID;

@RestController
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping(value = EndpointPaths.LOGIN)
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return new ResponseEntity<>(authService.login(request), HttpStatus.OK);
    }

    @PostMapping(value = EndpointPaths.REGISTER)
    public ResponseEntity<UUID> register(@Valid @RequestBody RegisterRequest request,
                                         @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return new ResponseEntity<>(authService.register(request, authorization), HttpStatus.CREATED);
    }

    /**
     * Traefik ForwardAuth target: 200 lets the request through (with identity headers),
     * 401/403 is returned to the client and the backend is never called.
     */
    @RequestMapping(value = EndpointPaths.VALIDATE)
    public ResponseEntity<Void> validate(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                                         @RequestHeader(value = "X-Forwarded-Method", required = false) String method,
                                         @RequestHeader(value = "X-Forwarded-Uri", required = false) String uri) {
        TokenClaims claims = jwtService.parseBearer(authorization);
        if (!AccessPolicy.isAllowed(claims.getRole(), claims.getUserId(), method, uri)) {
            throw new ForbiddenException(uri);
        }
        return ResponseEntity.ok()
                .header("X-User-Id", claims.getUserId().toString())
                .header("X-User-Role", claims.getRole().name())
                .build();
    }

    // Internal only: Traefik does not route /credentials, user-service calls it directly on the Docker network
    @DeleteMapping(value = EndpointPaths.CREDENTIALS + EndpointPaths.USER_ID)
    public ResponseEntity<Void> deleteCredentials(@PathVariable("userId") UUID userId) {
        authService.deleteByUserId(userId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
