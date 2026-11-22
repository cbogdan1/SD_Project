package ro.tuc.monitoring.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ro.tuc.monitoring.entities.Device;

import java.util.UUID;

public interface DeviceRepository extends JpaRepository<Device, UUID> {

    // One DELETE statement (no SELECT first); the database also deletes the device's hourly rows (ON DELETE CASCADE)
    @Modifying
    @Query("DELETE FROM Device d WHERE d.id = :id")
    int deleteDirect(@Param("id") UUID id);

    // A deleted user's devices become unassigned, like the foreign key does in device-service
    @Modifying
    @Query("UPDATE Device d SET d.userId = NULL WHERE d.userId = :userId")
    int unassignUser(@Param("userId") UUID userId);
}
