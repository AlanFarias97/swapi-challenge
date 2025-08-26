/**
 * Capa de integración con SWAPI:
 * - Envia requests con paginado y filtros
 * - Aplica CircuitBreaker/Retry (resilience4j)
 * - Expone métodos cacheados (Caffeine)
 */
package com.swapi.challenge.swapi.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
/**
 * Cliente de SWAPI con cache y resiliencia.
 * <p>Films usa "title" como filtro; el resto usa "name".</p>
 */
@Service
public class SwapiService {

    @Value("${swapi.base-url}")
    String base;

    private final RestTemplate http;
    public SwapiService(RestTemplate http){ this.http = http; }

    // ---------- PEOPLE ----------
    @Cacheable(cacheNames="peopleList", key="T(String).format('p:%s:%s:%s', #page,#size,#name)")
    @CircuitBreaker(name="swapi", fallbackMethod="fallback")
    @Retry(name="swapi")
    public Map<String,Object> listPeople(Integer page, Integer size, String name){
        return list(resource("people"), page, size, name, "name");
    }

    @Cacheable(cacheNames="personDetail", key="#id")
    @CircuitBreaker(name="swapi", fallbackMethod="fallbackId")
    @Retry(name="swapi")
    public Map<String,Object> personById(String id){
        return byId(resource("people"), id);
    }

    // ---------- FILMS (usa title como filtro) ----------
    @Cacheable(cacheNames="filmsList", key="T(String).format('f:%s:%s:%s', #page,#size,#name)")
    @CircuitBreaker(name="swapi", fallbackMethod="fallback")
    @Retry(name="swapi")
    public Map<String,Object> listFilms(Integer page, Integer size, String name){
        return list(resource("films"), page, size, name, "title");
    }

    @Cacheable(cacheNames="filmDetail", key="#id")
    @CircuitBreaker(name="swapi", fallbackMethod="fallbackId")
    @Retry(name="swapi")
    public Map<String,Object> filmById(String id){
        return byId(resource("films"), id);
    }

    // ---------- STARSHIPS ----------
    @Cacheable(cacheNames="starshipsList", key="T(String).format('s:%s:%s:%s', #page,#size,#name)")
    @CircuitBreaker(name="swapi", fallbackMethod="fallback")
    @Retry(name="swapi")
    public Map<String,Object> listStarships(Integer page, Integer size, String name){
        return list(resource("starships"), page, size, name, "name");
    }

    @Cacheable(cacheNames="starshipDetail", key="#id")
    @CircuitBreaker(name="swapi", fallbackMethod="fallbackId")
    @Retry(name="swapi")
    public Map<String,Object> starshipById(String id){
        return byId(resource("starships"), id);
    }

    // ---------- VEHICLES ----------
    @Cacheable(cacheNames="vehiclesList", key="T(String).format('v:%s:%s:%s', #page,#size,#name)")
    @CircuitBreaker(name="swapi", fallbackMethod="fallback")
    @Retry(name="swapi")
    public Map<String,Object> listVehicles(Integer page, Integer size, String name){
        return list(resource("vehicles"), page, size, name, "name");
    }

    @Cacheable(cacheNames="vehicleDetail", key="#id")
    @CircuitBreaker(name="swapi", fallbackMethod="fallbackId")
    @Retry(name="swapi")
    public Map<String,Object> vehicleById(String id){
        return byId(resource("vehicles"), id);
    }

    // ---------- Helpers HTTP ----------
    private String resource(String r){ return base + "/" + r; }

    private Map<String,Object> list(String resourceBase, Integer page, Integer size,
                                    String filter, String filterKey){
        UriComponentsBuilder b = UriComponentsBuilder.fromHttpUrl(resourceBase)
                .queryParam("page", page == null ? 1 : page)
                .queryParam("limit", size == null ? 10 : size);
        if (filter != null && !filter.trim().isEmpty()) {
            b.queryParam(filterKey, filter);
        }
        return http.getForObject(b.toUriString(), Map.class);
    }

    private Map<String,Object> byId(String resourceBase, String id){
        return http.getForObject(resourceBase + "/" + id, Map.class);
    }

    // ---------- Fallbacks ----------
    private Map<String,Object> fallback(Integer page, Integer size, String name, Throwable t){
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "SWAPI no disponible");
    }
    private Map<String,Object> fallbackId(String id, Throwable t){
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "SWAPI no disponible");
    }
}
