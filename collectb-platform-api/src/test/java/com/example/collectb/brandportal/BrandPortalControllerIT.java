package com.example.collectb.brandportal;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
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
 * HTTP-level test for the brand portal's JSON REST API ({@code /api/v1/me/brands},
 * {@code /api/v1/brands/{brandId}/users}), exercising brand-scoped self-service authorization:
 * a {@code BRAND_ADMIN} for one specific brand can manage that brand's users without holding
 * the platform-wide {@code PLATFORM_IDENTITY:MANAGE} permission, and cannot manage a brand they
 * were never granted access to.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestcontainerConfiguration.class)
class BrandPortalControllerIT {

	@Autowired
	private MockMvc mockMvc;

	private String login(String email, String password) throws Exception {
		String loginBody = """
				{"email": "%s", "password": "%s"}
				""".formatted(email, password);
		String response = mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(loginBody))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return response.split("\"accessToken\":\"")[1].split("\"")[0];
	}

	private String register(String email, String password) throws Exception {
		String registerBody = """
				{"email": "%s", "displayName": "Portal Test User", "password": "%s"}
				""".formatted(email, password);
		String response = mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(registerBody))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return response.split("\"accessToken\":\"")[1].split("\"")[0];
	}

	private String createBrandAsAdmin(String adminToken) throws Exception {
		String brandCode = "brand-" + UUID.randomUUID().toString().substring(0, 8);
		String createBody = """
				{"brandCode": "%s", "name": "%s", "description": "Portal test brand"}
				""".formatted(brandCode, brandCode + "-name");
		String createResponse = mockMvc.perform(post("/api/v1/brands")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(createBody))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return createResponse.split("\"brandId\":\"")[1].split("\"")[0];
	}

	private String userId(String token) throws Exception {
		String response = mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return response.split("\"userId\":\"")[1].split("\"")[0];
	}

	@Test
	void userDetailsAllowSelfAndManagedBrandsWithoutExposingOtherScopes() throws Exception {
		String adminToken = login("admin@example.com", "abc123");
		String satoshiToken = login("Satoshi@example.com", "abc123");
		mockMvc.perform(get("/api/v1/users/" + userId(satoshiToken))
						.header("Authorization", "Bearer " + satoshiToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("Satoshi@example.com"))
				.andExpect(jsonPath("$.roleAssignments[*].roleName", hasItem("BRAND_ADMIN")));
		mockMvc.perform(get("/api/v1/users/roles").header("Authorization", "Bearer " + satoshiToken))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/v1/users/roles").header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk());

		String brandId = createBrandAsAdmin(adminToken);
		String otherBrandId = createBrandAsAdmin(adminToken);
		String managerEmail = "detail-manager-" + UUID.randomUUID() + "@example.com";
		register(managerEmail, "Abc12345!");
		mockMvc.perform(post("/api/v1/brands/" + brandId + "/users")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","roleName":"BRAND_ADMIN"}
								""".formatted(managerEmail)))
				.andExpect(status().isCreated());
		String managerToken = login(managerEmail, "Abc12345!");
		String targetEmail = "detail-target-" + UUID.randomUUID() + "@example.com";
		String targetToken = register(targetEmail, "Abc12345!");
		String targetId = userId(targetToken);
		for (String id : java.util.List.of(brandId, otherBrandId)) {
			mockMvc.perform(post("/api/v1/brands/" + id + "/users")
							.header("Authorization", "Bearer " + adminToken)
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{"email":"%s","roleName":"%s"}
									""".formatted(targetEmail, id.equals(brandId) ? "BRAND_VIEWER" : "BRAND_ADMIN")))
					.andExpect(status().isCreated());
		}
		mockMvc.perform(get("/api/v1/users/" + targetId).header("Authorization", "Bearer " + managerToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.roleAssignments", hasSize(1)))
				.andExpect(jsonPath("$.roleAssignments[0].scopeId").value(brandId))
				.andExpect(jsonPath("$.permissions", hasItem("COLLECTION:READ")))
				.andExpect(jsonPath("$.permissions", not(hasItem("COLLECTION:APPROVE"))))
				.andExpect(jsonPath("$.permissions", not(hasItem("COLLECTOR_PROFILE:READ"))));
		mockMvc.perform(post("/api/v1/brands/" + brandId + "/users")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","roleName":"BRAND_EDITOR"}
								""".formatted(targetEmail)))
				.andExpect(status().isCreated());
		for (String token : java.util.List.of(managerToken, adminToken)) {
			mockMvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + token))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$[?(@.userId == '" + targetId + "')]", hasSize(1)))
					.andExpect(jsonPath("$[?(@.userId == '" + targetId + "')].roleName",
							hasItem(org.hamcrest.Matchers.containsString("BRAND_VIEWER @"))))
					.andExpect(jsonPath("$[?(@.userId == '" + targetId + "')].roleName",
							hasItem(org.hamcrest.Matchers.containsString("BRAND_EDITOR @"))));
		}
		mockMvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + managerToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.userId == '" + targetId + "')].roleName",
						not(hasItem(org.hamcrest.Matchers.containsString("BRAND_ADMIN @")))));
		for (String token : java.util.List.of(adminToken, targetToken)) {
			mockMvc.perform(get("/api/v1/users/" + targetId).header("Authorization", "Bearer " + token))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.roleAssignments", hasSize(4)))
					.andExpect(jsonPath("$.permissions", hasItem("COLLECTOR_PROFILE:READ")));
		}
		String unrelatedToken = register("unrelated-" + UUID.randomUUID() + "@example.com", "Abc12345!");
		mockMvc.perform(get("/api/v1/users/" + userId(unrelatedToken))
						.header("Authorization", "Bearer " + managerToken))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/v1/users/" + targetId)
						.header("Authorization", "Bearer " + unrelatedToken))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/v1/users/assignable-roles").param("brandId", brandId)
						.header("Authorization", "Bearer " + managerToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasItem("BRAND_VIEWER")))
				.andExpect(jsonPath("$", not(hasItem("PLATFORM_ADMIN"))));
		mockMvc.perform(get("/api/v1/users/assignable-roles").param("brandId", otherBrandId)
						.header("Authorization", "Bearer " + managerToken))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/v1/users/" + targetId + "/roles")
						.header("Authorization", "Bearer " + managerToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"roleName\":\"PLATFORM_ADMIN\",\"scopeType\":\"PLATFORM\",\"scopeId\":null}"))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/v1/brands/" + otherBrandId + "/users")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","roleName":"BRAND_ADMIN"}
								""".formatted(managerEmail)))
				.andExpect(status().isCreated());
		managerToken = login(managerEmail, "Abc12345!");
		mockMvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + managerToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[?(@.userId == '" + targetId + "')]", hasSize(1)))
				.andExpect(jsonPath("$[?(@.userId == '" + targetId + "')].roleName",
						hasItem(org.hamcrest.Matchers.containsString("BRAND_ADMIN @"))));
	}

	@Test
	void brandAdmin_selfManagesOwnBrandUsers_butNotOthers() throws Exception {
		String adminToken = login("admin@example.com", "abc123");
		String brandId = createBrandAsAdmin(adminToken);
		String otherBrandId = createBrandAsAdmin(adminToken);

		String brandAdminEmail = "brand-admin-" + UUID.randomUUID() + "@example.com";
		String brandAdminPassword = "Abc12345!";
		register(brandAdminEmail, brandAdminPassword);

		// Platform admin grants BRAND_ADMIN on the first brand only.
		mockMvc.perform(post("/api/v1/brands/" + brandId + "/users")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email": "%s", "roleName": "BRAND_ADMIN"}
								""".formatted(brandAdminEmail)))
				.andExpect(status().isCreated());

		String brandAdminToken = login(brandAdminEmail, brandAdminPassword);

		// "My brands" shows exactly the one brand they were granted.
		mockMvc.perform(get("/api/v1/me/brands").header("Authorization", "Bearer " + brandAdminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].brandId").value(brandId))
				.andExpect(jsonPath("$[0].myRoleName").value("BRAND_ADMIN"));

		// Can grant a viewer role on their own brand.
		String viewerEmail = "brand-viewer-" + UUID.randomUUID() + "@example.com";
		register(viewerEmail, "Abc12345!");
		mockMvc.perform(post("/api/v1/brands/" + brandId + "/users")
						.header("Authorization", "Bearer " + brandAdminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email": "%s", "roleName": "BRAND_VIEWER"}
								""".formatted(viewerEmail)))
				.andExpect(status().isCreated());

		mockMvc.perform(get("/api/v1/brands/" + brandId + "/users").header("Authorization", "Bearer " + brandAdminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)));

		// Cannot manage users on a brand they have no grant for.
		mockMvc.perform(get("/api/v1/brands/" + otherBrandId + "/users")
						.header("Authorization", "Bearer " + brandAdminToken))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/v1/brands/" + otherBrandId + "/users")
						.header("Authorization", "Bearer " + brandAdminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email": "%s", "roleName": "BRAND_VIEWER"}
								""".formatted(viewerEmail)))
				.andExpect(status().isForbidden());
	}

	@Test
	void plainCollector_cannotListOrManageBrandUsers() throws Exception {
		String adminToken = login("admin@example.com", "abc123");
		String brandId = createBrandAsAdmin(adminToken);

		String collectorEmail = "collector-" + UUID.randomUUID() + "@example.com";
		String collectorToken = register(collectorEmail, "Abc12345!");

		mockMvc.perform(get("/api/v1/me/brands").header("Authorization", "Bearer " + collectorToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(0)));

		mockMvc.perform(get("/api/v1/brands/" + brandId + "/users").header("Authorization", "Bearer " + collectorToken))
				.andExpect(status().isForbidden());
	}
}
