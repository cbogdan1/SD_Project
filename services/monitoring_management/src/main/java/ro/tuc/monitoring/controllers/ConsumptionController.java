package ro.tuc.monitoring.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ro.tuc.common.EndpointPaths;
import ro.tuc.monitoring.dtos.HourlyConsumptionDTO;
import ro.tuc.monitoring.services.ConsumptionService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@CrossOrigin
@RequestMapping(value = EndpointPaths.CONSUMPTION)
// every endpoint here needs the JWT (the gateway checks it), so Swagger sends the token for all of them
@SecurityRequirement(name = "bearerAuth")
public class ConsumptionController {

    private final ConsumptionService consumptionService;

    public ConsumptionController(ConsumptionService consumptionService) {
        this.consumptionService = consumptionService;
    }

    // The userId is in the path so that the gateway (AccessPolicy) can check that a CLIENT asks only for himself
    @Operation(summary = "Get the hourly energy consumption of one device of a user, for one day")
    @ApiResponse(responseCode = "200", description = "24 values: hour 0..23 and the energy used in that hour (kWh)")
    @ApiResponse(responseCode = "400", description = "Missing or invalid deviceId / date (format: 2026-11-22)")
    @ApiResponse(responseCode = "403", description = "The device does not belong to this user")
    @ApiResponse(responseCode = "404", description = "Device not found")
    @GetMapping(value = EndpointPaths.USER_ID)
    public ResponseEntity<List<HourlyConsumptionDTO>> getDailyConsumption(
            @PathVariable("userId") UUID userId,
            @RequestParam("deviceId") UUID deviceId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return new ResponseEntity<>(consumptionService.getDailyConsumption(userId, deviceId, date), HttpStatus.OK);
    }
}
