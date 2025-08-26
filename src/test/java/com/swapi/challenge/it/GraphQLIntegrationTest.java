package com.swapi.challenge.it;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swapi.challenge.security.JwtTestUtils;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.http.HttpHeaders;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.HashMap;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GraphQLIntegrationTest {


    private WebTestClient unauth;
    private WebTestClient client;
    private String user;
    @LocalServerPort int port;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static WireMockServer wm = new WireMockServer(0); // random port
    static String jwtSecret = "this-is-a-long-test-secret-key-32b-min!"; // >=32 bytes

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r){
        wm.start();
        r.add("swapi.base-url", () -> "http://localhost:" + wm.port() + "/api");
        r.add("security.jwt.secret", () -> jwtSecret);
    }

    @BeforeEach
    void reset(){ wm.resetAll(); }

    @BeforeAll
    void buildClient() {
        unauth = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();

        user = "it_" + UUID.randomUUID().toString();
        String pwd = "it";

        // --- register ---
        Map<String, String> regBody = new HashMap<>();
        regBody.put("username", user);
        regBody.put("password", pwd);

        unauth.post().uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(regBody)   // ✅ sin Map.of
                .exchange();          // puede ser 2xx o 409, no aserto

        // --- login ---
        Map<String, String> loginBody = new HashMap<>();
        loginBody.put("username", user);
        loginBody.put("password", pwd);

        AtomicReference<String> jwt = new AtomicReference<>();
        unauth.post().uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(loginBody) // ✅ sin Map.of
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody()
                .jsonPath("$.token").value(t -> jwt.set(t.toString()));

        client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + jwt.get())
                .defaultHeader("X-Correlation-Id", "it-test")
                .build();
    }


    @AfterAll
    static void stop(){ wm.stop(); }

    private static String gqlBody(String query, Map<String,Object> vars) throws Exception {
        Map<String,Object> body = new HashMap<>();
        body.put("query", query);
        if (vars != null) body.put("variables", vars);
        return MAPPER.writeValueAsString(body);
    }

    @Test
    void people_por_nombre_y_paginado() throws Exception {
        wm.stubFor(get(urlPathEqualTo("/api/people"))
                .withQueryParam("page", equalTo("1"))
                .withQueryParam("limit", equalTo("10"))
                .withQueryParam("name", equalTo("sky"))
                .willReturn(okJson("{\"total_pages\":1,\"total_records\":1," +
                        "\"results\":[{\"uid\":\"1\",\"name\":\"Luke Skywalker\"}]}")));

        String q = "query($p:Int,$s:Int,$n:String){ people(page:$p,size:$s,name:$n){ totalPages totalRecords results{ id name } } }";
        Map<String,Object> vars = new HashMap<>();
        vars.put("p", 1); vars.put("s", 10); vars.put("n", "sky");

        client.post().uri("/graphql")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(gqlBody(q, vars))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.people.totalRecords").isEqualTo(1)
                .jsonPath("$.data.people.results[0].name").isEqualTo("Luke Skywalker");

        // Cache: segunda llamada
        client.post().uri("/graphql")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(gqlBody(q, vars))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.people.totalRecords").isEqualTo(1);

        wm.verify(1, getRequestedFor(urlPathEqualTo("/api/people"))
                .withQueryParam("name", equalTo("sky")));
    }

    @Test
    void films_por_title_hope() throws Exception {
        wm.stubFor(get(urlPathEqualTo("/api/films"))
                .withQueryParam("title", equalTo("hope"))
                .withQueryParam("page", equalTo("1"))
                .withQueryParam("limit", equalTo("10"))
                .willReturn(okJson("{\"results\":[{\"uid\":\"1\",\"properties\":{\"title\":\"A New Hope\",\"release_date\":\"1977-05-25\"}}]}")));

        String q = "query($p:Int,$s:Int,$n:String){ films(page:$p,size:$s,name:$n){ results{ id title releaseDate } } }";
        Map<String,Object> vars = new HashMap<>();
        vars.put("p", 1); vars.put("s", 10); vars.put("n", "hope");

        client.post().uri("/graphql")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(gqlBody(q, vars))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.films.results[0].title").isEqualTo("A New Hope")
                .jsonPath("$.data.films.results[0].releaseDate").isEqualTo("1977-05-25");
    }
}
