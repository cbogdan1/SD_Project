package ro.tuc.devices.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ro.tuc.devices.entities.AppUser;

import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    // One DELETE statement, without reading the row first (deleteById() would do a SELECT before deleting)
    @Modifying
    @Query("DELETE FROM AppUser u WHERE u.id = :id")
    int deleteDirect(@Param("id") UUID id);
}
