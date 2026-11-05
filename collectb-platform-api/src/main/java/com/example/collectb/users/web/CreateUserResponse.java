package com.example.collectb.users.web;

import java.util.UUID;

/** Response for {@code POST /api/v1/users/accounts}: the new user's id plus a one-time
 *  temporary password (never retrievable again) for the admin to hand to the new user. */
public record CreateUserResponse(UUID userId, String email, String displayName, String actorType,
		String tempPassword) {
}
