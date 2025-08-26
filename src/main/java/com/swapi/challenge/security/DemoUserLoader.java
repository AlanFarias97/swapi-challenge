package com.swapi.challenge.security;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DemoUserLoader {
    @Bean
    ApplicationRunner seedUsers(UserRepository repo, PasswordEncoder encoder) {
        return args -> {
            if (!repo.existsByUsername("demo")) {
                UserEntity u = new UserEntity();
                u.setUsername("demo");
                u.setPassword(encoder.encode("demo"));
                u.setRoles("USER");
                repo.save(u);
            }
        };
    }
}
