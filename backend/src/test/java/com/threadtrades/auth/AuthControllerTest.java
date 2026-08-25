package com.threadtrades.auth;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import com.threadtrades.TestcontainersConfiguration;
import com.threadtrades.security.SessionCookieService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

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

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isCreated())
                .andExpect(cookie().exists(SessionCookieService.COOKIE_NAME))
                .andExpect(cookie().httpOnly(SessionCookieService.COOKIE_NAME, true))
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.name").value("Alice Example"))
                .andReturn();

        Cookie sessionCookie = registerResult.getResponse().getCookie(SessionCookieService.COOKIE_NAME);

        mockMvc.perform(get("/api/users/me").cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.name").value("Alice Example"));

        String loginJson =
                """
                {"email":"alice@example.com","password":"hunter2pass"}
                """;
        mockMvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(loginJson))
                .andExpect(status().isOk())
                .andExpect(cookie().value(SessionCookieService.COOKIE_NAME, not(blankOrNullString())))
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    void registerRejectsDuplicateEmail() throws Exception {
        String registerJson =
                """
                {"email":"bob@example.com","password":"hunter2pass","username":"bob","name":"Bob Example"}
                """;
        mockMvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(registerJson))
                .andExpect(status().isCreated());

        String duplicateEmailJson =
                """
                {"email":"bob@example.com","password":"anotherpass1","username":"bob2","name":"Bob Duplicate"}
                """;
        mockMvc.perform(post("/api/auth/register").with(csrf())
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
        mockMvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(registerJson))
                .andExpect(status().isCreated());

        String wrongPasswordJson =
                """
                {"email":"carol@example.com","password":"wrongpass1"}
                """;
        mockMvc.perform(
                        post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(wrongPasswordJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meRejectsRequestsWithoutToken() throws Exception {
        mockMvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutClearsSessionCookieAndSubsequentRequestsAreUnauthorized() throws Exception {
        String registerJson =
                """
                {"email":"dana@example.com","password":"hunter2pass","username":"dana","name":"Dana Example"}
                """;
        MvcResult registerResult = mockMvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isCreated())
                .andReturn();
        Cookie sessionCookie = registerResult.getResponse().getCookie(SessionCookieService.COOKIE_NAME);

        MvcResult logoutResult = mockMvc.perform(post("/api/auth/logout").with(csrf()).cookie(sessionCookie))
                .andExpect(status().isNoContent())
                .andReturn();
        Cookie clearedCookie = logoutResult.getResponse().getCookie(SessionCookieService.COOKIE_NAME);

        mockMvc.perform(get("/api/users/me").cookie(clearedCookie)).andExpect(status().isUnauthorized());
    }
}
