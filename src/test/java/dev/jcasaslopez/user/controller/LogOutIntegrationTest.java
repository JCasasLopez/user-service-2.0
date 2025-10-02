package dev.jcasaslopez.user.controller;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.core.JsonProcessingException;

import dev.jcasaslopez.user.dto.StandardResponse;
import dev.jcasaslopez.user.testhelper.AuthenticationTestHelper;
import dev.jcasaslopez.user.testhelper.TestHelper;
import dev.jcasaslopez.user.testhelper.UserTestBuilder;
import dev.jcasaslopez.user.utilities.Constants;

// We verify the 'logout' endpoint because its logic is custom and does not follow Spring Security standard flow. 
// In contrast, endpoints like 'login' are handled directly by Spring and do not require dedicated integration tests.

// @AutoConfigureMockMvc is needed because AuthenticationTestHelper requires MockMvc bean,
// which is not available by default in @SpringBootTest with RANDOM_PORT configuration.

@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class LogOutIntegrationTest {
	
	@Autowired private TestHelper testHelper;
	@Autowired private AuthenticationTestHelper authTestHelper;
	@Autowired private TestRestTemplate testRestTemplate;
	@Autowired private RedisTemplate<String, String> redisTemplate;
	
	// Immutable test constants defining the input data.
	private static final String USERNAME = "Yorch22";
	private static final String PASSWORD = "Password123!";
	
	@BeforeEach
	void setup() throws JsonProcessingException {
		UserTestBuilder builder = new UserTestBuilder(USERNAME, PASSWORD);
		testHelper.createAndPersistUser(builder);
	}
	
	@AfterEach
	void cleanDatabase() {
		testHelper.cleanDataBaseAndRedis();
	}
	
	@Test
	@DisplayName("User logs out correctly when valid refresh token is provided")
	void logOut_WhenRefreshTokenProvided_ShouldLogOutUserAndRevokeToken() {
		// Arrange
		String refreshToken = authTestHelper.logInWithTestRestTemplate(USERNAME, PASSWORD).getRefreshToken();
		String redisKey = testHelper.buildRedisKey(refreshToken, Constants.REFRESH_TOKEN_REDIS_KEY);
		
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(refreshToken);
		HttpEntity<String> request = new HttpEntity<>(headers);
		
		// Act
		ResponseEntity<StandardResponse> response = testRestTemplate.postForEntity(Constants.LOGOUT_PATH, request, StandardResponse.class);
		
		// Assert
		String redisEntryValue = redisTemplate.opsForValue().get(redisKey);
		
		// Spring Security handles the request in a different thread than the test, and since SecurityContextHolder
		// is thread-local, we can't verify in the test the changes made inside the filter. That
		// verification is done in the dedicated unit tests for TokenService.logOut().
		assertAll(
				() -> assertEquals(HttpStatus.OK, response.getStatusCode(), "Expected 200 OK status code"),
				() -> assertNotNull(response.getBody(), "Response body should not be null"),
				() -> assertEquals("The user has been logged out", response.getBody().getMessage(), "Logout message mismatch"),
				() -> assertEquals("blacklisted", redisEntryValue, "Refresh token should be blacklisted in Redis")
				);
			}
	
	@Test
	@DisplayName("User cannot log out when access or invalid token is provided")
	void logOut_WhenAccessOrInvalidTokenProvided_ShouldNotLogOutUser() {
		// Arrange
		String accessToken = authTestHelper.logInWithTestRestTemplate(USERNAME, PASSWORD).getAccessToken();
		
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(accessToken);
		HttpEntity<String> request = new HttpEntity<>(headers);
		
		// Act
		ResponseEntity<StandardResponse> response = testRestTemplate.postForEntity(Constants.LOGOUT_PATH, request, StandardResponse.class);
		
		// Assert
		assertAll(
				() -> assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode(), "Expected 401 UNAUTHORIZED status code"),
				() -> assertNotNull(response.getBody(), "Response body should not be null"),
				() -> assertTrue(response.getBody().getMessage().contains("Access denied"), "Unexpected response message")
				);
		}
}