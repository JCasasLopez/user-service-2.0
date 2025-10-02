package dev.jcasaslopez.user.controller;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import dev.jcasaslopez.user.dto.StandardResponse;
import dev.jcasaslopez.user.entity.User;
import dev.jcasaslopez.user.enums.TokenType;
import dev.jcasaslopez.user.repository.UserRepository;
import dev.jcasaslopez.user.service.EmailService;
import dev.jcasaslopez.user.service.TokenService;
import dev.jcasaslopez.user.testhelper.AuthenticationTestHelper;
import dev.jcasaslopez.user.testhelper.TestHelper;
import dev.jcasaslopez.user.testhelper.UserTestBuilder;
import dev.jcasaslopez.user.utilities.Constants;
import io.jsonwebtoken.Claims;

// Scenarios related to token validity, expiration, or signature are tested separately in AuthenticationFilterTest.
// Those concerning the internal logic of the password change mechanism, such as validating 
// the new password format and ensuring it differs from the old one, are covered in PasswordServiceTest.
//
// This class contains a full integration test that covers the lifecycle of a password:
// 1) The reset process is correctly initiated by sending an email with a verification token.
// 2) Password reset: the new password is persisted in the database.
// 3) Failed password change: if the user is not authenticated, a 401 error is returned.
// 4) Successful password change: an authenticated user can change their password, and it is updated in the database.

// @AutoConfigureMockMvc is needed because AuthenticationTestHelper requires MockMvc bean,
// which is not available by default in @SpringBootTest with RANDOM_PORT configuration.

