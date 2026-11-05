package com.example.collectb.identity;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.collectb.PostgresTestcontainerConfiguration;

/**
 * HTTP-level test for the MVP email/password registration and login endpoints
 * ({@code POST /api/v1/auth/register}, {@code POST /api/v1/auth/login}).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestcontainerConfiguration.class)
class AuthControllerIT {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void register_thenLogin_returnsAccessToken() throws Exception {
		String email = "http-flow-" + UUID.randomUUID() + "@example.com";
		String registerBody = """
				{"email": "%s", "displayName": "HTTP Flow User", "password": "Abc12345!"}
				""".formatted(email);

		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(registerBody))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.userId", notNullValue()))
				.andExpect(jsonPath("$.email").value(email))
				.andExpect(jsonPath("$.actorType").value("COLLECTOR"))
				.andExpect(jsonPath("$.accessToken", notNullValue()))
				.andExpect(jsonPath("$.tokenType").value("Bearer"));

		String loginBody = """
				{"email": "%s", "password": "Abc12345!"}
				""".formatted(email);

		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(loginBody))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(email))
				.andExpect(jsonPath("$.accessToken", notNullValue()));
	}

	@Test
	void login_withWrongPassword_returnsUnauthorized() throws Exception {
		String email = "http-flow-bad-" + UUID.randomUUID() + "@example.com";
		String registerBody = """
				{"email": "%s", "displayName": "HTTP Flow Bad", "password": "Abc12345!"}
				""".formatted(email);
		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(registerBody))
				.andExpect(status().isCreated());

		String loginBody = """
				{"email": "%s", "password": "wrong-password"}
				""".formatted(email);
		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(loginBody))
				.andExpect(status().isUnauthorized());
	}
}
