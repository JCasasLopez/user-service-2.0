package dev.jcasaslopez.user.controller;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import dev.jcasaslopez.user.repository.UserRepository;
import dev.jcasaslopez.user.service.EmailService;
import dev.jcasaslopez.user.service.TokenService;
import dev.jcasaslopez.user.testhelper.AuthenticationTestHelper;
import dev.jcasaslopez.user.testhelper.TestHelper;
import dev.jcasaslopez.user.testhelper.UserTestBuilder;
import dev.jcasaslopez.user.utilities.Constants;

// These tests verify exclusively the happy path of the reset and change password flow. 
// Scenarios related to security are tested separately in AuthenticationFilterTest and EndpointsSecurityTest.
// Those concerning the internal logic of the password change mechanism, such as validating 
// the new password format and ensuring it differs from the old one, are covered in PasswordServiceTest.

// @AutoConfigureMockMvc is needed because AuthenticationTestHelper requires MockMvc bean,
// which is not available by default in @SpringBootTest with RANDOM_PORT configuration.

@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ResetAndChangePasswordE2ETest {
	
	@Autowired private TestRestTemplate testRestTemplate;
	@Autowired private TokenService tokenService;
	@Autowired private TestHelper testHelper;
	@Autowired private AuthenticationTestHelper authTestHelper;
	@Autowired private UserRepository userRepository;
	@Autowired private PasswordEncoder passwordEncoder;
	@MockBean private EmailService emailService;
	
	private User user;
	
	// Immutable test constants defining the input data.
	private static final String USERNAME = "Yorch22";
	private static final String ORIGINAL_PASSWORD = "Password123!";
	
	@BeforeEach
	void setup() {
		UserTestBuilder builder = new UserTestBuilder(USERNAME, ORIGINAL_PASSWORD);
		user = testHelper.createAndPersistUser(builder);
	}
	
	@AfterEach
	void cleanAfterTest() {
		testHelper.cleanDataBaseAndRedis();
	}
	
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
	    verify(emailService, times(1)).sendEmail(anyString(), anyString(), anyString());	    
		assertAll(
				() -> assertEquals(HttpStatus.OK, response.getStatusCode(), "Expected HTTP status 200 OK"),
				() -> assertNotNull(response.getBody(), "Response body should not be null"),
			    () -> assertEquals("Token created successfully and sent to the user to reset password", response.getBody().getMessage(), "Unexpected response message")
				);
	}
	
	@Test
	@DisplayName("Resets password correctly")
	public void resetPassword_whenValid_ShouldReturn200AndResetPassword() {
		// Arrange
		String newPassword = "Password456!";
		String verificationToken = tokenService.createVerificationToken(USERNAME);
		
		HttpHeaders headers = new HttpHeaders();
	    headers.setContentType(MediaType.APPLICATION_JSON);
	    
	    // Using a newly created verification token (any valid token is acceptable).
		headers.setBearerAuth(verificationToken);
	    headers.setAccept(List.of(MediaType.APPLICATION_JSON));
	    
	    HttpEntity<String> request = new HttpEntity<>(newPassword, headers);
	    
	    // Act
	    ResponseEntity<StandardResponse> response = testRestTemplate.exchange(Constants.RESET_PASSWORD_PATH, HttpMethod.PUT, request, StandardResponse.class);
	    // Refresh the 'user' variable with the persisted user from the database, which now has the updated password.
	    user = userRepository.findByUsername(USERNAME).get();
	    
	    // Assert
		assertAll(
				() -> assertEquals(HttpStatus.OK, response.getStatusCode(), "Expected HTTP status to be 200 OK"),
				() -> assertNotNull(response.getBody(), "Response body should not be null"),
				() -> assertEquals("Password reset successfully", response.getBody().getMessage(), "Unexpected response message"),
				() -> assertTrue(passwordEncoder.matches(newPassword, user.getPassword()), "Passwords should match")
				);
	}
	
	@Test
	@DisplayName("Changes password successfully")
	public void changePassword_whenUserLoggedIn_ShouldSendEmailChangePasswordReturn200() {
		// Arrange
		String accessToken = authTestHelper.logInWithTestRestTemplate(USERNAME, ORIGINAL_PASSWORD).getAccessToken();
		String newPassword = "Password456!";
		// Create request body matching the API endpoint expected structure.
		Map<String, String> passwords = new HashMap<>();
		passwords.put("oldPassword", ORIGINAL_PASSWORD);
		passwords.put("newPassword", newPassword);
		
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(accessToken);
	    headers.setContentType(MediaType.APPLICATION_JSON); 
	    HttpEntity<Map<String, String>> request = new HttpEntity<>(passwords, headers);

	    // Act
	    ResponseEntity<StandardResponse> response = testRestTemplate.exchange(Constants.CHANGE_PASSWORD_PATH, HttpMethod.PUT, request, StandardResponse.class);
	    // Refresh the 'user' variable with the persisted user from the database, which now has the updated password.
	    user = userRepository.findByUsername(USERNAME).get();

		// Assert
	    verify(emailService, times(1)).sendEmail(anyString(), anyString(), anyString());	    
	    assertAll(
		        () -> assertEquals(HttpStatus.OK, response.getStatusCode(), "Expected status 200 OK"),
		        () -> assertNotNull(response.getBody(), "Response body should not be null"),
		        () -> assertEquals("Password changed successfully", response.getBody().getMessage(), "Unexpected response message"),
		        () -> assertTrue(passwordEncoder.matches(newPassword, user.getPassword()), "Passwords should match")
		    );
	}

}
