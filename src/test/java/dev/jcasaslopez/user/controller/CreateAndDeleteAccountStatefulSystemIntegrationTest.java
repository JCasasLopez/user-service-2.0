package dev.jcasaslopez.user.controller;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
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

import dev.jcasaslopez.user.dto.StandardResponse;
import dev.jcasaslopez.user.dto.UserDto;
import dev.jcasaslopez.user.entity.User;
import dev.jcasaslopez.user.repository.UserRepository;
import dev.jcasaslopez.user.service.EmailService;
import dev.jcasaslopez.user.service.TokenServiceImpl;
import dev.jcasaslopez.user.testhelper.AuthenticationTestHelper;
import dev.jcasaslopez.user.testhelper.TestHelper;
import dev.jcasaslopez.user.testhelper.UserTestBuilder;
import dev.jcasaslopez.user.utilities.Constants;
import io.jsonwebtoken.Claims;

// These tests verify exclusively the happy path of the account creation and deletion flow. 
// Scenarios related to security are tested separately in AuthenticationFilterTest and EndpointsSecurityTest.
// Validation of unique fields is covered in the entity tests, specifically in UniquenessUserFieldsTest.

// This class contains a full integration test covering the user account lifecycle:
// 1) Registration: initiates successfully and stores the entry in Redis.
// 2) Account creation: the account is persisted in the database with correct data.
// 3) Successful deletion: an authenticated user can delete their account, and it's removed from the database.

// IMPORTANT: Initiate registration and create account are inextricably linked because the token and Redis entry
// generated in the first are used in the second. The best way to test them is to preserve the state of the fields 
// 'user' and 'verificationToken' between tests. To this end, @TestInstance(TestInstance.Lifecycle.PER_CLASS) 
// creates a single instance of the test class shared by all tests, which are executed in a specific order 
// (@TestMethodOrder(MethodOrderer.OrderAnnotation.class)).
// The third test (delete account) could be tested independently, but it is included in this stateful approach 
// because it reflects the natural user journey where actions build upon previous steps, and significantly 
// reduces setup complexity and execution time.

// @AutoConfigureMockMvc is needed because AuthenticationTestHelper requires MockMvc bean,
// which is not available by default in @SpringBootTest with RANDOM_PORT configuration.

