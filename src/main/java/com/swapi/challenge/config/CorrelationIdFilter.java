package com.swapi.challenge.config;

import org.jboss.logging.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {
    @Override protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String cid = Optional.ofNullable(req.getHeader("X-Correlation-Id"))
                .filter(s -> !s.isEmpty()).orElse(UUID.randomUUID().toString());
        MDC.put("cid", cid); res.setHeader("X-Correlation-Id", cid);
        try { chain.doFilter(req, res); } finally { MDC.remove("cid"); }
    }
}
