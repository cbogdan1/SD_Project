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

    @Operation(summary = "Create a device (userId is optional: the owner)")
    @ApiResponse(responseCode = "201", description = "Device created, returns its id")
    @ApiResponse(responseCode = "400", description = "Invalid device data")
    @ApiResponse(responseCode = "404", description = "The owner (userId) does not exist")
    @PostMapping()
    public ResponseEntity<UUID> insertDevice(@Valid @RequestBody DeviceDTO dto) {
        return new ResponseEntity<>(deviceService.insert(dto), HttpStatus.CREATED);
    }

    @Operation(summary = "Update a device, also used to assign/unassign it to a user")
    @ApiResponse(responseCode = "200", description = "The updated device")
    @ApiResponse(responseCode = "400", description = "Invalid device data")
    @ApiResponse(responseCode = "404", description = "Device or owner (userId) not found")
    @PutMapping(value = EndpointPaths.ID)
    public ResponseEntity<DeviceDTO> updateDevice(@PathVariable("id") UUID id, @Valid @RequestBody DeviceDTO dto) {
        return new ResponseEntity<>(deviceService.update(id, dto), HttpStatus.OK);
    }

    @Operation(summary = "Delete a device")
    @ApiResponse(responseCode = "204", description = "Device deleted")
    @ApiResponse(responseCode = "404", description = "Device not found")
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

    // Called by user-service after it creates a user
    @Operation(summary = "Add a user id to the local app_user table (called by user-service)")
    @ApiResponse(responseCode = "201", description = "User id saved")
    @PostMapping(value = EndpointPaths.USER_ID)
    public ResponseEntity<Void> addUser(@PathVariable("userId") UUID userId) {
        deviceService.addUser(userId);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    // Called by user-service when it deletes a user
    @Operation(summary = "Delete a user id from app_user; the user's devices become unassigned (called by user-service)")
    @ApiResponse(responseCode = "204", description = "User id deleted")
    @DeleteMapping(value = EndpointPaths.USER_ID)
    public ResponseEntity<Void> deleteUser(@PathVariable("userId") UUID userId) {
        deviceService.deleteUser(userId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
