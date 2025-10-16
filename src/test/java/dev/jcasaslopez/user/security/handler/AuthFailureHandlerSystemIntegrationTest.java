package dev.jcasaslopez.user.security.handler;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
import dev.jcasaslopez.user.enums.LoginFailureReason;
import dev.jcasaslopez.user.service.LoginAttemptService;
import dev.jcasaslopez.user.testhelper.TestHelper;
import dev.jcasaslopez.user.testhelper.UserTestBuilder;
import dev.jcasaslopez.user.utilities.Constants;

//@AutoConfigureMockMvc is needed because AuthenticationTestHelper requires MockMvc bean,
//which is not available by default in @SpringBootTest with RANDOM_PORT configuration.

@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AuthFailureHandlerSystemIntegrationTest {
	
	@Value ("${auth.maxFailedAttempts}") int maxNumberFailedAttempts;
	@Value ("${security.auth.account-lock-duration-seconds}") int accountLockDuration;

	@Autowired private TestRestTemplate testRestTemplate;
	@Autowired private TestHelper testHelper;
	@MockBean LoginAttemptService loginAttemptService;
	@Autowired private RedisTemplate<String, String> redisTemplate;

	// Immutable test constants defining the input data.
	private static final String USERNAME = "Yorch22";
	private static final String NON_EXISTANT_USERNAME = "Yorch66";
	private static final String PASSWORD = "Jorge22!";
	private static final String WRONG_PASSWORD = "Jorge2234444!";

	@BeforeEach
	void setUp() {
		UserTestBuilder builder = new UserTestBuilder(USERNAME, PASSWORD);
		testHelper.createAndPersistUser(builder);
	}

	@AfterEach
	void cleanAfterTest() {
		testHelper.cleanDataBaseAndRedis();
	}
	
	@Test
	@DisplayName("If username or password missing, it should return 400 BAD REQUEST")
	void CustomAuthenticationFailureHandler_WhenUsernameOrPasswordMissing_ShouldReturn400() {
		// Arrange		
		ArgumentCaptor<Boolean> attemptSuccessfulCaptor = ArgumentCaptor.forClass(Boolean.class);
		ArgumentCaptor<LoginFailureReason> loginFailureReasonCaptor = ArgumentCaptor.forClass(LoginFailureReason.class);
		
		HttpEntity<String> request = configHttpRequest(" ", PASSWORD);

		// Act
		ResponseEntity<StandardResponse> response = testRestTemplate.postForEntity(Constants.LOGIN_PATH, request, StandardResponse.class);

		// Assert
		verify(loginAttemptService).recordAttempt(attemptSuccessfulCaptor.capture(), anyString(), loginFailureReasonCaptor.capture(), any());
		assertAll(
			    () -> assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(),  "Response HTTP status should be 400 BAD REQUEST"),
			    () -> assertEquals("Username and password are required", response.getBody().getMessage(), "Unexpected response message"),
			    () -> assertNotNull(response.getBody().getMessage(), "Response body should not be null"),
			    () -> assertEquals(false, attemptSuccessfulCaptor.getValue(), "The login attempt should have failed, but was succesful"),
			    () -> assertEquals(LoginFailureReason.MISSING_FIELD, loginFailureReasonCaptor.getValue(), "Unexpected login failure reason")
			    );
	}
	
	@Test
	@DisplayName("If username not found, it should return 401 UNAUTHORIZED")
	void CustomAuthenticationFailureHandler_WhenUsernameNotFound_ShouldReturn401() {
		// Arrange		
		ArgumentCaptor<Boolean> attemptSuccessfulCaptor = ArgumentCaptor.forClass(Boolean.class);
		ArgumentCaptor<LoginFailureReason> loginFailureReasonCaptor = ArgumentCaptor.forClass(LoginFailureReason.class);
		
		HttpEntity<String> request = configHttpRequest(NON_EXISTANT_USERNAME, PASSWORD);

		// Act
		ResponseEntity<StandardResponse> response = testRestTemplate.postForEntity(Constants.LOGIN_PATH, request, StandardResponse.class);

		// Assert
		verify(loginAttemptService).recordAttempt(attemptSuccessfulCaptor.capture(), anyString(), loginFailureReasonCaptor.capture(), any());
		assertAll(
			    () -> assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode(),  "Response HTTP status should be 401 UNAUTHORIZED"),
			    () -> assertEquals("Bad credentials", response.getBody().getMessage(), "Unexpected response message"),
			    () -> assertNotNull(response.getBody().getMessage(), "Response body should not be null"),
			    () -> assertEquals(false, attemptSuccessfulCaptor.getValue(), "The login attempt should have failed, but was succesful"),
			    () -> assertEquals(LoginFailureReason.USER_NOT_FOUND, loginFailureReasonCaptor.getValue(), "Unexpected login failure reason")
			    );
	}
	
	@Test
	@DisplayName("If credentials are wrong, it should return 401 UNAUTHORIZED")
	void customAuthenticationFailureHandler_WhenCredentialsWrong_ShouldReturn401() {
		// Arrange		
		ArgumentCaptor<Boolean> attemptSuccessfulCaptor = ArgumentCaptor.forClass(Boolean.class);
		ArgumentCaptor<LoginFailureReason> loginFailureReasonCaptor = ArgumentCaptor.forClass(LoginFailureReason.class);
		
		HttpEntity<String> request = configHttpRequest(USERNAME, WRONG_PASSWORD);

		// Act
		ResponseEntity<StandardResponse> response = testRestTemplate.postForEntity(Constants.LOGIN_PATH, request, StandardResponse.class);

		// Assert
		verify(loginAttemptService).recordAttempt(attemptSuccessfulCaptor.capture(), anyString(), loginFailureReasonCaptor.capture(), any());
		assertAll(
			    () -> assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode(),  "Response HTTP status should be 401 UNAUTHORIZED"),
			    () -> assertEquals("Bad credentials", response.getBody().getMessage(), "Unexpected response message"),
			    () -> assertNotNull(response.getBody().getMessage(), "Response body should not be null"),
			    () -> assertEquals(false, attemptSuccessfulCaptor.getValue(), "The login attempt should have failed, but was succesful"),
			    () -> assertEquals(LoginFailureReason.INCORRECT_PASSWORD, loginFailureReasonCaptor.getValue(), "Unexpected login failure reason")
			    );
	}
	
	@ParameterizedTest
	@DisplayName("When account is locked, it should return 403 FORBIDDEN with appropriate message")
	@MethodSource("provideLockedAccountScenarios")
	void customAuthenticationFailureHandler_WhenAccountLocked_ShouldReturn403(AccountStatus accountStatus, 
	        String expectedMessage, boolean needsRedisEntry) {  
	    // Arrange		
	    ArgumentCaptor<Boolean> attemptSuccessfulCaptor = ArgumentCaptor.forClass(Boolean.class);
	    ArgumentCaptor<LoginFailureReason> loginFailureReasonCaptor = ArgumentCaptor.forClass(LoginFailureReason.class);
	    
	    testHelper.cleanDataBaseAndRedis();
	    UserTestBuilder builder = new UserTestBuilder(USERNAME, PASSWORD).withAccountStatus(accountStatus);
	    testHelper.createAndPersistUser(builder);
	    
	    if (needsRedisEntry) {
	    	String redisKey = Constants.LOGIN_ATTEMPTS_REDIS_KEY + USERNAME;
	    	String maxNumberFailedAttemptsAsString = String.valueOf(maxNumberFailedAttempts);
	    	redisTemplate.opsForValue().set(redisKey, maxNumberFailedAttemptsAsString, accountLockDuration, TimeUnit.SECONDS);
	    }
	    
	    HttpEntity<String> request = configHttpRequest(USERNAME, PASSWORD);

	    // Act
	    ResponseEntity<StandardResponse> response = testRestTemplate.postForEntity(
	        Constants.LOGIN_PATH, request, StandardResponse.class);

	    // Assert
	    verify(loginAttemptService).recordAttempt(attemptSuccessfulCaptor.capture(), anyString(), loginFailureReasonCaptor.capture(), any());
	    
	    assertAll(
	        () -> assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(), "Response HTTP status should be 403 FORBIDDEN"),
	        () -> assertEquals(expectedMessage, response.getBody().getMessage(), "Unexpected response message"),
	        () -> assertNotNull(response.getBody().getMessage(), "Response body should not be null"),
	        () -> assertEquals(false, attemptSuccessfulCaptor.getValue(), "The login attempt should have failed, but was successful"),
	        () -> assertEquals(LoginFailureReason.ACCOUNT_LOCKED, loginFailureReasonCaptor.getValue(), "Unexpected login failure reason")
	    );
	}

	private static Stream<Arguments> provideLockedAccountScenarios() {
	    return Stream.of(
	        Arguments.of(AccountStatus.TEMPORARILY_BLOCKED,
	            "Your account is locked due to too many failed login attempts. It will be reactivated automatically in a few hours",
	            true  
	        ),
	        Arguments.of(
	            AccountStatus.BLOCKED,
	            "Your account has been locked by an administrator. Please contact support if you believe this is a mistake",
	            false  
	        ),
	        Arguments.of(
	            AccountStatus.PERMANENTLY_SUSPENDED,
	            "Your account is permanently suspended",
	            false  
	        )
	    );
	}
	
	private HttpEntity<String> configHttpRequest(String username, String password){
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);		
		String body = "username=" + username + "&password=" + password;
		return new HttpEntity<>(body, headers);
	}

}
