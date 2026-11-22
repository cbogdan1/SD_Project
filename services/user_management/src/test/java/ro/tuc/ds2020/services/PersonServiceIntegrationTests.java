package ro.tuc.ds2020.services;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.jdbc.Sql;
import ro.tuc.common.events.SyncEvent;
import ro.tuc.common.events.SyncEventType;
import ro.tuc.ds2020.Ds2020TestConfig;
import ro.tuc.ds2020.dtos.PersonDTO;
import ro.tuc.ds2020.dtos.PersonDetailsDTO;
import ro.tuc.ds2020.kafka.SyncEventPublisher;

import static org.springframework.test.util.AssertionErrors.assertEquals;

import java.util.List;
import java.util.UUID;

@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:/test-sql/create.sql")
@Sql(executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD, scripts = "classpath:/test-sql/delete.sql")
public class PersonServiceIntegrationTests extends Ds2020TestConfig {

    @Autowired
    PersonService personService;

    // Kafka does not run in tests: the mock makes publish() do nothing
    @MockBean
    SyncEventPublisher syncEventPublisher;

    @Test
    public void testGetCorrect() {
        List<PersonDTO> personDTOList = personService.findPersons();
        assertEquals("Test Insert Person", 1, personDTOList.size());
    }

    @Test
    public void testInsertKeepsTheEventIdWithGetById() {
        UUID id = UUID.randomUUID();
        personService.insert(userCreated(id));

        PersonDetailsDTO expected = new PersonDetailsDTO(id, "John", "Somewhere Else street", 22);
        PersonDetailsDTO fetched = personService.findPersonById(id);

        assertEquals("Test Inserted Person", expected, fetched);
        assertEquals("Test Inserted Username", "john", fetched.getUsername());
    }

    @Test
    public void testInsertSameEventTwiceKeepsOnePerson() {
        UUID id = UUID.randomUUID();
        personService.insert(userCreated(id));
        personService.insert(userCreated(id));

        List<PersonDTO> personDTOList = personService.findPersons();
        assertEquals("Test Inserted Persons", 2, personDTOList.size());
    }

    private static SyncEvent userCreated(UUID id) {
        SyncEvent event = new SyncEvent(SyncEventType.USER_CREATED, id, null);
        event.setUsername("john");
        event.setName("John");
        event.setAddress("Somewhere Else street");
        event.setAge(22);
        return event;
    }
}
