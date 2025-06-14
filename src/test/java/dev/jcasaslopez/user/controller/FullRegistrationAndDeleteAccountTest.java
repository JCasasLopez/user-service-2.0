package dev.jcasaslopez.user.controller;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import dev.jcasaslopez.user.dto.StandardResponse;
import dev.jcasaslopez.user.dto.UserDto;
import dev.jcasaslopez.user.entity.User;
import dev.jcasaslopez.user.enums.AccountStatus;
import dev.jcasaslopez.user.enums.RoleName;
import dev.jcasaslopez.user.enums.TokenType;
import dev.jcasaslopez.user.repository.UserRepository;
import dev.jcasaslopez.user.service.EmailService;
import dev.jcasaslopez.user.service.TokenServiceImpl;
import dev.jcasaslopez.user.testhelper.TestHelper;
import dev.jcasaslopez.user.utilities.Constants;
import io.jsonwebtoken.Claims;

// These tests exclusively verify the happy path of the account creation flow.
// Scenarios related to token validity, expiration, or signature are tested
// separately in AuthenticationFilterTest.
// Validation of unique fields (username, email, etc.) is covered in the entity tests,
// specifically in UniquenessUserFieldsTest.
//
// This class contains a full integration test covering the user account lifecycle:
// 1) Registration: initiates successfully and stores the entry in Redis.
// 2) Account creation: the account is persisted in the database with correct data.
// 3) Failed deletion: unauthenticated users receive a 401 error when trying to delete.
// 4) Successful deletion: an authenticated user can delete their account, and it's removed from the database.

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FullRegistrationAndDeleteAccountTest {
	
	@Autowired private TestRestTemplate testRestTemplate;
	@Autowired private TokenServiceImpl tokenServiceImpl;
	@Autowired private RedisTemplate<String, String> redisTemplate;
	@Autowired private UserRepository userRepository;
	@Autowired private PasswordEncoder passwordEncoder;
	@Autowired private ObjectMapper mapper;
	@Autowired private TestHelper testHelper;
	@MockBean private EmailService emailService;

	private static String token;
	private static User user;
	private static String userJson;
	private static final String username = "Yorch22";
	private static final String password = "Jorge22!";
	
	@BeforeAll
	void setup() throws JsonProcessingException {
		
		// Jackson cannot serialize or deserialize java.time.LocalDate by default.
		// You need to register the JavaTimeModule with the ObjectMapper.
		mapper.registerModule(new JavaTimeModule());
		
		user = testHelper.createUser(username, password);
		
		// createUser() returns the user with the password encoded, but 'userJson'
		// expects the password in plain text (since it gets encoded again in 
		// AccountOrchestrationService.initiateRegistration())
		user.setPassword(password);
		userJson = mapper.writeValueAsString(user);
	}
	
	@AfterAll
	void cleanDatabase() {
		testHelper.cleanDataBaseAndRedis();
	}
	
	// This integration test verifies:
	// - That a 200 OK response is returned.
	// - That the token is included in the email body and is valid.
	// - That the username extracted from the Redis entry match the input.
	// - That an email is sent to the user (only the message content is verified).
	@Order(1)
	@Test
	@DisplayName("Registration process initiates correctly")
	public void initiateRegistration_whenValidDetails_ShouldUploadRedisEntryAndReturn200()
			throws JSONException, JsonProcessingException {
		
		// Arrange
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> request = new HttpEntity<>(userJson, headers);

		// Act
		ResponseEntity<StandardResponse> response = testRestTemplate
				.postForEntity(Constants.INITIATE_REGISTRATION_PATH, request, StandardResponse.class);

		// Assert

		// We extract the token from the email. Verify the token in the message body and the email is sent.
		ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
		verify(emailService).sendEmail(anyString(), anyString(), bodyCaptor.capture());
		String emailBody = bodyCaptor.getValue();
		Pattern pattern = Pattern.compile("token=([\\w-]+\\.[\\w-]+\\.[\\w-]+)");
		Matcher matcher = pattern.matcher(emailBody);
		assertTrue(matcher.find(), "Token not found in email body");
		token = matcher.group(1);

		// We build the key for the Redis entry and verify the token is valid.
		String tokenJti = tokenServiceImpl.getJtiFromToken(token);
		Optional<Claims> optionalClaims = tokenServiceImpl.getValidClaims(token);
		assertTrue(optionalClaims.isPresent());
		String redisKey = Constants.CREATE_ACCOUNT_REDIS_KEY + tokenJti;

		// If the user is correct, we are verifying indirectly that the Redis entry is correct also.
		String storedUserAsJson = redisTemplate.opsForValue().get(redisKey);
		assertNotNull(storedUserAsJson, "Redis entry not found for token");
		UserDto storedUser = mapper.readValue(storedUserAsJson, UserDto.class);

		assertAll(
				() -> assertEquals(HttpStatus.OK, response.getStatusCode(), 
						"Expected HTTP status 200 OK"),
				() -> assertNotNull(response.getBody(), "Response body should not be null"),
			    () -> assertEquals("Token created successfully and sent to the user to verify email",
			    		response.getBody().getMessage(), "Unexpected response message"),
				() -> assertEquals(username, storedUser.getUsername(), "Username does not match")
				);
	}
	
	@Order(2)
	@Test
	@DisplayName("Creates account correctly")
	public void createAccount_whenValidDetails_ShouldWorkCorrectly() throws JsonProcessingException {
		
		// Arrange
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(token);
		HttpEntity<Void> request = new HttpEntity<>(headers); 
	
		// Act
		ResponseEntity<StandardResponse> response = testRestTemplate
		        .postForEntity(Constants.REGISTRATION_PATH, request, StandardResponse.class);
		Optional<User> optionalUserJPA = userRepository.findByUsername(username);
		
		// Assert
		assertAll(
			    () -> assertEquals(HttpStatus.CREATED, response.getStatusCode(), 
			    		"Expected HTTP status to be 201 CREATED"),
			    () -> assertNotNull(response.getBody(), 
			    		"Response body should not be null"),
			    () -> assertEquals("Account created successfully", response.getBody().getMessage(), 
			    		"Unexpected response message"),
			    () -> assertTrue(optionalUserJPA.isPresent(), 
			    		"Expected user to be present in the database"),
			    () -> assertEquals(username, optionalUserJPA.get().getUsername(), 
			    		"Username does not match"),
			    () -> assertEquals(user.getFullName(), optionalUserJPA.get().getFullName(), 
			    		"Full name does not match"),
			    () -> assertEquals(user.getEmail(), optionalUserJPA.get().getEmail(), 
			    		"Email does not match"),
			    () -> assertEquals(user.getDateOfBirth(), optionalUserJPA.get().getDateOfBirth(), 
			    		"Birth date does not match"),
			    () -> assertTrue(passwordEncoder.matches("Jorge22!", optionalUserJPA.get().getPassword()), 
			    		"Password was not encoded or does not match")
			);
	}
	
	@Order(3)
	@Test
	@DisplayName("Fails to delete account when user is not authenticated")
	public void deleteAccount_whenNoUserAuthenticated_ShouldReturn401() {
	    // Arrange
	    HttpHeaders headers = new HttpHeaders();
	   	    
	    // No authentication token in the header = no authenticated user.
	    HttpEntity<Void> request = new HttpEntity<>(headers);

	    // Act
	    ResponseEntity<StandardResponse> response = testRestTemplate.exchange(
	            "/deleteAccount", HttpMethod.DELETE, request, StandardResponse.class);

	    // Assert
	    assertAll(
	        () -> assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode(), 
	                "Expected 401 Unauthorized when no token is provided"),
	        () -> assertNotNull(response.getBody(), "Response body should not be null"),
	        () -> assertEquals("Access denied: invalid or missing token", 
	                response.getBody().getMessage(), "Unexpected response message")
	    );
	}

	@Order(4)
	@Test
	@DisplayName("Deletes account successfully")
	public void deleteAccount_whenUserLoggedIn_ShouldDeleteAccount() {
		// Arrange
		String accessToken = testHelper.loginUser(user, TokenType.ACCESS);
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(accessToken);
		HttpEntity<Void> request = new HttpEntity<>(headers); 
		
		// Act
		ResponseEntity<StandardResponse> response = testRestTemplate.exchange(
			    "/deleteAccount", HttpMethod.DELETE, request, StandardResponse.class);

		// Assert
		assertAll(
			    () -> assertEquals(HttpStatus.OK, response.getStatusCode(), 
			    		"Expected status 200 OK"),
			    () -> assertNotNull(response.getBody(), 
			    		"Response body should not be null"),
			    () -> assertEquals("Account deleted successfully", response.getBody().getMessage(), 
			    		"Unexpected response message"),
			    () -> assertTrue(userRepository.findByUsername(user.getUsername()).isEmpty(), 
			    		"User should no longer exist")
			);
	}
}