package ro.tuc.ds2020.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import ro.tuc.common.EndpointPaths;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@CrossOrigin
// /users/ also goes through the gateway token check (admin only)
@SecurityRequirement(name = "bearerAuth")
public class IndexController {

    @Operation(summary = "Check that the service is running")
    @ApiResponse(responseCode = "200", description = "Status message")
    @GetMapping(value = EndpointPaths.ROOT)
    public ResponseEntity<String> getStatus() {
        return new ResponseEntity<>("City APP Service is running...", HttpStatus.OK);
    }
}
