package com.example.collectb.identity.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Password hashing configuration. Uses argon2id as the default algorithm for encoding new
 * passwords, wrapped in Spring Security's {@link DelegatingPasswordEncoder} so stored hashes
 * self-describe their algorithm via an {@code {id}} prefix (e.g. {@code {argon2}$argon2id$...}).
 * <p>
 * The argon2 parameters here (saltLength=16 bytes, hashLength=32 bytes, parallelism=2,
 * memory=65536 KiB, iterations=3) match the values used when the seed data in
 * {@code db/changelog/changes/insert.sql} was generated, so newly encoded passwords use the
 * same cost profile as the seeded ones. Verifying a stored hash does not depend on these
 * parameters matching, since Argon2's PHC-formatted hash string embeds its own parameters.
 */
@Configuration
public class PasswordEncoderConfig {

	private static final String ARGON2_ENCODING_ID = "argon2";

	@Bean
	public PasswordEncoder passwordEncoder() {
		Argon2PasswordEncoder argon2 = new Argon2PasswordEncoder(16, 32, 2, 65536, 3);
		Map<String, PasswordEncoder> encoders = new HashMap<>();
		encoders.put(ARGON2_ENCODING_ID, argon2);
		encoders.put("bcrypt", new BCryptPasswordEncoder());

		DelegatingPasswordEncoder delegating = new DelegatingPasswordEncoder(ARGON2_ENCODING_ID, encoders);
		delegating.setDefaultPasswordEncoderForMatches(argon2);
		return delegating;
	}
}
