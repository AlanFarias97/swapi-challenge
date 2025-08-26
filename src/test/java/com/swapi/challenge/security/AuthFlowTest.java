package com.swapi.challenge.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowTest {

    @Autowired MockMvc mvc;

    @Test
    void register_then_login_returns_jwt() throws Exception {
        String u = "demo_" + java.util.UUID.randomUUID(); // 👈 único

        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\""+u+"\",\"password\":\"demo\"}"))
                .andExpect(status().is2xxSuccessful());

        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\""+u+"\",\"password\":\"demo\"}"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.expiresAtEpochMillis").exists());
    }
}
