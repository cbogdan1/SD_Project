package ro.tuc.auth.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import ro.tuc.auth.dtos.LoginRequest;
import ro.tuc.auth.dtos.RegisterRequest;
import ro.tuc.auth.dtos.TokenResponse;
import ro.tuc.auth.entities.Credential;
import ro.tuc.auth.entities.Role;
import ro.tuc.auth.repositories.CredentialRepository;
import ro.tuc.common.EndpointPaths;
import ro.tuc.common.handlers.exceptions.model.CustomException;
import ro.tuc.common.handlers.exceptions.model.DuplicateResourceException;
import ro.tuc.common.handlers.exceptions.model.ForbiddenException;
import ro.tuc.common.handlers.exceptions.model.UnauthorizedException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthService.class);

    private final CredentialRepository credentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RestTemplate restTemplate;
    private final String userServiceUrl;
    private final String adminUsername;
    private final String adminPassword;

    public AuthService(CredentialRepository credentialRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       RestTemplate restTemplate,
                       @Value("${services.user.url}") String userServiceUrl,
                       @Value("${admin.username}") String adminUsername,
                       @Value("${admin.password}") String adminPassword) {
        this.credentialRepository = credentialRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.restTemplate = restTemplate;
        this.userServiceUrl = userServiceUrl;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    public TokenResponse login(LoginRequest request) {
        Credential credential = credentialRepository.findByUsername(request.getUsername())
                .filter(c -> passwordEncoder.matches(request.getPassword(), c.getPasswordHash()))
                .orElseThrow(() -> new UnauthorizedException("Credential"));
        return new TokenResponse(jwtService.createToken(credential), credential.getRole(), credential.getUserId());
    }

    /** Creates the Person in user-service, then the credential linked to it. Returns the new user id. */
    public UUID register(RegisterRequest request, String authorizationHeader) {
        Role role = request.getRole() == null ? Role.CLIENT : request.getRole();
        if (role == Role.ADMIN && jwtService.parseBearer(authorizationHeader).getRole() != Role.ADMIN) {
            throw new ForbiddenException("Role ADMIN");
        }
        if (credentialRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username " + request.getUsername());
        }
        UUID userId = createPerson(request);
        credentialRepository.save(new Credential(request.getUsername(),
                passwordEncoder.encode(request.getPassword()), role, userId));
        LOGGER.debug("Registered {} with role {} and user id {}", request.getUsername(), role, userId);
        return userId;
    }

    /** Called by user-service when a person is deleted. */
    @Transactional
    public void deleteByUserId(UUID userId) {
        credentialRepository.deleteByUserId(userId);
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!credentialRepository.existsByUsername(adminUsername)) {
            credentialRepository.save(new Credential(adminUsername, passwordEncoder.encode(adminPassword),
                    Role.ADMIN, UUID.randomUUID()));
            LOGGER.info("Seeded admin account '{}'", adminUsername);
        }
    }

    private UUID createPerson(RegisterRequest request) {
        Map<String, Object> person = Map.of(
                "name", request.getName(),
                "address", request.getAddress(),
                "age", request.getAge());
        try {
            return restTemplate.postForObject(userServiceUrl + EndpointPaths.PERSON, person, UUID.class);
        } catch (HttpStatusCodeException e) {
            // e.g. 400 when user-service rejects the age; pass its status and reason through
            throw new CustomException("User profile rejected", e.getStatusCode(), "Person",
                    List.of(e.getResponseBodyAsString()));
        } catch (ResourceAccessException e) {
            throw new CustomException("User service unavailable", HttpStatus.SERVICE_UNAVAILABLE, "Person",
                    List.of());
        }
    }
}
