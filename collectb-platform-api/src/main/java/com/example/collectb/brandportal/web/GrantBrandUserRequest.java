package com.example.collectb.brandportal.web;

/** Request body for granting a brand-scoped role to a user. */
public record GrantBrandUserRequest(String email, String roleName) {
}
