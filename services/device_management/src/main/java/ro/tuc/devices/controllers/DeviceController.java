package ro.tuc.devices.controllers;

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
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping()
    public ResponseEntity<List<DeviceDTO>> getDevices() {
        return new ResponseEntity<>(deviceService.findDevices(), HttpStatus.OK);
    }

    @GetMapping(value = EndpointPaths.ID)
    public ResponseEntity<DeviceDTO> getDevice(@PathVariable("id") UUID id) {
        return new ResponseEntity<>(deviceService.findDeviceById(id), HttpStatus.OK);
    }

    @PostMapping()
    public ResponseEntity<UUID> insertDevice(@Valid @RequestBody DeviceDTO dto) {
        return new ResponseEntity<>(deviceService.insert(dto), HttpStatus.CREATED);
    }

    @PutMapping(value = EndpointPaths.ID)
    public ResponseEntity<DeviceDTO> updateDevice(@PathVariable("id") UUID id, @Valid @RequestBody DeviceDTO dto) {
        return new ResponseEntity<>(deviceService.update(id, dto), HttpStatus.OK);
    }

    @DeleteMapping(value = EndpointPaths.ID)
    public ResponseEntity<Void> deleteDevice(@PathVariable("id") UUID id) {
        deviceService.delete(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping(value = EndpointPaths.USER_ID)
    public ResponseEntity<List<DeviceDTO>> getDevicesByUser(@PathVariable("userId") UUID userId) {
        return new ResponseEntity<>(deviceService.findDevicesByUser(userId), HttpStatus.OK);
    }

    @DeleteMapping(value = EndpointPaths.USER_ID)
    public ResponseEntity<Void> unassignUser(@PathVariable("userId") UUID userId) {
        deviceService.unassignUser(userId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
