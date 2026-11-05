package com.example.collectb.brand;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.collectb.PostgresTestcontainerConfiguration;

/**
 * HTTP-level test for the Brand endpoints ({@code POST/GET /api/v1/brands}), exercising the
 * full stack: REST login (issuing a Bearer JWT with the seeded {@code admin@example.com}
 * PLATFORM_ADMIN account) -&gt; brand creation authorized purely from the token's
 * {@code PLATFORM_BRAND:MANAGE} permission claim -&gt; public GET listing/lookup.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestcontainerConfiguration.class)
class BrandControllerIT {

	@Autowired
	private MockMvc mockMvc;

	private String adminAccessToken() throws Exception {
		String loginBody = """
				{"email": "admin@example.com", "password": "abc123"}
				""";
		String response = mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(loginBody))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return response.split("\"accessToken\":\"")[1].split("\"")[0];
	}

	@Test
	void createBrand_asPlatformAdmin_thenListAndGet() throws Exception {
		String accessToken = adminAccessToken();
		String brandCode = "brand-" + UUID.randomUUID().toString().substring(0, 8);
		String createBody = """
				{"brandCode": "%s", "name": "%s", "description": "Test brand"}
				""".formatted(brandCode, brandCode + "-name");

		String createResponse = mockMvc.perform(post("/api/v1/brands")
						.header("Authorization", "Bearer " + accessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(createBody))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.brandId", notNullValue()))
				.andExpect(jsonPath("$.brandCode").value(brandCode))
				.andExpect(jsonPath("$.licenseStatus").value("DRAFT"))
				.andReturn().getResponse().getContentAsString();

		String brandId = createResponse.split("\"brandId\":\"")[1].split("\"")[0];

		mockMvc.perform(get("/api/v1/brands"))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/brands/" + brandId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.brandCode").value(brandCode));
	}

	@Test
	void createBrand_withoutPermission_isForbidden() throws Exception {
		String email = "collector-" + UUID.randomUUID() + "@example.com";
		String registerBody = """
				{"email": "%s", "displayName": "Collector", "password": "Abc12345!"}
				""".formatted(email);
		String registerResponse = mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(registerBody))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		String accessToken = registerResponse.split("\"accessToken\":\"")[1].split("\"")[0];

		String createBody = """
				{"brandCode": "forbidden-brand", "name": "Forbidden Brand"}
				""";

		mockMvc.perform(post("/api/v1/brands")
						.header("Authorization", "Bearer " + accessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(createBody))
				.andExpect(status().isForbidden());
	}

	@Test
	void getBrand_withUnknownId_returnsNotFound() throws Exception {
		mockMvc.perform(get("/api/v1/brands/" + UUID.randomUUID()))
				.andExpect(status().isNotFound());
	}
}
