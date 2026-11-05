package com.example.collectb.users.web;

import java.util.UUID;

/** Request body for granting a brand-scoped role to a user from the aggregated "Users" screen,
 *  which (unlike the per-brand grant form) must also specify which brand since a caller here may
 *  be able to grant on more than one. */
public record GrantUserRequest(UUID brandId, String email, String roleName) {
}
