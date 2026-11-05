package com.example.collectb.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import com.example.collectb.PostgresTestcontainerConfiguration;
import com.example.collectb.identity.entity.PlatformUser;
import com.example.collectb.identity.model.CredentialType;
import com.example.collectb.identity.repository.IdentityCredentialRepository;
import com.example.collectb.identity.repository.PlatformUserRepository;
import com.example.collectb.identity.security.PlatformUserPrincipal;
import com.example.collectb.identity.service.PlatformUserAuthenticationService;
import com.example.collectb.identity.service.PlatformUserRegistrationRequest;
import com.example.collectb.identity.service.PlatformUserRegistrationService;

@SpringBootTest
@Import(PostgresTestcontainerConfiguration.class)
class PlatformUserAuthenticationFlowIT {

	@Autowired
	private PlatformUserRegistrationService registrationService;

	@Autowired
	private PlatformUserAuthenticationService authenticationService;

	@Autowired
	private PlatformUserRepository platformUserRepository;

	@Autowired
	private IdentityCredentialRepository identityCredentialRepository;

	@Autowired
	private JwtDecoder jwtDecoder;

	@Test
	void registerCollector_authenticateAndIssueJwt() {
		String email = "flow-" + UUID.randomUUID() + "@example.com";
		String password = "Abc12345!";

		PlatformUser registeredUser = registrationService.registerCollector(
				new PlatformUserRegistrationRequest(email, "Flow Test Collector", password));

		assertThat(platformUserRepository.findByEmail(email)).isPresent();
		assertThat(identityCredentialRepository.findByUserIdAndCredentialType(registeredUser.getUserId(), CredentialType.PASSWORD))
				.isNotEmpty();

		PlatformUserPrincipal principal = authenticationService.authenticate(email, password);
		String jwt = authenticationService.issueJwt(principal);

		assertThat(jwt).contains(".");
		assertThat(jwtDecoder.decode(jwt).getSubject()).isEqualTo(registeredUser.getUserId().toString());
		assertThat(jwtDecoder.decode(jwt).getClaimAsString("user_id")).isEqualTo(registeredUser.getUserId().toString());
		assertThat(jwtDecoder.decode(jwt).getClaimAsString("actor_type")).isEqualTo("COLLECTOR");
	}
}
