package ro.tuc.auth.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.tuc.auth.dtos.LoginRequest;
import ro.tuc.auth.dtos.RegisterRequest;
import ro.tuc.auth.dtos.TokenResponse;
import ro.tuc.auth.entities.Credential;
import ro.tuc.auth.entities.Role;
import ro.tuc.auth.kafka.SyncEventPublisher;
import ro.tuc.auth.repositories.CredentialRepository;
import ro.tuc.common.events.SyncEvent;
import ro.tuc.common.events.SyncEventType;
import ro.tuc.common.handlers.exceptions.model.DuplicateResourceException;
import ro.tuc.common.handlers.exceptions.model.ForbiddenException;
import ro.tuc.common.handlers.exceptions.model.UnauthorizedException;

import java.util.UUID;

@Service
public class AuthService implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthService.class);

    // profile of the seeded admin account (it gets a person in user-service like any other user)
    private static final String ADMIN_NAME = "Administrator";
    private static final String ADMIN_ADDRESS = "-";
    private static final int ADMIN_AGE = 18;

    private final CredentialRepository credentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SyncEventPublisher syncEventPublisher;
    private final String adminUsername;
    private final String adminPassword;

    public AuthService(CredentialRepository credentialRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       SyncEventPublisher syncEventPublisher,
                       @Value("${admin.username}") String adminUsername,
                       @Value("${admin.password}") String adminPassword) {
        this.credentialRepository = credentialRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.syncEventPublisher = syncEventPublisher;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    public TokenResponse login(LoginRequest request) {
        Credential credential = credentialRepository.findByUsername(request.getUsername())
                .filter(c -> passwordEncoder.matches(request.getPassword(), c.getPasswordHash()))
                .orElseThrow(() -> new UnauthorizedException("Credential"));
        return new TokenResponse(jwtService.createToken(credential), credential.getRole(), credential.getUserId());
    }

    /**
     * Generates the user id, saves the login, then publishes USER_CREATED with the profile on Kafka.
     * user-service creates the person and device-service adds the id to app_user when they read the event.
     * If Kafka is down, publish() throws and @Transactional rolls back the login, so nothing is half-created.
     */
    @Transactional
    public UUID register(RegisterRequest request, String authorizationHeader) {
        Role role = request.getRole() == null ? Role.CLIENT : request.getRole();
        if (role == Role.ADMIN && jwtService.parseBearer(authorizationHeader).getRole() != Role.ADMIN) {
            throw new ForbiddenException("Role ADMIN");
        }
        if (credentialRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username " + request.getUsername());
        }
        UUID userId = UUID.randomUUID();
        credentialRepository.save(new Credential(request.getUsername(),
                passwordEncoder.encode(request.getPassword()), role, userId));
        syncEventPublisher.publish(userCreated(userId, request.getUsername(),
                request.getName(), request.getAddress(), request.getAge()));
        LOGGER.info("Registered {} with role {} and user id {}", request.getUsername(), role, userId);
        return userId;
    }

    /** USER_DELETED event (the person was deleted in user-service): the user cannot log in anymore. */
    @Transactional
    public void deleteByUserId(UUID userId) {
        credentialRepository.deleteByUserId(userId);
        LOGGER.info("Deleted the login of user {}", userId);
    }

    /**
     * Creates the admin login if it is missing, then publishes USER_CREATED for it on every start.
     * The consumers ignore a user they already have, so this also gives older databases (admin login without
     * a person / app_user row) the missing rows, and a failed publish is simply retried on the next start.
     */
    @Override
    public void run(ApplicationArguments args) {
        Credential admin = credentialRepository.findByUsername(adminUsername).orElseGet(() -> {
            LOGGER.info("Seeding admin account '{}'", adminUsername);
            return credentialRepository.save(new Credential(adminUsername, passwordEncoder.encode(adminPassword),
                    Role.ADMIN, UUID.randomUUID()));
        });
        syncEventPublisher.publish(userCreated(admin.getUserId(), adminUsername, ADMIN_NAME, ADMIN_ADDRESS, ADMIN_AGE));
    }

    private static SyncEvent userCreated(UUID userId, String username, String name, String address, int age) {
        SyncEvent event = new SyncEvent(SyncEventType.USER_CREATED, userId, null);
        event.setUsername(username);
        event.setName(name);
        event.setAddress(address);
        event.setAge(age);
        return event;
    }
}
