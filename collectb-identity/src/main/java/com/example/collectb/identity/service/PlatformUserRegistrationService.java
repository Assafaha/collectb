package com.example.collectb.identity.service;

import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.collectb.identity.entity.IdentityCredential;
import com.example.collectb.identity.entity.PlatformUser;
import com.example.collectb.identity.entity.Role;
import com.example.collectb.identity.entity.RoleAssignment;
import com.example.collectb.identity.model.AccountStatus;
import com.example.collectb.identity.model.ActorType;
import com.example.collectb.identity.model.CredentialProvider;
import com.example.collectb.identity.model.CredentialStatus;
import com.example.collectb.identity.model.CredentialType;
import com.example.collectb.identity.repository.IdentityCredentialRepository;
import com.example.collectb.identity.repository.PlatformUserRepository;
import com.example.collectb.identity.repository.RoleAssignmentRepository;
import com.example.collectb.identity.repository.RoleRepository;

@Service
public class PlatformUserRegistrationService {

	private final PlatformUserRepository platformUserRepository;
	private final IdentityCredentialRepository identityCredentialRepository;
	private final RoleRepository roleRepository;
	private final RoleAssignmentRepository roleAssignmentRepository;
	private final PasswordEncoder passwordEncoder;

	public PlatformUserRegistrationService(PlatformUserRepository platformUserRepository,
			IdentityCredentialRepository identityCredentialRepository, RoleRepository roleRepository,
			RoleAssignmentRepository roleAssignmentRepository, PasswordEncoder passwordEncoder) {
		this.platformUserRepository = platformUserRepository;
		this.identityCredentialRepository = identityCredentialRepository;
		this.roleRepository = roleRepository;
		this.roleAssignmentRepository = roleAssignmentRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public PlatformUser registerCollector(PlatformUserRegistrationRequest request) {
		String email = normalizeEmail(request.email());
		if (request.displayName() == null || request.displayName().isBlank()) {
			throw new IllegalArgumentException("displayName is required");
		}
		if (request.password() == null || request.password().isBlank()) {
			throw new IllegalArgumentException("password is required");
		}
		if (platformUserRepository.findByEmail(email).isPresent()) {
			throw new IllegalStateException("A user with email " + email + " already exists");
		}

		PlatformUser user = new PlatformUser(email, request.displayName(), ActorType.COLLECTOR, AccountStatus.ACTIVE);
		PlatformUser savedUser = platformUserRepository.save(user);

		IdentityCredential credential = new IdentityCredential(savedUser.getUserId(), CredentialProvider.LOCAL,
				CredentialType.PASSWORD, CredentialStatus.ACTIVE);
		credential.setPasswordHash(passwordEncoder.encode(request.password()));
		credential.setPasswordAlgorithm("argon2id");
		credential.setMfaEnabled(false);
		identityCredentialRepository.save(credential);

		Role collectorRole = roleRepository.findByRoleName("COLLECTOR")
				.orElseThrow(() -> new IllegalStateException("COLLECTOR role is missing from the identity seed data"));
		RoleAssignment assignment = new RoleAssignment(savedUser.getUserId(), collectorRole.getRoleId(),
				"COLLECTOR", null, "ACTIVE", savedUser.getUserId());
		roleAssignmentRepository.save(assignment);

		return savedUser;
	}

	private static String normalizeEmail(String email) {
		if (email == null || email.isBlank()) {
			throw new IllegalArgumentException("email is required");
		}
		String normalized = email.trim();
		if (!normalized.contains("@")) {
			throw new IllegalArgumentException("email must be valid");
		}
		return normalized.toLowerCase(Locale.ROOT);
	}
}