@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CreateAndDeleteAccountStatefulSystemIntegrationTest {
	
	@Autowired private TestRestTemplate testRestTemplate;
	@Autowired private TokenServiceImpl tokenServiceImpl;
	@Autowired private RedisTemplate<String, String> redisTemplate;
	@Autowired private UserRepository userRepository;
	@Autowired private PasswordEncoder passwordEncoder;
	@Autowired private ObjectMapper mapper;
	@Autowired private TestHelper testHelper;
	@Autowired private AuthenticationTestHelper authTestHelper;
	@MockBean private EmailService emailService;

	// Static variables to share state (token, created user) between the ordered test methods.
	private static String verificationToken;
	private static User user;
	
	// Immutable test constants defining the input data.
	private static final String USERNAME = "Yorch22"; 
	private static final String PASSWORD = "Password123!"; 
	
	@BeforeAll
	void setup() throws JsonProcessingException {
		UserTestBuilder builder = new UserTestBuilder(USERNAME, PASSWORD);
		user = testHelper.createUser(builder);
	}
	
	@AfterAll
	void cleanDatabase() {
		testHelper.cleanDataBaseAndRedis();
	}
	
	// This test and the next one are inherently coupled because of the token needen for verification and the Redis entry
	// that contains the user details. Since this high degree of coupling is inevitable, we might as well verify explicitly 
	// the token and the Redis entry are correct, so that we have as much information as possible if the tests fail. 
	@Order(1)
	@Test
	@DisplayName("Registration process initiates correctly")
	public void initiateRegistration_whenValidDetails_ShouldUploadRedisEntryAndReturn200() throws JsonProcessingException {
		
		// Arrange
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		String userJson = testHelper.returnUserAsJson(user);
		HttpEntity<String> request = new HttpEntity<>(userJson, headers);

		// Act
		ResponseEntity<StandardResponse> response = testRestTemplate.postForEntity(Constants.INITIATE_REGISTRATION_PATH, request, StandardResponse.class);

		// Assert
		// We mock EmailService to extract the token from the email body. This is necessary because we need
		// the exact same token that was issued (due to its unique jti claim) to build the Redis key in subsequent operations.
		verificationToken = testHelper.extractTokenFromEmail();
		
		// Verify the extracted token is valid. If test #2 fails, we need to know if the cause was an invalid
		// token generated here, rather than a problem in the account creation logic itself.
		Optional<Claims> optionalClaims = tokenServiceImpl.getValidClaims(verificationToken);
		boolean isTokenValid = optionalClaims.isPresent();
		assertTrue(isTokenValid);

		// Verify Redis entry exists and contains correct data. While test #2 implicitly validates this
		// (if Redis data were wrong, the persisted user would be incorrect), making this verification explicit
		// provides immediate visibility when something fails. If test #2 fails, we'll know whether the issue
		// originated here (bad Redis data) or in the account creation step itself.
		String redisKey = testHelper.buildRedisKey(verificationToken, Constants.CREATE_ACCOUNT_REDIS_KEY);
		String storedInRedisUserAsJson = redisTemplate.opsForValue().get(redisKey);
		assertNotNull(storedInRedisUserAsJson, "Redis entry not found for this user");
		
		UserDto storedInRedisUser = mapper.readValue(storedInRedisUserAsJson, UserDto.class);
		assertAll(
				() -> assertEquals(HttpStatus.OK, response.getStatusCode(), "Expected HTTP status 200 OK"),
				() -> assertNotNull(response.getBody(), "Response body should not be null"),
			    () -> assertEquals("Token created successfully and sent to the user to verify email", response.getBody().getMessage(), "Unexpected response message"),
				() -> assertEquals(USERNAME, storedInRedisUser.getUsername(), "Username does not match")
				);
	}
	
	@Order(2)
	@Test
	@DisplayName("Creates account correctly")
	public void createAccount_whenValidDetails_ShouldWorkCorrectly() {
		
		// Arrange
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(verificationToken);
		HttpEntity<Void> request = new HttpEntity<>(headers); 
	
		// Act
		ResponseEntity<StandardResponse> response = testRestTemplate.postForEntity(Constants.REGISTRATION_PATH, request, StandardResponse.class);
		Optional<User> optionalUserJPA = userRepository.findByUsername(USERNAME);
		
		// Assert
		assertAll(
			    () -> assertEquals(HttpStatus.CREATED, response.getStatusCode(), "Expected HTTP status to be 201 CREATED"),
			    () -> assertNotNull(response.getBody(), "Response body should not be null"),
			    () -> assertEquals("Account created successfully", response.getBody().getMessage(), "Unexpected response message"),
			    () -> assertTrue(optionalUserJPA.isPresent(), "Expected user to be present in the database"),
			    () -> assertEquals(USERNAME, optionalUserJPA.get().getUsername(), "Username does not match"),
			    () -> assertEquals(user.getFullName(), optionalUserJPA.get().getFullName(), "Full name does not match"),
			    () -> assertEquals(user.getEmail(), optionalUserJPA.get().getEmail(), "Email does not match"),
			    () -> assertEquals(user.getDateOfBirth(), optionalUserJPA.get().getDateOfBirth(), "Birth date does not match"),
			    () -> assertTrue(passwordEncoder.matches(PASSWORD, optionalUserJPA.get().getPassword()), "Password was not encoded or does not match")
			);
	}
	
	@Order(3)
	@Test
	@DisplayName("Deletes account successfully")
	public void deleteAccount_whenUserLoggedIn_ShouldDeleteAccount() {
		// Arrange
		String accessToken = authTestHelper.logInWithTestRestTemplate(USERNAME, PASSWORD).getAccessToken();
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(accessToken);
		HttpEntity<Void> request = new HttpEntity<>(headers); 
		
		// Act
		ResponseEntity<StandardResponse> response = testRestTemplate.exchange(Constants.DELETE_ACCOUNT_PATH, HttpMethod.DELETE, request, StandardResponse.class);

		// Assert
		assertAll(
			    () -> assertEquals(HttpStatus.OK, response.getStatusCode(), "Expected status 200 OK"),
			    () -> assertNotNull(response.getBody(), "Response body should not be null"),
			    () -> assertEquals("Account deleted successfully", response.getBody().getMessage(), "Unexpected response message"),
			    () -> assertTrue(userRepository.findByUsername(user.getUsername()).isEmpty(), "User should no longer exist")
			);
	}
}