package ro.tuc.ds2020.controllers;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.tuc.common.EndpointPaths;
import ro.tuc.ds2020.dtos.PersonDTO;
import ro.tuc.ds2020.dtos.PersonDetailsDTO;
import ro.tuc.ds2020.entities.Person;
import ro.tuc.ds2020.services.PersonService;

import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@CrossOrigin
@RequestMapping(value = EndpointPaths.PERSON)
// every endpoint here needs the JWT (the gateway checks it), so Swagger sends the token for all of them
@SecurityRequirement(name = "bearerAuth")
public class PersonController {

    private final PersonService personService;

    @Autowired
    public PersonController(PersonService personService) {
        this.personService = personService;
    }

    @Operation(summary = "Get all users")
    @ApiResponse(responseCode = "200", description = "List of all users")
    @GetMapping()
    public ResponseEntity<List<PersonDTO>> getPersons() {
        List<PersonDTO> dtos = personService.findPersons();
        for (PersonDTO dto : dtos) {
            Link personLink = linkTo(methodOn(PersonController.class)
                    .getPerson(dto.getId())).withRel("personDetails");
            dto.add(personLink);
        }
        return new ResponseEntity<>(dtos, HttpStatus.OK);
    }

    @Operation(summary = "Create a user (called by auth-service on register); also adds the id in device-service")
    @ApiResponse(responseCode = "201", description = "User created, returns its id")
    @ApiResponse(responseCode = "400", description = "Invalid user data (e.g. age under 18)")
    @ApiResponse(responseCode = "503", description = "device-service is not reachable, user not saved")
    @PostMapping()
    public ResponseEntity<UUID> insertProsumer(@Valid @RequestBody PersonDetailsDTO personDTO) {
        UUID personID = personService.insert(personDTO);
        return new ResponseEntity<>(personID, HttpStatus.CREATED);
    }

    @Operation(summary = "Get one user by id")
    @ApiResponse(responseCode = "200", description = "The user")
    @ApiResponse(responseCode = "404", description = "User not found")
    @GetMapping(value = EndpointPaths.ID)
    public ResponseEntity<PersonDetailsDTO> getPerson(@PathVariable("id") UUID personId) {
        PersonDetailsDTO dto = personService.findPersonById(personId);
        return new ResponseEntity<>(dto, HttpStatus.OK);
    }

    @Operation(summary = "Update a user's name, address and age")
    @ApiResponse(responseCode = "200", description = "The updated user")
    @ApiResponse(responseCode = "400", description = "Invalid user data")
    @ApiResponse(responseCode = "404", description = "User not found")
    @PutMapping(value = EndpointPaths.ID)
    public ResponseEntity<PersonDetailsDTO> updatePerson(@PathVariable("id") UUID personId,
                                                         @Valid @RequestBody PersonDetailsDTO personDTO) {
        return new ResponseEntity<>(personService.update(personId, personDTO), HttpStatus.OK);
    }

    @Operation(summary = "Delete a user, its login (auth-service) and its id in device-service")
    @ApiResponse(responseCode = "204", description = "User deleted")
    @ApiResponse(responseCode = "404", description = "User not found")
    @ApiResponse(responseCode = "503", description = "auth-service or device-service not reachable, user kept")
    @DeleteMapping(value = EndpointPaths.ID)
    public ResponseEntity<Void> deletePerson(@PathVariable("id") UUID personId) {
        personService.delete(personId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
