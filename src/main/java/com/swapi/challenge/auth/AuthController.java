package com.swapi.challenge.auth;

import com.swapi.challenge.auth.dto.*;
import com.swapi.challenge.security.*;
import java.util.Arrays;
import java.util.Collections;
import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtService jwt;
    private final UserRepository users;
    private final PasswordEncoder encoder;

    public AuthController(AuthenticationManager am, JwtService jwt, UserRepository users, PasswordEncoder encoder) {
        this.authManager = am; this.jwt = jwt; this.users = users; this.encoder = encoder;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterReq req) {
        if (users.existsByUsername(req.getUsername())) {
            return ResponseEntity.status(409).body("username already exists");
        }
        UserEntity u = new UserEntity();
        u.setUsername(req.getUsername());
        u.setPassword(encoder.encode(req.getPassword()));
        u.setRoles("USER");
        users.save(u);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<TokenRes> login(@Valid @RequestBody LoginReq req) {
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword()));
        // Roles del usuario autenticado (opc: extraer de DB)
        String[] roles = auth.getAuthorities().stream().map(a -> a.getAuthority()).toArray(String[]::new);
        String token = jwt.generateToken(req.getUsername(), Arrays.asList(roles));
        long exp = jwt.getExpirationEpochMillis(token);
        return ResponseEntity.ok(new TokenRes(token, exp));
    }
}
