package com.swapi.challenge.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

@EnableCaching
public class CacheConfig {

    @Bean
    public CaffeineCacheManager cacheManager() {
        CaffeineCacheManager m = new CaffeineCacheManager();
        m.registerCustomCache("peopleList",
                Caffeine.newBuilder().maximumSize(1000).expireAfterWrite(5, TimeUnit.MINUTES).build());
        m.registerCustomCache("personDetail",
                Caffeine.newBuilder().maximumSize(2000).expireAfterWrite(30, TimeUnit.MINUTES).build());
        m.registerCustomCache("filmsList",
                Caffeine.newBuilder().maximumSize(1000).expireAfterWrite(5, TimeUnit.MINUTES).build());
        m.registerCustomCache("filmDetail",
                Caffeine.newBuilder().maximumSize(2000).expireAfterWrite(30, TimeUnit.MINUTES).build());
        m.registerCustomCache("starshipsList",
                Caffeine.newBuilder().maximumSize(1000).expireAfterWrite(5, TimeUnit.MINUTES).build());
        m.registerCustomCache("starshipDetail",
                Caffeine.newBuilder().maximumSize(2000).expireAfterWrite(30, TimeUnit.MINUTES).build());
        m.registerCustomCache("vehiclesList",
                Caffeine.newBuilder().maximumSize(1000).expireAfterWrite(5, TimeUnit.MINUTES).build());
        m.registerCustomCache("vehicleDetail",
                Caffeine.newBuilder().maximumSize(2000).expireAfterWrite(30, TimeUnit.MINUTES).build());
        return m;
    }
}