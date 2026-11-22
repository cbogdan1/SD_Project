package ro.tuc.ds2020.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.tuc.common.events.SyncEvent;
import ro.tuc.common.events.SyncEventType;
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
    private final SyncEventPublisher syncEventPublisher;

    @Autowired
    public PersonService(PersonRepository personRepository, SyncEventPublisher syncEventPublisher) {
        this.personRepository = personRepository;
        this.syncEventPublisher = syncEventPublisher;
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
     * USER_CREATED event from auth-service: saves the person with the same id as the login.
     * The profile was already validated by auth-service. Kafka can deliver an event twice, and auth-service
     * re-sends the admin's event on every start, so a person that already exists is skipped.
     * Nothing is published here: the event that created the person is already on the topic.
     */
    public void insert(SyncEvent event) {
        if (personRepository.existsById(event.getId())) {
            LOGGER.info("Person {} already exists, USER_CREATED skipped", event.getId());
            return;
        }
        personRepository.save(new Person(event.getId(), event.getUsername(),
                event.getName(), event.getAddress(), event.getAge()));
        LOGGER.info("Person {} ({}) was inserted in db", event.getId(), event.getUsername());
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
     * Deletes the person, then publishes USER_DELETED: auth-service deletes the login, device-service removes the id
     * from app_user (the user's devices become unassigned) and monitoring-service forgets the owner.
     * If Kafka is down, publish() throws and @Transactional rolls back the delete, so the user is kept (503).
     */
    @Transactional
    public void delete(UUID id) {
        personRepository.delete(getOrThrow(id));
        syncEventPublisher.publish(new SyncEvent(SyncEventType.USER_DELETED, id, null));
    }

    private Person getOrThrow(UUID id) {
        return personRepository.findById(id).orElseThrow(() -> {
            LOGGER.error("Person with id {} was not found in db", id);
            return new ResourceNotFoundException(Person.class.getSimpleName() + " with id: " + id);
        });
    }
}
