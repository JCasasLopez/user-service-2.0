package dev.jcasaslopez.user.controller;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import dev.jcasaslopez.user.dto.StandardResponse;
import dev.jcasaslopez.user.enums.TokenType;
import dev.jcasaslopez.user.service.TokenService;
import dev.jcasaslopez.user.testhelper.AuthenticationTestHelper;
import dev.jcasaslopez.user.testhelper.TestHelper;
import dev.jcasaslopez.user.testhelper.UserTestBuilder;
import dev.jcasaslopez.user.utilities.Constants;
import io.jsonwebtoken.Claims;

// @AutoConfigureMockMvc is needed because AuthenticationTestHelper requires MockMvc bean,
// which is not available by default in @SpringBootTest with RANDOM_PORT configuration.

@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RefreshTokenE2ETest {
	
	@Autowired private TokenService tokenService;
	@Autowired private TestRestTemplate testRestTemplate;
	@Autowired private TestHelper testHelper;
	@Autowired private AuthenticationTestHelper authTestHelper;
	
	// Immutable test constants defining the input data.
	private static final String USERNAME = "Yorch22";
	private static final String PASSWORD = "Password123!";

	@BeforeEach
	void setUp() {
		UserTestBuilder builder = new UserTestBuilder(USERNAME, PASSWORD);
		testHelper.createAndPersistUser(builder);
	}

	@AfterEach
	void cleanUp() {
	    testHelper.cleanDataBaseAndRedis();
	}
	
	@Test
	@DisplayName("Refresh token endpoint returns an access and a refresh token")
	public void refreshToken_WhenAuthenticated_ShouldReturnAccessAndRefreshToken() {
		// Arrange
		String refreshToken = authTestHelper.logInWithTestRestTemplate(USERNAME, PASSWORD).getRefreshToken();
		HttpHeaders headers = new HttpHeaders();
	    headers.setBearerAuth(refreshToken);
		HttpEntity<Void> request = new HttpEntity<>(headers);
		
		// Act
		ResponseEntity<StandardResponse> response = testRestTemplate.postForEntity(Constants.REFRESH_TOKEN_PATH, request, StandardResponse.class);
		
		// Assert
		// The endpoint is guaranteed to return a List<String>, making this cast safe.
		@SuppressWarnings("unchecked")
		List<String> tokens = (List<String>) response.getBody().getDetails();
		
		assertAll(
		        () -> assertEquals(HttpStatus.CREATED, response.getBody().getStatus()),
		        () -> assertNotNull(response.getBody()),
		        () -> assertEquals("New refresh and access tokens sent successfully", response.getBody().getMessage()),
		        () -> assertEquals(2, tokens.size()),
		        () -> assertTokenPurpose(tokens.get(0), TokenType.REFRESH),
		        () -> assertTokenPurpose(tokens.get(1), TokenType.ACCESS)
		    );
	}
	
	private void assertTokenPurpose(String token, TokenType expected) {
	    Claims claims = tokenService.getValidClaims(token).orElseThrow();
	    String purposeStr = claims.get("purpose", String.class);
	    TokenType purpose = TokenType.valueOf(purposeStr);
	    assertEquals(expected, purpose);
	}
}