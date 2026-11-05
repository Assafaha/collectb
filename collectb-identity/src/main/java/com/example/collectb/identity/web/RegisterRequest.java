package com.example.collectb.identity.web;

/** Request body for {@code POST /api/v1/auth/register}. */
public record RegisterRequest(String email, String displayName, String password) {
}
