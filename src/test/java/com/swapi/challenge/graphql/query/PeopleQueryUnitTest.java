package com.swapi.challenge.graphql.query;

import com.swapi.challenge.swapi.service.SwapiService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.graphql.test.tester.GraphQlTester;

import java.util.*;

@GraphQlTest(PeopleQuery.class)
class PeopleQueryUnitTest {

    @Autowired GraphQlTester graphQlTester;
    @MockBean SwapiService swapi;

    @Test
    void people_paginado_por_nombre() {
        Map<String,Object> item = new HashMap<>();
        item.put("uid", "1");
        item.put("name", "Luke Skywalker");
        Map<String,Object> raw = new HashMap<>();
        raw.put("results", Collections.singletonList(item));
        raw.put("total_pages", 1);
        raw.put("total_records", 1);

        Mockito.when(swapi.listPeople(1, 10, "sky")).thenReturn(raw);

        String query = "query($p:Int,$s:Int,$n:String){ people(page:$p,size:$s,name:$n){ totalPages totalRecords results{ id name } } }";
        graphQlTester.document(query)
                .variable("p", 1).variable("s", 10).variable("n", "sky")
                .execute()
                .path("people.totalRecords").entity(Integer.class).isEqualTo(1)
                .path("people.results[0].id").entity(String.class).isEqualTo("1")
                .path("people.results[0].name").entity(String.class).isEqualTo("Luke Skywalker");
    }

    @Test
    void people_id_y_name_AND() {
        // detalle por id
        Map<String,Object> props = new HashMap<>();
        props.put("name", "Luke Skywalker");
        Map<String,Object> result = new HashMap<>();
        result.put("uid", "1");
        result.put("properties", props);
        Map<String,Object> rawDetail = new HashMap<>();
        rawDetail.put("result", result);

        Mockito.when(swapi.personById("1")).thenReturn(rawDetail);

        String query = "query($id:ID,$n:String){ people(id:$id,name:$n){ totalRecords results{ id name } } }";
        graphQlTester.document(query)
                .variable("id", "1").variable("n", "luke")
                .execute()
                .path("people.totalRecords").entity(Integer.class).isEqualTo(1)
                .path("people.results[0].id").entity(String.class).isEqualTo("1");
    }
}
