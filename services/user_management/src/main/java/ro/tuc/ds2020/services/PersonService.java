package ro.tuc.ds2020.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import ro.tuc.common.EndpointPaths;
import ro.tuc.common.handlers.exceptions.model.CustomException;
import ro.tuc.common.handlers.exceptions.model.ResourceNotFoundException;
import ro.tuc.ds2020.dtos.PersonDTO;
import ro.tuc.ds2020.dtos.PersonDetailsDTO;
import ro.tuc.ds2020.dtos.builders.PersonBuilder;
import ro.tuc.ds2020.entities.Person;
import ro.tuc.ds2020.repositories.PersonRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PersonService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PersonService.class);
    private final PersonRepository personRepository;
    private final RestTemplate restTemplate;
    private final String authServiceUrl;
    private final String deviceServiceUrl;

    @Autowired
    public PersonService(PersonRepository personRepository,
                         RestTemplate restTemplate,
                         @Value("${services.auth.url}") String authServiceUrl,
                         @Value("${services.device.url}") String deviceServiceUrl) {
        this.personRepository = personRepository;
        this.restTemplate = restTemplate;
        this.authServiceUrl = authServiceUrl;
        this.deviceServiceUrl = deviceServiceUrl;
    }

    public List<PersonDTO> findPersons() {
        List<Person> personList = personRepository.findAll();
        return personList.stream()
                .map(PersonBuilder::toPersonDTO)
                .collect(Collectors.toList());
    }

    public PersonDetailsDTO findPersonById(UUID id) {
        return PersonBuilder.toPersonDetailsDTO(getOrThrow(id));
    }

    /** Saves the person in the User DB first, then tells device-service to add the id to its app_user table. */
    public UUID insert(PersonDetailsDTO personDTO) {
        Person person = PersonBuilder.toEntity(personDTO);
        person = personRepository.save(person);
        LOGGER.debug("Person with id {} was inserted in db", person.getId());
        String userPath = EndpointPaths.USER_ID.replace("{userId}", person.getId().toString());
        try {
            restTemplate.postForEntity(deviceServiceUrl + EndpointPaths.DEVICE + userPath, null, Void.class);
        } catch (RestClientException e) {
            // device-service did not get the id: delete the person again, so both databases stay the same
            LOGGER.error("Could not add person {} to device-service, person not saved", person.getId(), e);
            personRepository.delete(person);
            throw new CustomException("Could not add the user to device-service",
                    HttpStatus.SERVICE_UNAVAILABLE, Person.class.getSimpleName(), List.of(e.getMessage()));
        }
        return person.getId();
    }

    public PersonDetailsDTO update(UUID id, PersonDetailsDTO personDTO) {
        Person person = getOrThrow(id);
        // the username is not changed here: it must stay the same as the login in auth-service
        person.setName(personDTO.getName());
        person.setAddress(personDTO.getAddress());
        person.setAge(personDTO.getAge());
        return PersonBuilder.toPersonDetailsDTO(personRepository.save(person));
    }

    /**
     * Removes the login (auth-service) and the user id from app_user (device-service, its devices become unassigned)
     * first, so a failure there leaves the person intact.
     */
    public void delete(UUID id) {
        Person person = getOrThrow(id);
        String userPath = EndpointPaths.USER_ID.replace("{userId}", id.toString());
        try {
            restTemplate.delete(authServiceUrl + EndpointPaths.CREDENTIALS + userPath);
            restTemplate.delete(deviceServiceUrl + EndpointPaths.DEVICE + userPath);
        } catch (RestClientException e) {
            LOGGER.error("Cleanup for person {} failed, person not deleted", id, e);
            throw new CustomException("Could not clean up linked credentials/devices",
                    HttpStatus.SERVICE_UNAVAILABLE, Person.class.getSimpleName(), List.of(e.getMessage()));
        }
        personRepository.delete(person);
    }

    private Person getOrThrow(UUID id) {
        return personRepository.findById(id).orElseThrow(() -> {
            LOGGER.error("Person with id {} was not found in db", id);
            return new ResourceNotFoundException(Person.class.getSimpleName() + " with id: " + id);
        });
    }
}
