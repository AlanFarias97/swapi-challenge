package com.swapi.challenge.auth.dto;

public class TokenRes {
    private String token;
    private long expiresAtEpochMillis;

    public TokenRes(String token, long expiresAtEpochMillis) {
        this.token = token; this.expiresAtEpochMillis = expiresAtEpochMillis;
    }
    public String getToken() { return token; }
    public long getExpiresAtEpochMillis() { return expiresAtEpochMillis; }
}
