package com.swapi.challenge.swapi.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.springframework.test.web.client.MockRestServiceServer;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SwapiServiceUnitTest {

    private SwapiService svc;
    private RestTemplate rt;
    private MockRestServiceServer server;

    @BeforeEach
    void setup() {
        rt = new RestTemplate();
        server = MockRestServiceServer.bindTo(rt).ignoreExpectOrder(true).build();
        svc = new SwapiService(rt);
        // inyectar base-url via reflexión si tu campo es package-private:
        try {
            java.lang.reflect.Field f = SwapiService.class.getDeclaredField("base");
            f.setAccessible(true);
            f.set(svc, "https://www.swapi.tech/api");
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Test
    void listFilms_usa_title_como_param() {
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/films")))
                .andExpect(queryParam("title", "hope"))
                .andExpect(queryParam("page", "1"))
                .andExpect(queryParam("limit", "10"))
                .andRespond(withSuccess("{\"results\":[{\"uid\":\"1\"}]}", MediaType.APPLICATION_JSON));

        Map<String,Object> resp = svc.listFilms(1, 10, "hope");
        assertThat(resp).containsKey("results");
        server.verify();
    }
}
