package ro.tuc.auth.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.tuc.auth.entities.Credential;

import java.util.Optional;
import java.util.UUID;

public interface CredentialRepository extends JpaRepository<Credential, UUID> {

    Optional<Credential> findByUsername(String username);

    boolean existsByUsername(String username);

    void deleteByUserId(UUID userId);
}
