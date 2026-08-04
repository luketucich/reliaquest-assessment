package com.challenge.api.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.challenge.api.security.ApiKeyFilter;
import com.challenge.api.service.EmployeeService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "api.security.api-key=" + EmployeeControllerTest.API_KEY)
class EmployeeControllerTest {

    static final String API_KEY = "test-api-key";
    private static final String BASE_PATH = "/api/v1/employee";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmployeeService employeeService;

    private static MockHttpServletRequestBuilder authorized(MockHttpServletRequestBuilder request) {
        return request.header(ApiKeyFilter.HEADER, API_KEY);
    }

    private static String createRequestBody(String salary, String age, String email) {
        return """
                {
                  "firstName": "Ada",
                  "lastName": "Byron",
                  "salary": %s,
                  "age": %s,
                  "jobTitle": "Software Engineer",
                  "email": "%s"
                }"""
                .formatted(salary, age, email);
    }

    @Test
    void getAllEmployeesReturnsEveryEmployee() throws Exception {
        // Read the count rather than hardcoding it: the context is shared, so a test that creates an
        // employee would otherwise break this one depending on the order they run in.
        int employeeCount = employeeService.getAllEmployees().size();

        mockMvc.perform(authorized(get(BASE_PATH)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(employeeCount))
                .andExpect(jsonPath("$[0].uuid").isNotEmpty())
                .andExpect(jsonPath("$[0].fullName").isNotEmpty());
    }

    @Test
    void getEmployeeByUuidReturnsTheMatchingEmployee() throws Exception {
        UUID uuid = employeeService.getAllEmployees().get(0).getUuid();

        mockMvc.perform(authorized(get(BASE_PATH + "/" + uuid)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value(uuid.toString()))
                .andExpect(jsonPath("$.fullName").isNotEmpty());
    }

    @Test
    void getEmployeeByUuidReturnsNotFoundForAnUnknownUuid() throws Exception {
        mockMvc.perform(authorized(get(BASE_PATH + "/" + UUID.randomUUID()))).andExpect(status().isNotFound());
    }

    @Test
    void getEmployeeByUuidReturnsBadRequestForAMalformedUuid() throws Exception {
        mockMvc.perform(authorized(get(BASE_PATH + "/not-a-uuid"))).andExpect(status().isBadRequest());
    }

    @Test
    void createEmployeeReturnsCreatedWithAGeneratedUuid() throws Exception {
        mockMvc.perform(authorized(post(BASE_PATH))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestBody("95000", "30", "ada.byron@example.com")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uuid").isNotEmpty())
                .andExpect(jsonPath("$.fullName").value("Ada Byron"))
                .andExpect(jsonPath("$.contractTerminationDate").doesNotExist());
    }

    @Test
    void createEmployeeReturnsBadRequestListingEveryInvalidField() throws Exception {
        mockMvc.perform(authorized(post(BASE_PATH))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestBody("-5", "9", "not-an-email")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.salary").isNotEmpty())
                .andExpect(jsonPath("$.age").isNotEmpty())
                .andExpect(jsonPath("$.email").isNotEmpty());
    }

    @Test
    void requestWithoutAnApiKeyIsUnauthorized() throws Exception {
        mockMvc.perform(get(BASE_PATH)).andExpect(status().isUnauthorized());
    }

    @Test
    void requestWithTheWrongApiKeyIsUnauthorized() throws Exception {
        mockMvc.perform(get(BASE_PATH).header(ApiKeyFilter.HEADER, "wrong-key")).andExpect(status().isUnauthorized());
    }
}
