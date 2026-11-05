package com.example.collectb.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.example.collectb.PostgresTestcontainerConfiguration;
import com.jayway.jsonpath.JsonPath;

/**
 * End-to-end test of the hosted-login-form authorization_code+PKCE flow
 * ({@code /oauth2/authorize} -&gt; {@code /login} -&gt; {@code /oauth2/authorize} -&gt;
 * {@code /oauth2/token}), proving that the browser flow authenticates against the very same
 * {@link com.example.collectb.identity.security.PlatformUserDetailsService} (and therefore the
 * same role/permission claims) as the direct {@code /api/v1/auth/login} REST endpoint exercised
 * by {@link AuthControllerIT}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestcontainerConfiguration.class)
class Oauth2PkceAuthorizationCodeFlowIT {

	private static final String CLIENT_ID = "collectb-web";
	private static final String REDIRECT_URI = "http://127.0.0.1:3000/callback";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtDecoder jwtDecoder;

	@Test
	void authorizationCodeWithPkce_afterFormLogin_issuesTokenForRegisteredUser() throws Exception {
		String email = "pkce-flow-" + UUID.randomUUID() + "@example.com";
		String password = "Abc12345!";
		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email": "%s", "displayName": "PKCE Flow User", "password": "%s"}
								""".formatted(email, password)))
				.andExpect(status().isCreated());

		String codeVerifier = randomUrlSafeString(64);
		String codeChallenge = s256(codeVerifier);
		String state = randomUrlSafeString(16);
		String authorizeUri = "/oauth2/authorize?response_type=code&client_id=" + CLIENT_ID
				+ "&scope=openid&redirect_uri=" + REDIRECT_URI
				+ "&code_challenge=" + codeChallenge + "&code_challenge_method=S256&state=" + state;

		MockHttpSession session = new MockHttpSession();

		// 1. Unauthenticated authorize request -> redirected to the hosted login form.
		MvcResult authorizeRedirect = mockMvc.perform(get(authorizeUri).session(session))
				.andExpect(status().is3xxRedirection())
				.andReturn();
		String loginRedirect = authorizeRedirect.getResponse().getRedirectedUrl();
		assertThat(loginRedirect).contains("/login");

		// 2. Submit the hosted login form against the seeded registration's credentials; this
		// goes through the DaoAuthenticationProvider backed by PlatformUserDetailsService.
		MvcResult loginResult = mockMvc.perform(post("/login")
						.session(session)
						.with(SecurityMockMvcRequestPostProcessors.csrf())
						.param("username", email)
						.param("password", password))
				.andExpect(status().is3xxRedirection())
				.andReturn();
		assertThat(loginResult.getResponse().getRedirectedUrl()).isNotEqualTo("/login?error");

		// 3. Re-issue the authorize request in the now-authenticated session; with consent
		// disabled for this client, it redirects straight to the redirect_uri with a code.
		MvcResult authorizeAgain = mockMvc.perform(get(authorizeUri).session(session))
				.andExpect(status().is3xxRedirection())
				.andReturn();
		String redirectWithCode = authorizeAgain.getResponse().getRedirectedUrl();
		assertThat(redirectWithCode).startsWith(REDIRECT_URI);
		String code = extractQueryParam(redirectWithCode, "code");
		assertThat(code).isNotBlank();

		// 4. Exchange the authorization code (+ PKCE verifier) for tokens. Public client
		// (ClientAuthenticationMethod.NONE), so no client secret/Basic auth is presented.
		MvcResult tokenResult = mockMvc.perform(post("/oauth2/token")
						.contentType(MediaType.APPLICATION_FORM_URLENCODED)
						.param("grant_type", "authorization_code")
						.param("client_id", CLIENT_ID)
						.param("code", code)
						.param("redirect_uri", REDIRECT_URI)
						.param("code_verifier", codeVerifier))
				.andExpect(status().isOk())
				.andReturn();

		String tokenBody = tokenResult.getResponse().getContentAsString();
		String accessToken = JsonPath.read(tokenBody, "$.access_token");
		assertThat(accessToken).isNotBlank();

		// 5. The access token's claims must reflect the PlatformUserPrincipal loaded by
		// PlatformUserDetailsService for the very user who just logged in via the form.
		Jwt jwt = jwtDecoder.decode(accessToken);
		assertThat(jwt.getClaimAsStringList("roles")).contains("COLLECTOR");
		String userId = jwt.getClaim("user_id");
		String actorType = jwt.getClaim("actor_type");
		assertThat(userId).isNotBlank();
		assertThat(actorType).isEqualTo("COLLECTOR");
	}

	private static String s256(String verifier) throws Exception {
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		byte[] hash = digest.digest(verifier.getBytes(StandardCharsets.US_ASCII));
		return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
	}

	private static String randomUrlSafeString(int length) {
		byte[] bytes = new byte[length];
		new java.security.SecureRandom().nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).substring(0, length);
	}

	private static String extractQueryParam(String url, String name) {
		Pattern pattern = Pattern.compile("[?&]" + Pattern.quote(name) + "=([^&]+)");
		Matcher matcher = pattern.matcher(URI.create(url).getRawQuery() == null ? "" : "?" + URI.create(url).getRawQuery());
		if (matcher.find()) {
			return matcher.group(1);
		}
		throw new IllegalStateException("Query param " + name + " not found in " + url);
	}
}
