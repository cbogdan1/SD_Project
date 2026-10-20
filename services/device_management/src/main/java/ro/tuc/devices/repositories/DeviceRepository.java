package ro.tuc.devices.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ro.tuc.devices.entities.Device;

import java.util.List;
import java.util.UUID;

public interface DeviceRepository extends JpaRepository<Device, UUID> {

    List<Device> findByUserId(UUID userId);

    @Modifying
    @Query("UPDATE Device d SET d.userId = NULL WHERE d.userId = :userId")
    int unassignAll(@Param("userId") UUID userId);
}
