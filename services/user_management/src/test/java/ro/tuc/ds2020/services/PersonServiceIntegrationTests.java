package ro.tuc.ds2020.services;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.web.client.RestTemplate;
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

    // auth-service does not run in tests: the mock makes the call to it do nothing
    @MockBean
    RestTemplate restTemplate;

    // Kafka does not run in tests: the mock makes publish() do nothing
    @MockBean
    SyncEventPublisher syncEventPublisher;

    @Test
    public void testGetCorrect() {
        List<PersonDTO> personDTOList = personService.findPersons();
        assertEquals("Test Insert Person", 1, personDTOList.size());
    }

    @Test
    public void testInsertCorrectWithGetById() {
        PersonDetailsDTO p = new PersonDetailsDTO("John", "Somewhere Else street", 22);
        p.setUsername("john");
        UUID insertedID = personService.insert(p);

        PersonDetailsDTO insertedPerson = new PersonDetailsDTO(insertedID, p.getName(),p.getAddress(), p.getAge());
        PersonDetailsDTO fetchedPerson = personService.findPersonById(insertedID);

        assertEquals("Test Inserted Person", insertedPerson, fetchedPerson);
    }

    @Test
    public void testInsertCorrectWithGetAll() {
        PersonDetailsDTO p = new PersonDetailsDTO("John", "Somewhere Else street", 22);
        p.setUsername("john");
        personService.insert(p);

        List<PersonDTO> personDTOList = personService.findPersons();
        assertEquals("Test Inserted Persons", 2, personDTOList.size());
    }
}
