package com.swapi.challenge.graphql.query;

import com.swapi.challenge.swapi.service.SwapiService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.client.HttpClientErrorException;

import java.util.*;
import java.util.stream.Collectors;

@Controller
public class PeopleQuery {
    private final SwapiService svc;
    public PeopleQuery(SwapiService svc) { this.svc = svc; }

    @QueryMapping
    public Map<String,Object> people(@Argument String name,
                                     @Argument Integer page,
                                     @Argument Integer size,
                                     @Argument String id) {
        // --- Caso con ID: AND con name ---
        if (id != null && !id.trim().isEmpty()) {
            Map<String,Object> out = new LinkedHashMap<>();
            try {
                Map<String,Object> rawDetail = svc.personById(id);     // GET /people/{id}
                Map<String,Object> person = mapDetailToPerson(rawDetail); // { id, name }

                boolean matches = true;
                if (name != null && !name.trim().isEmpty()) {
                    matches = containsIgnoreCase(String.valueOf(person.get("name")), name);
                }

                if (person != null && matches) {
                    out.put("totalPages", 1);
                    out.put("totalRecords", 1);
                    out.put("results", Collections.singletonList(person));
                } else {
                    out.put("totalPages", 0);
                    out.put("totalRecords", 0);
                    out.put("results", Collections.emptyList());
                }
                return out;
            } catch (Exception e) {
                Map<String,Object> empty = new LinkedHashMap<>();
                empty.put("totalPages", 0);
                empty.put("totalRecords", 0);
                empty.put("results", Collections.emptyList());
                return empty;
            }
        }

        // --- Sin ID: listado paginado + filtro por name ---
        Map<String,Object> raw = svc.listPeople(page, size, name); // /people?page=&limit=&name=
        Integer totalPages   = (Integer) raw.get("total_pages");
        Integer totalRecords = (Integer) raw.get("total_records");

        Object node = raw.get("results");
        if (node == null) node = raw.get("result"); // SWAPI a veces usa "result" cuando hay name

        @SuppressWarnings("unchecked")
        List<Map<String,Object>> rawItems =
                (node instanceof List) ? (List<Map<String,Object>>) node : Collections.emptyList();

        List<Map<String,Object>> items = rawItems.stream()
                .map(this::mapListItemToPerson) // { id, name }
                .collect(Collectors.toList());

        if (totalPages == null)   totalPages   = items.isEmpty() ? 0 : 1;
        if (totalRecords == null) totalRecords = items.size();

        Map<String,Object> out = new LinkedHashMap<>();
        out.put("totalPages", totalPages);
        out.put("totalRecords", totalRecords);
        out.put("results", items);
        return out;
    }

    // helpers
    private boolean containsIgnoreCase(String s, String q) {
        if (s == null || q == null) return false;
        return s.toLowerCase(Locale.ROOT).contains(q.toLowerCase(Locale.ROOT).trim());
    }

    private Map<String,Object> mapListItemToPerson(Map<String, Object> item) {
        Map<String,Object> p = new LinkedHashMap<>();
        p.put("id", item.get("uid"));
        Object nm = item.get("name");
        if (nm == null && item.get("properties") instanceof Map) {
            nm = ((Map<?,?>) item.get("properties")).get("name");
        }
        p.put("name", nm);
        return p;
    }

    @SuppressWarnings("unchecked")
    private Map<String,Object> mapDetailToPerson(Map<String,Object> detail) {
        if (detail == null) return null;
        Object resultObj = detail.get("result");
        if (!(resultObj instanceof Map)) return null;

        Map<String,Object> result = (Map<String,Object>) resultObj;
        Map<String,Object> p = new LinkedHashMap<>();
        p.put("id", result.get("uid"));
        Object nm = result.get("name");
        if (nm == null && result.get("properties") instanceof Map) {
            nm = ((Map<?,?>) result.get("properties")).get("name");
        }
        p.put("name", nm);
        return p;
    }

}
