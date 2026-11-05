package com.example.collectb.identity.config;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Maps a JWT's {@code roles}/{@code permissions} claims (populated by
 * {@link JwtClaimsCustomizerConfig} for OAuth2-issued tokens and directly by
 * {@code PlatformUserAuthenticationService#issueJwt} for the direct REST login endpoint) to
 * {@link GrantedAuthority} instances, mirroring
 * {@link com.example.collectb.identity.security.PlatformUserPrincipal#getAuthorities()}: role
 * names are prefixed {@code ROLE_} (for {@code hasRole(...)}/{@code @PreAuthorize} checks),
 * permission keys are used as-is (for {@code hasAuthority(...)} checks).
 * <p>
 * This is what lets resource-server (Bearer token) requests be authorized identically to the
 * session-based hosted login flow, without re-querying the database on every request - all the
 * authorization data a request needs already round-trips inside the token itself.
 */
public class JwtAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

	@Override
	public Collection<GrantedAuthority> convert(Jwt jwt) {
		Stream<String> roleAuthorities = claimAsStrings(jwt, "roles").map(role -> "ROLE_" + role);
		Stream<String> permissionAuthorities = claimAsStrings(jwt, "permissions");
		return Stream.concat(roleAuthorities, permissionAuthorities)
				.map(SimpleGrantedAuthority::new)
				.map(GrantedAuthority.class::cast)
				.toList();
	}

	private static Stream<String> claimAsStrings(Jwt jwt, String claimName) {
		List<String> values = jwt.getClaimAsStringList(claimName);
		return values == null ? Stream.empty() : values.stream();
	}
}
