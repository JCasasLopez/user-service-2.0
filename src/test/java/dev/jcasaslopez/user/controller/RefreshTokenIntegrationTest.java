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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import dev.jcasaslopez.user.dto.StandardResponse;
import dev.jcasaslopez.user.entity.User;
import dev.jcasaslopez.user.enums.TokenType;
import dev.jcasaslopez.user.service.TokenService;
import dev.jcasaslopez.user.testhelper.TestHelper;
import dev.jcasaslopez.user.utilities.Constants;
import io.jsonwebtoken.Claims;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RefreshTokenIntegrationTest {
	
	@Autowired private TokenService tokenService;
	@Autowired private TestRestTemplate testRestTemplate;
	@Autowired private TestHelper testHelper;
	
	private static User user;

	@BeforeEach
	void setUp() {
	    user = testHelper.createUser("Yorch22", "Jorge22!");
	}

	@AfterEach
	void cleanUp() {
	    testHelper.cleanDataBaseAndRedis();
	}
	
	@Test
	@DisplayName("Refresh token endpoint returns an access and a refresh token")
	public void refreshToken_WhenAuthenticated_ShouldReturnAccessAndRefreshToken() {
		// Arrange
		String refreshToken = testHelper.loginUser(user, TokenType.REFRESH);
		HttpEntity<Void> request = buildRequestWithToken(refreshToken);
		
		// Act
		ResponseEntity<StandardResponse> response = testRestTemplate
		        .postForEntity(Constants.REFRESH_TOKEN_PATH, request, StandardResponse.class);
		
		// Assert
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
	
	private HttpEntity<Void> buildRequestWithToken(String token) {
	    HttpHeaders headers = new HttpHeaders();
	    headers.setBearerAuth(token);
	    return new HttpEntity<>(headers);
	}

	private void assertTokenPurpose(String token, TokenType expected) {
	    Claims claims = tokenService.getValidClaims(token).orElseThrow();
	    String purposeStr = claims.get("purpose", String.class);
	    TokenType purpose = TokenType.valueOf(purposeStr);
	    assertEquals(expected, purpose);
	}
}