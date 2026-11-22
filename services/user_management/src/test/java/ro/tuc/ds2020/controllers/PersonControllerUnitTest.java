package ro.tuc.ds2020.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ro.tuc.ds2020.Ds2020TestConfig;
import ro.tuc.ds2020.dtos.PersonDetailsDTO;
import ro.tuc.ds2020.services.PersonService;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


public class PersonControllerUnitTest extends Ds2020TestConfig {
    private static final String PERSON_URL = "/person/" + UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PersonService service;

    @Test
    public void updatePersonTest() throws Exception {
        mockMvc.perform(put(PERSON_URL)
                .content(json(new PersonDetailsDTO("John", "Somewhere Else street", 22)))
                .contentType("application/json"))
                .andExpect(status().isOk());
    }

    @Test
    public void updatePersonTestFailsDueToAge() throws Exception {
        mockMvc.perform(put(PERSON_URL)
                .content(json(new PersonDetailsDTO("John", "Somewhere Else street", 17)))
                .contentType("application/json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void updatePersonTestFailsDueToNull() throws Exception {
        mockMvc.perform(put(PERSON_URL)
                .content(json(new PersonDetailsDTO("John", null, 22)))
                .contentType("application/json"))
                .andExpect(status().isBadRequest());
    }

    // users are created only through /auth/register + the USER_CREATED event
    @Test
    public void createPersonOverHttpIsNotAllowed() throws Exception {
        mockMvc.perform(post("/person")
                .content(json(new PersonDetailsDTO("John", "Somewhere Else street", 22)))
                .contentType("application/json"))
                .andExpect(status().isMethodNotAllowed());
    }

    private static String json(PersonDetailsDTO dto) throws Exception {
        return new ObjectMapper().writeValueAsString(dto);
    }
}
