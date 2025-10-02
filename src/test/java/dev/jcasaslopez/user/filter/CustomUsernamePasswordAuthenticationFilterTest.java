package dev.jcasaslopez.user.filter;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import dev.jcasaslopez.user.dto.StandardResponse;
import dev.jcasaslopez.user.entity.User;
import dev.jcasaslopez.user.enums.AccountStatus;
import dev.jcasaslopez.user.service.EmailService;
import dev.jcasaslopez.user.service.UserAccountService;
import dev.jcasaslopez.user.testhelper.TestHelper;
import dev.jcasaslopez.user.testhelper.UserTestBuilder;
import dev.jcasaslopez.user.utilities.Constants;

// Account lockout mechanism REMINDER

// Scenario 1: Redis lock ACTIVE
// - Account status: TEMPORARILY_BLOCKED  
// - Redis entry: PRESENT with TTL
// - Expected: 403 Forbidden

// Scenario 2: Redis lock EXPIRED + Account blocked  
// - Account status: TEMPORARILY_BLOCKED
// - Redis entry: ABSENT (TTL expired)
// - Expected: Auto-reactivate to ACTIVE + login success

// Scenario 3: No lock ever existed
// - Account status: ACTIVE
// - Redis entry: ABSENT
// - Expected: Normal login

// @AutoConfigureMockMvc is needed because AuthenticationTestHelper requires MockMvc bean,
// which is not available by default in @SpringBootTest with RANDOM_PORT configuration.

@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CustomUsernamePasswordAuthenticationFilterTest {
	
	@Value ("${auth.maxFailedAttempts}") int maxNumberFailedAttempts;
	
	@Autowired private TestRestTemplate testRestTemplate;
	@Autowired private RedisTemplate<String, String> redisTemplate;
	@Autowired private UserAccountService userAccountService;
	@Autowired private TestHelper testHelper;
	
	// Allows verification that the 'sendEmail' method was invoked, without actually sending it.
	@MockBean private EmailService emailService;
	
	// Immutable test constants defining the input data.
	private static final String USERNAME = "Yorch22";
	private static final String PASSWORD = "Jorge22!";

	@BeforeEach
	void setUp() {
		UserTestBuilder builder = new UserTestBuilder(USERNAME, PASSWORD)
											.withAccountStatus(AccountStatus.TEMPORARILY_BLOCKED);
		testHelper.createAndPersistUser(builder);
		
	}
	
	@AfterEach
	void cleanAfterTest() {
		testHelper.cleanDataBaseAndRedis();
	}

	// The user's account has been set to "TEMPORARILY BLOCKED" straight in the database, by-passing the account lockout 
	// mechanism, so there is no Redis entry. We are in scenario 2 (see comment at the beginning of the class).
	@Test
	@DisplayName("Switches account to active when lock timeout is over")
	void WhenLockTimeoutOver_ShouldSwitchAccountToActive() {
		// Arrange	
		HttpEntity<String> request = setHttpRequest(USERNAME, PASSWORD);
		
		// Act
		ResponseEntity<StandardResponse> response = testRestTemplate.postForEntity(Constants.LOGIN_PATH, request, StandardResponse.class);
								
		ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
		verify(emailService).sendEmail(anyString(), anyString(), bodyCaptor.capture());
		String emailBody = bodyCaptor.getValue();
		
		AccountStatus finalAccountStatus = userAccountService.findUser(USERNAME).getAccountStatus();
	
		// Assert
		assertAll(
				() -> assertEquals(AccountStatus.ACTIVE, finalAccountStatus, "User account status should be ACTIVE"),
				() -> assertTrue(emailBody.contains("Your account is active again."), "Email body does not contain expected content"),
				() -> assertEquals(HttpStatus.OK, response.getStatusCode(), "HTTP response status should be 200 OK"),
				() -> assertEquals("Login attempt successful", response.getBody().getMessage(), "Unexpected HTTP response message"),
				() -> assertNotNull(response.getBody().getDetails(), "Response body should not be null")
		);
	}
	
	// The user's account has been set to "TEMPORARILY BLOCKED", but there is a Redis entry: we are in scenario 1 
	// (see comment at the beginning of the class).
	@Test
	@DisplayName("If the Redis entry is present should return 403 FORBIDDEN")
	void WhenLockTimeoutIsNotOver_ShoulddReturn403Forbidden() {
		// Arrange
    	String redisKey = Constants.LOGIN_ATTEMPTS_REDIS_KEY + USERNAME;
		redisTemplate.opsForValue().set(redisKey, "3", 5, TimeUnit.MINUTES);
		
		HttpEntity<String> request = setHttpRequest(USERNAME, PASSWORD);
		
		// Act
		ResponseEntity<StandardResponse> response = testRestTemplate.postForEntity(Constants.LOGIN_PATH, request, StandardResponse.class);
				
		// Assert
		assertAll(
				() -> assertEquals(AccountStatus.TEMPORARILY_BLOCKED, userAccountService.findUser(USERNAME).getAccountStatus(), "User account status shoud be TEMPORARILY_BLOCKED"),
				() -> assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(), "HTTP response status should be 403 FORBIDDEN"),
				() -> assertEquals("Your account is locked due to too many failed login attempts. It will be reactivated automatically in a few hours", 
						response.getBody().getMessage(), "Unexpected HTTP response message")
		);
	}
	
	@Test
	@DisplayName("After maximum number of failed logins, account gets locked")
	void afterThreeFailedLogins_AccountGetsLocked() {
	    // Arrange - Perform 2 failed login attempts (just below the threshold)
		String wrongPassword = "Jorge66!";
		HttpEntity<String> requestWithWrongPassword = setHttpRequest(USERNAME, wrongPassword);
		
	    for (int i = 0; i <= maxNumberFailedAttempts - 1; i++) {
	        // These failed attempts should increment the Redis counter but not trigger lockout yet
	        testRestTemplate.postForEntity(Constants.LOGIN_PATH, requestWithWrongPassword, StandardResponse.class);
	    }
	    
	    // Act - last failed attempt (should trigger account lockout)
	    ResponseEntity<StandardResponse> response = testRestTemplate.postForEntity(Constants.LOGIN_PATH, requestWithWrongPassword, StandardResponse.class);
		AccountStatus finalAccountStatus = userAccountService.findUser(USERNAME).getAccountStatus();

	    // Assert
	    assertAll(
	        () -> assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(), "3rd failed attempt should return 403 FORBIDDEN"),
	        () -> assertEquals(AccountStatus.TEMPORARILY_BLOCKED, finalAccountStatus, "Account status should be TEMPORARILY_BLOCKED after max failed attempts")
	    );
	}
	
	@Test
	@DisplayName("If request has no username/password, returns 400 BAD CREDENTIALS")
	void WhenNoUsernameOrPassword_ShouldThrowException() {
		// Arrange
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		
		HttpEntity<String> request = setHttpRequest(" ", PASSWORD);
		
		// Act
		ResponseEntity<StandardResponse> response = testRestTemplate.postForEntity(Constants.LOGIN_PATH, request, StandardResponse.class);
		
		// Assert
		assertAll(
				() -> assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), "HTTP response status should be 400 BAD_REQUEST"),
				() -> assertEquals("Username and password are required", response.getBody().getMessage(), "Unexpected HTTP response message")
		);
	}
	
	private HttpEntity<String> setHttpRequest(String username, String password) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);		
		String body = "username=" + username + "&password=" + password;
		return new HttpEntity<>(body, headers);
	}
}