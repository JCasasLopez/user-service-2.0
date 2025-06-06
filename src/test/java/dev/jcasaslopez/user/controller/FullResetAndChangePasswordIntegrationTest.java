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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import dev.jcasaslopez.user.testhelper.TestHelper;
import dev.jcasaslopez.user.utilities.Constants;
import io.jsonwebtoken.Claims;

// Los escenarios relacionados con la validez, expiración o firma del token
// se testean de forma aislada en AuthenticationFilterTest.
// Los relacionados con el funcionamiento propio del mecanismo de cambio de contraseña, se decir,
// la validación de la nueva contraseña en lo que respecta a su formato y a que sea diferente
// de la antigua, se cubre en los tests de PasswordServiceTest.
// 
// Esta clase contiene un test de integración completo que cubre el ciclo de vida de una contraseña:
// 1) Se inicia correctamente el proceso enviando un email con el token de verificación.
// 2) Reseteo de contraseña: la nueva contraseña se persiste en la base de datos.
// 3) Cambio de contraseña fallida: si no hay usuario autenticado, se devuelve un error 401.
// 4) Cambio de contraseña exitosa: un usuario autenticado puede cambiar su contraseña.
//
//
// Scenarios related to token validity, expiration, or signature
// are tested separately in AuthenticationFilterTest.
// Those concerning the internal logic of the password change mechanism,
// such as validating the new password's format and ensuring it differs from the old one,
// are covered in PasswordServiceTest.
//
// This class contains a full integration test that covers the lifecycle of a password:
// 1) The reset process is correctly initiated by sending an email with a verification token.
// 2) Password reset: the new password is persisted in the database.
// 3) Failed password change: if the user is not authenticated, a 401 error is returned.
// 4) Successful password change: an authenticated user can change their password, and it is updated in the database.

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FullResetAndChangePasswordIntegrationTest {
	
	@Autowired private TestRestTemplate testRestTemplate;
	@Autowired private TokenService tokenService;
	@Autowired private TestHelper testHelper;
	@Autowired private UserRepository userRepository;
	@Autowired private PasswordEncoder passwordEncoder;
	@MockBean private EmailService emailService;
	
	private static String token;
	private static User user;
	private static final String username = "Yorch22";
	private static final String password = "Jorge22!";
	
	@BeforeAll
	void setup() {
		user = testHelper.createUser(username, password);
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
	    
	    String url = Constants.FORGOT_PASSWORD_PATH;
	    
	    token = tokenService.createVerificationToken(username);

	    // Act
	    ResponseEntity<StandardResponse> response = testRestTemplate
	    		.postForEntity(url, request, StandardResponse.class);

		// Assert
		ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
		verify(emailService).sendEmail(anyString(), anyString(), bodyCaptor.capture());
		
		String emailBody = bodyCaptor.getValue();
		Pattern pattern = Pattern.compile("token=([\\w-]+\\.[\\w-]+\\.[\\w-]+)");
		Matcher matcher = pattern.matcher(emailBody);
		assertTrue(matcher.find(), "Token not found in email body");
		
		token = matcher.group(1);
		Optional<Claims> optionalClaims = tokenService.getValidClaims(token);
		assertTrue(optionalClaims.isPresent());
		
		assertAll(
				() -> assertEquals(HttpStatus.OK, response.getStatusCode(), 
						"Expected HTTP status 200 OK"),
				() -> assertNotNull(response.getBody(), "Response body should not be null"),
			    () -> assertEquals("Token created successfully and sent to the user to reset password",
			    		response.getBody().getMessage(), "Unexpected response message")
				);
	}
	
	@Order(2)
	@Test
	@DisplayName("Resets password correctly")
	public void resetPassword_whenValid_ShouldReturn200AndResetPassword() {
		// Arrange
		String url = Constants.RESET_PASSWORD_PATH;
		HttpHeaders headers = new HttpHeaders();
	    headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setBearerAuth(token);
	    headers.setAccept(List.of(MediaType.APPLICATION_JSON));
	    
	    String newPassword = "Garcia22!";
	    HttpEntity<String> request = new HttpEntity<>(newPassword, headers);
	    
	    // Act
	    ResponseEntity<StandardResponse> response = testRestTemplate.
	    		exchange(url, HttpMethod.PUT, request, StandardResponse.class);
	    
	    // Actualizamos la variable 'user' con el usuario persistido en base de datos, 
	    // que ahora contiene la contraseña actualizada.
	    //
	    // We refresh the 'user' variable with the persisted user from the database,
	    // which now has the updated password.
	    user = userRepository.findByUsername(username).get();
	    
	    // Assert
		assertAll(
				() -> assertEquals(HttpStatus.OK, response.getStatusCode(),
						"Expected HTTP status to be 200 OK"),
				() -> assertNotNull(response.getBody(), "Response body should not be null"),
				() -> assertEquals("Password reset successfully", response.getBody().getMessage(),
						"Unexpected response message"),
				() -> assertTrue(passwordEncoder.matches("Garcia22!", user.getPassword()), 
						"Passwords should match")
				);
	}
	
	@Order(3)
	@Test
	@DisplayName("Fails to change password when user is not authenticated")
	public void changePassword_whenNoUserAuthenticated_ShouldReturn401() {
		// Arrange
		String url = "/changePassword";
		HttpHeaders headers = new HttpHeaders();
		
		// Sin token de autenticación en el encabezado = no hay usuario autenticado.
	    //
	    // No authentication token in the header = no authenticated user.
	    HttpEntity<Void> request = new HttpEntity<>(headers);

	    // Act
	    ResponseEntity<StandardResponse> response = testRestTemplate.exchange(
	    		url, HttpMethod.PUT, request, StandardResponse.class);

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
	@DisplayName("Changes password successfully")
	public void changePassword_whenUserLoggedIn_ShouldSendEmailChangePasswordReturn200() {
		// Arrange
		String accessToken = testHelper.loginUser(user, TokenType.ACCESS);
		
		Map<String, String> passwords = new HashMap<>();
		passwords.put("oldPassword", "Garcia22!");
		passwords.put("newPassword", "Jorge22!");
		String url = "/changePassword";
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(accessToken);
	    headers.setContentType(MediaType.APPLICATION_JSON); 
	    HttpEntity<Map<String, String>> request = new HttpEntity<>(passwords, headers);

	    // Act
	    ResponseEntity<StandardResponse> response = testRestTemplate.exchange(
	    		url, HttpMethod.PUT, request, StandardResponse.class);
	    user = userRepository.findByUsername(username).get();

		// Assert
	    ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
		verify(emailService).sendEmail(anyString(), anyString(), bodyCaptor.capture());
		
	    assertAll(
		        () -> assertEquals(HttpStatus.OK, response.getStatusCode(), 
			    		"Expected status 200 OK"),
		        () -> assertNotNull(response.getBody(), "Response body should not be null"),
		        () -> assertEquals("Password changed successfully", response.getBody().getMessage(), 
			    		"Unexpected response message"),
		        () -> assertTrue(passwordEncoder.matches("Jorge22!", user.getPassword()), 
						"Passwords should match")
		    );
	}
}