@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FullResetAndChangePasswordIntegrationTest {
	
	@Autowired private TestRestTemplate testRestTemplate;
	@Autowired private TokenService tokenService;
	@Autowired private TestHelper testHelper;
	@Autowired private AuthenticationTestHelper authTestHelper;
	@Autowired private UserRepository userRepository;
	@Autowired private PasswordEncoder passwordEncoder;
	@MockBean private EmailService emailService;
	
	// Static variables to share state (token, created user) between the ordered test methods.
	private static String token;
	private static User user;
	
	// Immutable test constants defining the input data.
	private static final String USERNAME = "Yorch22";
	private static final String ORIGINAL_PASSWORD = "Password123!";
	private static final String PASSWORD_AFTER_RESET = "Password456!";
	private static final String PASSWORD_AFTER_CHANGE = "Password789!";

	
	@BeforeAll
	void setup() {
		UserTestBuilder builder = new UserTestBuilder(USERNAME, ORIGINAL_PASSWORD);
		user = testHelper.createAndPersistUser(builder);
	}
	
	@AfterAll
	void cleanAfterTest() {
		testHelper.cleanDataBaseAndRedis();
	}
	
	@Order(1)
	@Test
	@DisplayName("Reset password process initiates correctly")
	public void forgotPassword_whenValidDetails_ShouldSendEmailAndReturn200() {
		// Arrange
		HttpHeaders headers = new HttpHeaders();
	    headers.setContentType(MediaType.APPLICATION_JSON);
	    headers.setAccept(List.of(MediaType.APPLICATION_JSON));
	    
	    String body = user.getEmail();
	    HttpEntity<String> request = new HttpEntity<>(body, headers);
	    	    
	    // Act
	    ResponseEntity<StandardResponse> response = testRestTemplate.postForEntity(Constants.FORGOT_PASSWORD_PATH, request, StandardResponse.class);

		/// Assert
		// The endpoint has called the method sendEmail() that we have mocked. One of the parameters is the email body, which 
		// we intercept, extracting the verification token and validating it. 
		token = testHelper.extractTokenFromEmail();
		Optional<Claims> optionalClaims = tokenService.getValidClaims(token);
		boolean isTokenValid = optionalClaims.isPresent();
		assertTrue(isTokenValid);
		
		assertAll(
				() -> assertEquals(HttpStatus.OK, response.getStatusCode(), "Expected HTTP status 200 OK"),
				() -> assertNotNull(response.getBody(), "Response body should not be null"),
			    () -> assertEquals("Token created successfully and sent to the user to reset password", response.getBody().getMessage(), "Unexpected response message")
				);
	}
	
	@Order(2)
	@Test
	@DisplayName("Resets password correctly")
	public void resetPassword_whenValid_ShouldReturn200AndResetPassword() {
		// Arrange
		HttpHeaders headers = new HttpHeaders();
	    headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setBearerAuth(token);
	    headers.setAccept(List.of(MediaType.APPLICATION_JSON));
	    HttpEntity<String> request = new HttpEntity<>(PASSWORD_AFTER_RESET, headers);
	    
	    // Act
	    ResponseEntity<StandardResponse> response = testRestTemplate.exchange(Constants.RESET_PASSWORD_PATH, HttpMethod.PUT, request, StandardResponse.class);
	    
	    // Refresh the 'user' variable with the persisted user from the database, which now has the updated password.
	    user = userRepository.findByUsername(USERNAME).get();
	    
	    // Assert
		assertAll(
				() -> assertEquals(HttpStatus.OK, response.getStatusCode(), "Expected HTTP status to be 200 OK"),
				() -> assertNotNull(response.getBody(), "Response body should not be null"),
				() -> assertEquals("Password reset successfully", response.getBody().getMessage(), "Unexpected response message"),
				() -> assertTrue(passwordEncoder.matches(PASSWORD_AFTER_RESET, user.getPassword()), "Passwords should match")
				);
	}
	
	@Order(3)
	@Test
	@DisplayName("Fails to change password when user is not authenticated")
	public void changePassword_whenNoUserAuthenticated_ShouldReturn401() {
		// Arrange
		HttpHeaders headers = new HttpHeaders();
		
	    // No authentication token in the header = no authenticated user.
	    HttpEntity<Void> request = new HttpEntity<>(headers);

	    // Act
	    ResponseEntity<StandardResponse> response = testRestTemplate.exchange(Constants.CHANGE_PASSWORD_PATH, HttpMethod.PUT, request, StandardResponse.class);

		// Assert
	    assertAll(
		        () -> assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode(), "Expected 401 Unauthorized when no token is provided"),
		        () -> assertNotNull(response.getBody(), "Response body should not be null"),
		        () -> assertEquals("Access denied: invalid or missing token", response.getBody().getMessage(), "Unexpected response message")
		    );
	}
	
	@Order(4)
	@Test
	@DisplayName("Changes password successfully")
	public void changePassword_whenUserLoggedIn_ShouldSendEmailChangePasswordReturn200() {
		// Arrange
		String accessToken = authTestHelper.logInWithTestRestTemplate(USERNAME, PASSWORD_AFTER_RESET).getAccessToken();
		
		Map<String, String> passwords = new HashMap<>();
		passwords.put("oldPassword", PASSWORD_AFTER_RESET);
		passwords.put("newPassword", PASSWORD_AFTER_CHANGE);
		
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(accessToken);
	    headers.setContentType(MediaType.APPLICATION_JSON); 
	    HttpEntity<Map<String, String>> request = new HttpEntity<>(passwords, headers);

	    // Act
	    ResponseEntity<StandardResponse> response = testRestTemplate.exchange(Constants.CHANGE_PASSWORD_PATH, HttpMethod.PUT, request, StandardResponse.class);
	    user = userRepository.findByUsername(USERNAME).get();

		// Assert
	    ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
		verify(emailService).sendEmail(anyString(), anyString(), bodyCaptor.capture());
		
	    assertAll(
		        () -> assertEquals(HttpStatus.OK, response.getStatusCode(), "Expected status 200 OK"),
		        () -> assertNotNull(response.getBody(), "Response body should not be null"),
		        () -> assertEquals("Password changed successfully", response.getBody().getMessage(), "Unexpected response message"),
		        () -> assertTrue(passwordEncoder.matches(PASSWORD_AFTER_CHANGE, user.getPassword()), "Passwords should match")
		    );
	}
}