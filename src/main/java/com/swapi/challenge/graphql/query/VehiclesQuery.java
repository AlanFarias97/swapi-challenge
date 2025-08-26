package com.swapi.challenge.graphql.query;

import com.swapi.challenge.swapi.service.SwapiService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.*;
import java.util.stream.Collectors;

@Controller
public class VehiclesQuery {
    private final SwapiService svc;
    public VehiclesQuery(SwapiService svc){ this.svc = svc; }

    @QueryMapping
    public Map<String,Object> vehicles(@Argument String name, @Argument Integer page,
                                       @Argument Integer size, @Argument String id){

        if (id != null && !id.trim().isEmpty()) {
            Map<String,Object> d = svc.vehicleById(id);
            Map<String,Object> v = mapDetailToVehicle(d);
            boolean matches = name==null || name.trim().isEmpty() ||
                    containsIgnoreCase(String.valueOf(v.get("name")), name);
            return pageOf(matches && v!=null ? Collections.singletonList(v) : Collections.emptyList());
        }

        Map<String,Object> raw = svc.listVehicles(page, size, name);
        Object node = raw.get("results"); if (node == null) node = raw.get("result");
        @SuppressWarnings("unchecked")
        List<Map<String,Object>> rawItems = (node instanceof List)?(List<Map<String,Object>>)node:Collections.emptyList();

        List<Map<String,Object>> items = rawItems.stream().map(this::mapListItemToVehicle).collect(Collectors.toList());
        Integer tp = (Integer) raw.get("total_pages"); Integer tr = (Integer) raw.get("total_records");
        if (tp==null) tp = items.isEmpty()?0:1; if (tr==null) tr = items.size();
        Map<String,Object> out = new LinkedHashMap<>();
        out.put("totalPages", tp); out.put("totalRecords", tr); out.put("results", items);
        return out;
    }

    @QueryMapping
    public Map<String,Object> vehicle(@Argument String id){
        return mapDetailToVehicle(svc.vehicleById(id));
    }

    // helpers
    private Map<String,Object> pageOf(List<Map<String,Object>> items){
        Map<String,Object> out = new LinkedHashMap<>();
        out.put("totalPages", items.isEmpty()?0:1);
        out.put("totalRecords", items.size());
        out.put("results", items);
        return out;
    }
    private boolean containsIgnoreCase(String s, String q){
        if (s==null || q==null) return false;
        return s.toLowerCase(Locale.ROOT).contains(q.toLowerCase(Locale.ROOT).trim());
    }
    private Map<String,Object> mapListItemToVehicle(Map<String,Object> item){
        Map<String,Object> v = new LinkedHashMap<>();
        v.put("id", item.get("uid"));
        Object name = item.get("name");
        Object model = null;
        if (item.get("properties") instanceof Map){
            Map<?,?> p = (Map<?,?>) item.get("properties");
            if (name==null) name = p.get("name");
            model = p.get("model");
        }
        v.put("name", name); v.put("model", model!=null?String.valueOf(model):null);
        return v;
    }
    @SuppressWarnings("unchecked")
    private Map<String,Object> mapDetailToVehicle(Map<String,Object> detail){
        if (detail==null) return null;
        Object r = detail.get("result"); if (!(r instanceof Map)) return null;
        Map<String,Object> d = (Map<String,Object>) r;
        Map<String,Object> v = new LinkedHashMap<>();
        v.put("id", d.get("uid"));
        Object name = d.get("name"); Object model = null;
        if (d.get("properties") instanceof Map){
            Map<?,?> p = (Map<?,?>) d.get("properties");
            if (name==null) name = p.get("name");
            model = p.get("model");
        }
        v.put("name", name); v.put("model", model!=null?String.valueOf(model):null);
        return v;
    }
}
