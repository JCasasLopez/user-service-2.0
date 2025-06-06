package dev.jcasaslopez.user.filter;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;

import dev.jcasaslopez.user.dto.StandardResponse;
import dev.jcasaslopez.user.entity.User;
import dev.jcasaslopez.user.enums.AccountStatus;
import dev.jcasaslopez.user.repository.UserRepository;
import dev.jcasaslopez.user.service.EmailService;
import dev.jcasaslopez.user.service.UserAccountService;
import dev.jcasaslopez.user.testhelper.TestHelper;
import dev.jcasaslopez.user.utilities.Constants;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CustomUsernamePasswordAuthenticationFilterTest {
	
	@Autowired private TestRestTemplate testRestTemplate;
	@Autowired private RedisTemplate<String, String> redisTemplate;
	@Autowired private UserAccountService userAccountService;
	@Autowired private UserRepository userRepository;
	@MockBean private EmailService emailService;
	@Autowired private TestHelper testHelper;
	
	private static User user;
	private static final String username = "Yorch22";
	private static final String password = "Jorge22!";

	@BeforeAll
	void setUp() {
		user = testHelper.createUser(username, password);
		
		// Por defecto, la cuenta se pone como ACTIVE, así que hay que bloquear la cuenta 'a mano'.
		//
		// By default, account is set as ACTIVE, so we have to block the account.
		user.setAccountStatus(AccountStatus.TEMPORARILY_BLOCKED);
		userRepository.save(user);
		userRepository.flush();
	}
	
	@AfterAll
	void cleanAfterTest() {
		testHelper.cleanDataBaseAndRedis();
	}

	@Test
	@DisplayName("Switches account to active when lock timeout is over")
	@Order(1)
	void WhenLockTimeoutOver_ShouldSwitchAccountToActive() {
		// Arrange	
		HttpEntity<String> request = setHttpRequest();
		
		// Act
		ResponseEntity<StandardResponse> response = testRestTemplate
				.postForEntity("/login", request, StandardResponse.class);
								
		ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
		verify(emailService).sendEmail(anyString(), anyString(), bodyCaptor.capture());
		String emailBody = bodyCaptor.getValue();
	
		// Assert
		assertAll(
				() -> assertEquals(AccountStatus.ACTIVE, userAccountService.findUser(username).getAccountStatus(),
						"User account status should be ACTIVE"),
				() -> assertTrue(emailBody.contains("Your account is active again."),
						"Email body does not contain expected content"),
				() -> assertEquals(HttpStatus.OK, response.getStatusCode(),
						"HTTP response status should be 200 OK"),
				() -> assertEquals("Login attempt successful", response.getBody().getMessage(),
						"Unexpected HTTP response message"),
				() -> assertNotNull(response.getBody().getDetails(),
						"Response body should not be null")
		);
	}
	
	@Test
	@DisplayName("If the Redis entry is still present should return 403 FORBIDDEN")
	@Order(2)
	void WhenLockTimeoutIsNotOver_ShoulddReturn403Forbidden() {
		// Arrange
		user.setAccountStatus(AccountStatus.TEMPORARILY_BLOCKED);
		userRepository.save(user);
		userRepository.flush();
		
    	String redisKey = Constants.LOGIN_ATTEMPTS_REDIS_KEY + username;
		redisTemplate.opsForValue().set(redisKey, "3", 5, TimeUnit.MINUTES);
		
		HttpEntity<String> request = setHttpRequest();
		
		// Act
		ResponseEntity<StandardResponse> response = testRestTemplate
				.postForEntity("/login", request, StandardResponse.class);
				
		// Assert
		assertAll(
				() -> assertEquals(AccountStatus.TEMPORARILY_BLOCKED, userAccountService.findUser(username).getAccountStatus(),
						"User account status shoud be TEMPORARILY_BLOCKED"),
				() -> assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(),
						"HTTP response status should be 403 FORBIDDEN"),
				() -> assertEquals("Account is locked", response.getBody().getMessage(),
						"Unexpected HTTP response message")
		);
	}
	
	@Test
	@DisplayName("If request has no username/password, it returns 400 BAD CREDENTIALS")
	void WhenNoUsernameOrPassword_ShouldThrowException() {
		// Arrange
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		
		String body = "username=Yorch22"; 
		HttpEntity<String> request = new HttpEntity<>(body, headers);
		
		// Act
		ResponseEntity<StandardResponse> response = testRestTemplate
				.postForEntity("/login", request, StandardResponse.class);
		
		// Assert
		assertAll(
				() -> assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(),
						"HTTP response status should be 400 BAD_REQUEST"),
				() -> assertEquals("Username and password are required", response.getBody().getMessage(),
						"Unexpected HTTP response message")
		);
	}
	
	private HttpEntity<String> setHttpRequest() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);		
		String body = "username=" + username + "&password=" + password;
		return new HttpEntity<>(body, headers);
	}
}