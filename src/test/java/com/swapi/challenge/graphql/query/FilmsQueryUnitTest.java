package com.swapi.challenge.graphql.query;

import com.swapi.challenge.swapi.service.SwapiService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.graphql.test.tester.GraphQlTester;

import java.util.*;

@GraphQlTest(FilmsQuery.class)
class FilmsQueryUnitTest {

    @Autowired GraphQlTester graphQlTester;
    @MockBean SwapiService swapi;

    @Test
    void films_por_title_hope() {
        Map<String,Object> item = new HashMap<>();
        item.put("uid", "1");
        Map<String,Object> props = new HashMap<>();
        props.put("title", "A New Hope");
        props.put("release_date", "1977-05-25");
        item.put("properties", props);

        Map<String,Object> raw = new HashMap<>();
        raw.put("results", Collections.singletonList(item));

        Mockito.when(swapi.listFilms(1, 10, "hope")).thenReturn(raw);

        String query = "query($p:Int,$s:Int,$n:String){ films(page:$p,size:$s,name:$n){ results{ id title releaseDate } } }";
        graphQlTester.document(query)
                .variable("p", 1).variable("s", 10).variable("n", "hope")
                .execute()
                .path("films.results[0].title").entity(String.class).isEqualTo("A New Hope")
                .path("films.results[0].releaseDate").entity(String.class).isEqualTo("1977-05-25");
    }
}
