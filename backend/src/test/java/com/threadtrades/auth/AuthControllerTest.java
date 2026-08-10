package com.threadtrades.auth;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.threadtrades.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void registerThenLoginThenFetchOwnProfile() throws Exception {
        String registerJson =
                """
                {"email":"alice@example.com","password":"hunter2pass","username":"alice","name":"Alice Example"}
                """;

        String registerResponseJson = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", not(blankOrNullString())))
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.name").value("Alice Example"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = JsonPath.read(registerResponseJson, "$.token");

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.name").value("Alice Example"));

        String loginJson =
                """
                {"email":"alice@example.com","password":"hunter2pass"}
                """;
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(blankOrNullString())))
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    void registerRejectsDuplicateEmail() throws Exception {
        String registerJson =
                """
                {"email":"bob@example.com","password":"hunter2pass","username":"bob","name":"Bob Example"}
                """;
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(registerJson))
                .andExpect(status().isCreated());

        String duplicateEmailJson =
                """
                {"email":"bob@example.com","password":"anotherpass1","username":"bob2","name":"Bob Duplicate"}
                """;
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(duplicateEmailJson))
                .andExpect(status().isConflict());
    }

    @Test
    void loginRejectsWrongPassword() throws Exception {
        String registerJson =
                """
                {"email":"carol@example.com","password":"correctpass","username":"carol","name":"Carol Example"}
                """;
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(registerJson))
                .andExpect(status().isCreated());

        String wrongPasswordJson =
                """
                {"email":"carol@example.com","password":"wrongpass1"}
                """;
        mockMvc.perform(
                        post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(wrongPasswordJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meRejectsRequestsWithoutToken() throws Exception {
        mockMvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
    }
}
