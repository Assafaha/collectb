package com.example.collectb.identity.config;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.example.collectb.identity.model.AccountStatus;
import com.example.collectb.identity.model.ActorType;
import com.example.collectb.identity.security.PlatformUserPrincipal;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Jackson mixin teaching the custom {@code JsonMapper} configured in
 * {@link AuthorizationServerConfig#authorizationService} how to (de)serialize
 * {@link PlatformUserPrincipal}, which the Spring Authorization Server persists as
 * part of the {@code principal_name}/{@code attributes} JSON payload in {@code oauth2_authorization}
 * whenever a login is completed through the hosted {@code /login} form.
 * <p>
 * {@link PlatformUserPrincipal} exposes no default constructor or setters (it is intentionally
 * immutable), so the JSON property names below must match its Jackson-visible getters, notably
 * the {@link org.springframework.security.core.userdetails.UserDetails}-overridden
 * {@code getUsername()}/{@code getPassword()} (serialized as {@code username}/{@code password},
 * not {@code email}/{@code passwordHash}). Any other bean-style getters (e.g.
 * {@code isAccountNonExpired()}) are serialized too but have no matching constructor parameter,
 * so {@link JsonIgnoreProperties} tells Jackson to ignore them on the way back in.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
abstract class PlatformUserPrincipalMixin {

	@JsonCreator
	PlatformUserPrincipalMixin(@JsonProperty("userId") UUID userId, @JsonProperty("username") String email,
			@JsonProperty("password") String passwordHash, @JsonProperty("actorType") ActorType actorType,
			@JsonProperty("accountStatus") AccountStatus accountStatus,
			@JsonProperty("roleNames") Set<String> roleNames, @JsonProperty("permissionKeys") Set<String> permissionKeys,
			@JsonProperty("scopeGrants") List<PlatformUserPrincipal.ScopeGrant> scopeGrants,
			@JsonProperty("mfaEnabled") boolean mfaEnabled) {
	}
}
