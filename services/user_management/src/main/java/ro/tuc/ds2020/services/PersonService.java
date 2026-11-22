package ro.tuc.ds2020.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import ro.tuc.common.EndpointPaths;
import ro.tuc.common.events.SyncEvent;
import ro.tuc.common.events.SyncEventType;
import ro.tuc.common.handlers.exceptions.model.CustomException;
import ro.tuc.common.handlers.exceptions.model.ResourceNotFoundException;
import ro.tuc.ds2020.dtos.PersonDTO;
import ro.tuc.ds2020.dtos.PersonDetailsDTO;
import ro.tuc.ds2020.dtos.builders.PersonBuilder;
import ro.tuc.ds2020.entities.Person;
import ro.tuc.ds2020.kafka.SyncEventPublisher;
import ro.tuc.ds2020.repositories.PersonRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PersonService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PersonService.class);
    private final PersonRepository personRepository;
    private final RestTemplate restTemplate;
    private final SyncEventPublisher syncEventPublisher;
    private final String authServiceUrl;

    @Autowired
    public PersonService(PersonRepository personRepository,
                         RestTemplate restTemplate,
                         SyncEventPublisher syncEventPublisher,
                         @Value("${services.auth.url}") String authServiceUrl) {
        this.personRepository = personRepository;
        this.restTemplate = restTemplate;
        this.syncEventPublisher = syncEventPublisher;
        this.authServiceUrl = authServiceUrl;
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

    /**
     * Saves the person, then publishes USER_CREATED on Kafka (device-service adds the id to its app_user table).
     * If Kafka is down, publish() throws and @Transactional rolls back the insert, so the user is not half-created.
     */
    @Transactional
    public UUID insert(PersonDetailsDTO personDTO) {
        Person person = personRepository.save(PersonBuilder.toEntity(personDTO));
        LOGGER.debug("Person with id {} was inserted in db", person.getId());
        syncEventPublisher.publish(new SyncEvent(SyncEventType.USER_CREATED, person.getId(), null));
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
     * Deletes the login in auth-service first (REST: we must know right away that the user cannot log in anymore),
     * then the person, then publishes USER_DELETED (device-service removes the id from app_user, so the user's
     * devices become unassigned). If a step fails, the person is kept and the API returns 503.
     */
    @Transactional
    public void delete(UUID id) {
        Person person = getOrThrow(id);
        String userPath = EndpointPaths.USER_ID.replace("{userId}", id.toString());
        try {
            restTemplate.delete(authServiceUrl + EndpointPaths.CREDENTIALS + userPath);
        } catch (RestClientException e) {
            LOGGER.error("Could not delete the login of person {}, person not deleted", id, e);
            throw new CustomException("Could not delete the login in auth-service",
                    HttpStatus.SERVICE_UNAVAILABLE, Person.class.getSimpleName(), List.of(e.getMessage()));
        }
        personRepository.delete(person);
        syncEventPublisher.publish(new SyncEvent(SyncEventType.USER_DELETED, id, null));
    }

    private Person getOrThrow(UUID id) {
        return personRepository.findById(id).orElseThrow(() -> {
            LOGGER.error("Person with id {} was not found in db", id);
            return new ResourceNotFoundException(Person.class.getSimpleName() + " with id: " + id);
        });
    }
}
