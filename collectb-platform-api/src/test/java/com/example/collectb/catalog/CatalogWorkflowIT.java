package com.example.collectb.catalog;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.example.collectb.PostgresTestcontainerConfiguration;
import com.example.collectb.catalog.security.CatalogStepUpAuthorizationService;

@SpringBootTest(properties = "collectb.catalog.allow-development-mfa-bypass=false")
@AutoConfigureMockMvc
@Import(PostgresTestcontainerConfiguration.class)
class CatalogWorkflowIT {

	private static final String PASSWORD = "CatalogTest123!";
	private static final String HEX_HASH = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CatalogStepUpAuthorizationService stepUpAuthorization;

	@Test
	void brandEditorCreatesCatalogAndBrandAdminReviewsAndPublishesWithStepUp() throws Exception {
		String adminToken = login("admin@example.com", "abc123");
		String brandId = createActiveBrand(adminToken);
		String editorEmail = "catalog-editor-" + UUID.randomUUID() + "@example.com";
		String reviewerEmail = "catalog-reviewer-" + UUID.randomUUID() + "@example.com";
		register(editorEmail);
		register(reviewerEmail);
		grantRole(adminToken, brandId, editorEmail, "BRAND_EDITOR");
		grantRole(adminToken, brandId, reviewerEmail, "BRAND_ADMIN");
		String editorToken = login(editorEmail, PASSWORD);
		String reviewerToken = login(reviewerEmail, PASSWORD);

		String mediaResponse = mockMvc.perform(post("/api/v1/brands/" + brandId + "/media-assets")
						.header("Authorization", "Bearer " + editorToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"assetType":"ARTWORK","storageUri":"s3://brand-assets/charizard.png",
								 "cdnUri":"https://cdn.example.com/charizard.png","contentType":"image/png",
								 "checksum":"%s","fileSizeBytes":1024}
								""".formatted(HEX_HASH)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		String mediaAssetId = jsonId(mediaResponse, "mediaAssetId");

		String collectionResponse = mockMvc.perform(post("/api/v1/brands/" + brandId + "/collections")
						.header("Authorization", "Bearer " + editorToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"collectionCode":"GENESIS","name":"Digital Genesis",
								 "description":"First release","releaseStartAt":null,"releaseEndAt":null}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.releaseStatus").value("DRAFT"))
				.andReturn().getResponse().getContentAsString();
		String collectionId = jsonId(collectionResponse, "collectionId");

		String templateResponse = mockMvc.perform(post("/api/v1/brands/" + brandId + "/collections/" + collectionId
						+ "/templates")
						.header("Authorization", "Bearer " + editorToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"templateCode":"CHARIZARD-HOLO","displayName":"Charizard Holographic",
								 "rarity":"LEGENDARY","editionSize":1000,"metadataSchemaVersion":"1.0",
								 "metadataUri":"s3://brand-assets/charizard.json","metadataHash":"%s",
								 "mediaAssetId":"%s","traits":{"element":"fire","finish":"holographic"}}
								""".formatted(HEX_HASH, mediaAssetId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.publishStatus").value("DRAFT"))
				.andExpect(jsonPath("$.traits.element").value("fire"))
				.andReturn().getResponse().getContentAsString();
		String templateId = jsonId(templateResponse, "templateId");

		mockMvc.perform(post("/api/v1/brands/" + brandId + "/collections/" + collectionId + "/submit-for-review")
						.header("Authorization", "Bearer " + editorToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.releaseStatus").value("READY_FOR_REVIEW"));

		if (stepUpAuthorization.isMfaRequired()) {
			mockMvc.perform(post("/api/v1/brands/" + brandId + "/collections/" + collectionId + "/review")
						.header("Authorization", "Bearer " + reviewerToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"decision\":\"APPROVE\",\"comment\":null}"))
				.andExpect(status().isForbidden());
		}

		mockMvc.perform(post("/api/v1/brands/" + brandId + "/collections/" + collectionId + "/review")
						.with(stepUpAuthorization.isMfaRequired() ? stepUp(reviewerEmail, userId(reviewerToken), brandId,
								"COLLECTION:APPROVE", "COLLECTION:PUBLISH", "COLLECTION:RETIRE",
								"TEMPLATE:APPROVE", "TEMPLATE:PUBLISH", "TEMPLATE:RETIRE")
								: request -> {
									request.addHeader("Authorization", "Bearer " + reviewerToken);
									return request;
								})
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"decision\":\"APPROVE\",\"comment\":null}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.releaseStatus").value("APPROVED"));

		mockMvc.perform(post("/api/v1/brands/" + brandId + "/collections/" + collectionId + "/publish")
						.with(stepUp(reviewerEmail, userId(reviewerToken), brandId, "COLLECTION:PUBLISH")))
				.andExpect(status().isConflict());

		mockMvc.perform(post("/api/v1/brands/" + brandId + "/collections/" + collectionId + "/templates/"
						+ templateId + "/submit-for-review")
						.header("Authorization", "Bearer " + editorToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.publishStatus").value("READY_FOR_REVIEW"));

		mockMvc.perform(post("/api/v1/brands/" + brandId + "/collections/" + collectionId + "/templates/"
						+ templateId + "/review")
						.with(stepUp(reviewerEmail, userId(reviewerToken), brandId, "TEMPLATE:APPROVE"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"decision\":\"APPROVE\",\"comment\":null}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.publishStatus").value("APPROVED"));

		mockMvc.perform(post("/api/v1/brands/" + brandId + "/collections/" + collectionId + "/templates/"
						+ templateId + "/publish")
						.with(stepUp(reviewerEmail, userId(reviewerToken), brandId, "TEMPLATE:PUBLISH")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.publishStatus").value("PUBLISHED"));

		mockMvc.perform(post("/api/v1/brands/" + brandId + "/collections/" + collectionId + "/publish")
						.with(stepUp(reviewerEmail, userId(reviewerToken), brandId, "COLLECTION:PUBLISH")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.releaseStatus").value("PUBLISHED"));
	}

	@Test
	void collectionAndTemplateMutationsRequireTheAssignedBrandScope() throws Exception {
		String adminToken = login("admin@example.com", "abc123");
		String brandId = createActiveBrand(adminToken);
		String editorEmail = "catalog-scope-" + UUID.randomUUID() + "@example.com";
		register(editorEmail);
		grantRole(adminToken, brandId, editorEmail, "BRAND_EDITOR");
		String editorToken = login(editorEmail, PASSWORD);
		String otherBrandId = createActiveBrand(adminToken);

		mockMvc.perform(post("/api/v1/brands/" + otherBrandId + "/collections")
						.header("Authorization", "Bearer " + editorToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"collectionCode":"OUTSIDE","name":"Outside brand",
								 "description":null,"releaseStartAt":null,"releaseEndAt":null}
								"""))
				.andExpect(status().isForbidden());
	}

	private String createActiveBrand(String adminToken) throws Exception {
		String code = "catalog-" + UUID.randomUUID().toString().substring(0, 8);
		String response = mockMvc.perform(post("/api/v1/brands")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"brandCode":"%s","name":"%s","description":"Catalog workflow test"}
								""".formatted(code, code)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		String brandId = jsonId(response, "brandId");
		mockMvc.perform(patch("/api/v1/admin/brands/" + brandId + "/license-status")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"licenseStatus\":\"ACTIVE\"}"))
				.andExpect(status().isOk());
		return brandId;
	}

	private void register(String email) throws Exception {
		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","displayName":"Catalog Test User","password":"%s"}
								""".formatted(email, PASSWORD)))
				.andExpect(status().isCreated());
	}

	private void grantRole(String adminToken, String brandId, String email, String roleName) throws Exception {
		mockMvc.perform(post("/api/v1/admin/brands/" + brandId + "/users")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","roleName":"%s"}
								""".formatted(email, roleName)))
				.andExpect(status().isCreated());
	}

	private String login(String email, String password) throws Exception {
		String response = mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","password":"%s"}
								""".formatted(email, password)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return response.split("\"accessToken\":\"")[1].split("\"")[0];
	}

	private String userId(String accessToken) throws Exception {
		String response = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
						"/api/v1/me")
						.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return jsonId(response, "userId");
	}

	private RequestPostProcessor stepUp(String email, String userId, String brandId, String... authorities) {
		GrantedAuthority[] grantedAuthorities = java.util.Arrays.stream(authorities)
				.map(SimpleGrantedAuthority::new)
				.toArray(GrantedAuthority[]::new);
		return jwt().jwt(token -> token
				.subject(email)
				.claim("user_id", userId)
				.claim("auth_strength", "mfa")
				.claim("scopes", java.util.List.of(Map.of(
						"role", "BRAND_ADMIN",
						"scope_type", "BRAND",
						"scope_id", brandId))))
				.authorities(grantedAuthorities);
	}

	private static String jsonId(String json, String property) {
		return json.split("\"" + property + "\":\"")[1].split("\"")[0];
	}
}
