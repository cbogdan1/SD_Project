package ro.tuc.devices.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.tuc.common.EndpointPaths;
import ro.tuc.devices.dtos.DeviceDTO;
import ro.tuc.devices.services.DeviceService;

import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@CrossOrigin
@RequestMapping(value = EndpointPaths.DEVICE)
// every endpoint here needs the JWT (the gateway checks it), so Swagger sends the token for all of them
@SecurityRequirement(name = "bearerAuth")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @Operation(summary = "Get all devices")
    @ApiResponse(responseCode = "200", description = "List of all devices")
    @GetMapping()
    public ResponseEntity<List<DeviceDTO>> getDevices() {
        return new ResponseEntity<>(deviceService.findDevices(), HttpStatus.OK);
    }

    @Operation(summary = "Get one device by id")
    @ApiResponse(responseCode = "200", description = "The device")
    @ApiResponse(responseCode = "404", description = "Device not found")
    @GetMapping(value = EndpointPaths.ID)
    public ResponseEntity<DeviceDTO> getDevice(@PathVariable("id") UUID id) {
        return new ResponseEntity<>(deviceService.findDeviceById(id), HttpStatus.OK);
    }

    @Operation(summary = "Create a device (userId is optional: the owner); publishes DEVICE_CREATED on Kafka")
    @ApiResponse(responseCode = "201", description = "Device created, returns its id")
    @ApiResponse(responseCode = "400", description = "Invalid device data")
    @ApiResponse(responseCode = "404", description = "The owner (userId) does not exist")
    @ApiResponse(responseCode = "503", description = "Kafka is not reachable, device not saved")
    @PostMapping()
    public ResponseEntity<UUID> insertDevice(@Valid @RequestBody DeviceDTO dto) {
        return new ResponseEntity<>(deviceService.insert(dto), HttpStatus.CREATED);
    }

    @Operation(summary = "Update a device, also used to assign/unassign it to a user; publishes DEVICE_UPDATED on Kafka")
    @ApiResponse(responseCode = "200", description = "The updated device")
    @ApiResponse(responseCode = "400", description = "Invalid device data")
    @ApiResponse(responseCode = "404", description = "Device or owner (userId) not found")
    @ApiResponse(responseCode = "503", description = "Kafka is not reachable, device not changed")
    @PutMapping(value = EndpointPaths.ID)
    public ResponseEntity<DeviceDTO> updateDevice(@PathVariable("id") UUID id, @Valid @RequestBody DeviceDTO dto) {
        return new ResponseEntity<>(deviceService.update(id, dto), HttpStatus.OK);
    }

    @Operation(summary = "Delete a device; publishes DEVICE_DELETED on Kafka")
    @ApiResponse(responseCode = "204", description = "Device deleted")
    @ApiResponse(responseCode = "404", description = "Device not found")
    @ApiResponse(responseCode = "503", description = "Kafka is not reachable, device not deleted")
    @DeleteMapping(value = EndpointPaths.ID)
    public ResponseEntity<Void> deleteDevice(@PathVariable("id") UUID id) {
        deviceService.delete(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Operation(summary = "Get the devices of a user")
    @ApiResponse(responseCode = "200", description = "List of the user's devices (empty if none)")
    @GetMapping(value = EndpointPaths.USER_ID)
    public ResponseEntity<List<DeviceDTO>> getDevicesByUser(@PathVariable("userId") UUID userId) {
        return new ResponseEntity<>(deviceService.findDevicesByUser(userId), HttpStatus.OK);
    }

    // The app_user table is no longer filled through REST: SyncEventListener reads USER_CREATED / USER_DELETED from Kafka.
}
