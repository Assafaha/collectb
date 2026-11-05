package com.example.collectb.identity.web;

/** Request body for {@code POST /api/v1/auth/login}. */
public record LoginRequest(String email, String password) {
}
