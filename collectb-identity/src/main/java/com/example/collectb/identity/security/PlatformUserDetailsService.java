package com.example.collectb.identity.security;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.collectb.identity.entity.IdentityCredential;
import com.example.collectb.identity.entity.PlatformUser;
import com.example.collectb.identity.entity.Role;
import com.example.collectb.identity.entity.RoleAssignment;
import com.example.collectb.identity.model.CredentialStatus;
import com.example.collectb.identity.model.CredentialType;
import com.example.collectb.identity.repository.IdentityCredentialRepository;
import com.example.collectb.identity.repository.PermissionRepository;
import com.example.collectb.identity.repository.PlatformUserRepository;
import com.example.collectb.identity.repository.RoleAssignmentRepository;
import com.example.collectb.identity.repository.RoleRepository;

/**
 * Loads a {@link PlatformUserPrincipal} for email/password authentication (the Spring Security
 * {@code DaoAuthenticationProvider} entry point for the authorization server's login form), per
 * design doc section 2.2.1 "Support collector registration and login using email/password for
 * the MVP.".
 * <p>
 * Resolves, in order: the {@code platform_user} row by email, its active {@code PASSWORD}
 * {@link IdentityCredential} (email/password login only ever authenticates against a LOCAL
 * PASSWORD credential - OIDC/SAML/PASSKEY credentials are validated through their own,
 * not-yet-implemented flows), and the set of role names/permission keys granted via
 * {@code role_assignment} -> {@code role} -> {@code role_permission} -> {@code permission}.
 */
@Service
public class PlatformUserDetailsService implements UserDetailsService {

	private static final Logger log = LoggerFactory.getLogger(PlatformUserDetailsService.class);

	private static final String ACTIVE_ASSIGNMENT_STATUS = "ACTIVE";

	private final PlatformUserRepository platformUserRepository;
	private final IdentityCredentialRepository identityCredentialRepository;
	private final RoleAssignmentRepository roleAssignmentRepository;
	private final RoleRepository roleRepository;
	private final PermissionRepository permissionRepository;

	public PlatformUserDetailsService(PlatformUserRepository platformUserRepository,
			IdentityCredentialRepository identityCredentialRepository,
			RoleAssignmentRepository roleAssignmentRepository, RoleRepository roleRepository,
			PermissionRepository permissionRepository) {
		this.platformUserRepository = platformUserRepository;
		this.identityCredentialRepository = identityCredentialRepository;
		this.roleAssignmentRepository = roleAssignmentRepository;
		this.roleRepository = roleRepository;
		this.permissionRepository = permissionRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		PlatformUser user = platformUserRepository.findByEmail(email)
				.orElseThrow(() -> {
					log.warn("loadUserByUsername: no platform_user found for email={}", email);
					return new UsernameNotFoundException("No platform_user with email " + email);
				});

		IdentityCredential passwordCredential = identityCredentialRepository
				.findByUserIdAndCredentialType(user.getUserId(), CredentialType.PASSWORD)
				.stream()
				.filter(credential -> credential.getStatus() == CredentialStatus.ACTIVE)
				.findFirst()
				.orElseThrow(() -> {
					log.warn("loadUserByUsername: no ACTIVE PASSWORD credential for email={} userId={}", email,
							user.getUserId());
					return new UsernameNotFoundException("No active PASSWORD credential for email " + email);
				});

		List<RoleAssignment> assignments = roleAssignmentRepository
				.findByUserIdAndStatus(user.getUserId(), ACTIVE_ASSIGNMENT_STATUS);
		List<Role> roles = roleRepository.findByRoleIdIn(
				assignments.stream().map(RoleAssignment::getRoleId).distinct().toList());
		java.util.Map<java.util.UUID, String> roleNameById = roles.stream()
				.collect(Collectors.toMap(Role::getRoleId, Role::getRoleName));
		Set<String> roleNames = roles.stream().map(Role::getRoleName).collect(Collectors.toSet());
		Set<String> permissionKeys = Set.copyOf(permissionRepository.findPermissionKeysByRoleIdIn(
				roles.stream().map(Role::getRoleId).distinct().toList()));
		List<PlatformUserPrincipal.ScopeGrant> scopeGrants = assignments.stream()
				.map(assignment -> new PlatformUserPrincipal.ScopeGrant(
						roleNameById.get(assignment.getRoleId()), assignment.getScopeType(), assignment.getScopeId()))
				.toList();

		log.debug("loadUserByUsername: resolved email={} userId={} roles={} assignments={}", email, user.getUserId(),
				roleNames, assignments.size());

		return new PlatformUserPrincipal(user.getUserId(), user.getEmail(), passwordCredential.getPasswordHash(),
				user.getActorType(), user.getAccountStatus(), roleNames, permissionKeys, scopeGrants,
				passwordCredential.isMfaEnabled());
	}
}
