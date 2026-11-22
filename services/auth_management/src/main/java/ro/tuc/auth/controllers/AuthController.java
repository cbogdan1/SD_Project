package ro.tuc.auth.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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

    @Operation(summary = "Log in with username and password, returns a JWT")
    @ApiResponse(responseCode = "200", description = "Token, role and user id")
    @ApiResponse(responseCode = "401", description = "Wrong username or password")
    @PostMapping(value = EndpointPaths.LOGIN)
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return new ResponseEntity<>(authService.login(request), HttpStatus.OK);
    }

    @Operation(summary = "Register a user: saves the login here and publishes USER_CREATED (with the profile) on Kafka")
    @ApiResponse(responseCode = "201", description = "User registered, returns the new user id")
    @ApiResponse(responseCode = "400", description = "Invalid data (e.g. short password, missing name/address, age under 18)")
    @ApiResponse(responseCode = "403", description = "Role ADMIN requested without an admin token")
    @ApiResponse(responseCode = "409", description = "Username already exists")
    @ApiResponse(responseCode = "503", description = "Kafka not reachable, user not saved")
    @PostMapping(value = EndpointPaths.REGISTER)
    public ResponseEntity<UUID> register(@Valid @RequestBody RegisterRequest request,
                                         @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return new ResponseEntity<>(authService.register(request, authorization), HttpStatus.CREATED);
    }

    /**
     * Traefik ForwardAuth target: 200 lets the request through (with identity headers),
     * 401/403 is returned to the client and the backend is never called.
     */
    // only /validate reads the token; login and register stay public (no lock in Swagger)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Check the token and the role before Traefik forwards a request (internal)")
    @ApiResponse(responseCode = "200", description = "Allowed; X-User-Id and X-User-Role headers are set")
    @ApiResponse(responseCode = "401", description = "Missing, invalid or expired token")
    @ApiResponse(responseCode = "403", description = "Valid token, but the role may not call this path")
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
}
