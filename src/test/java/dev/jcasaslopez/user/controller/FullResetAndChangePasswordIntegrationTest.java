package dev.jcasaslopez.user.controller;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import dev.jcasaslopez.user.dto.StandardResponse;
import dev.jcasaslopez.user.entity.Role;
import dev.jcasaslopez.user.entity.User;
import dev.jcasaslopez.user.enums.AccountStatus;
import dev.jcasaslopez.user.enums.RoleName;
import dev.jcasaslopez.user.enums.TokenType;
import dev.jcasaslopez.user.mapper.UserMapper;
import dev.jcasaslopez.user.repository.RoleRepository;
import dev.jcasaslopez.user.repository.UserRepository;
import dev.jcasaslopez.user.security.CustomUserDetails;
import dev.jcasaslopez.user.service.EmailService;
import dev.jcasaslopez.user.service.TokenServiceImpl;
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

@Transactional
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FullResetAndChangePasswordIntegrationTest {
	
	@Autowired TestRestTemplate testRestTemplate;
	@Autowired TokenServiceImpl tokenServiceImpl;
	@Autowired UserRepository userRepository;
	@Autowired RoleRepository roleRepository;
	@Autowired PasswordEncoder passwordEncoder;
	@Autowired UserMapper userMapper;
	@MockBean private EmailService emailService;
	
	private static String token;
	private static User user;
	private static String username;
	
	@BeforeAll
	void setup() {
	Set<Role> roles = new HashSet<>();
	Role roleUser = roleRepository.findByRoleName(RoleName.ROLE_USER).get();
	roles.add(roleUser);
	username = "Yorch123";
	
	user = new User(username, 
				"Jorge22!", 
				"Jorge García", 
				"jorgecasas22@hotmail.com", 
				LocalDate.of(1978, 11, 26));
	
	user.setRoles(roles);
	user.setAccountStatus(AccountStatus.ACTIVE);
	userRepository.save(user);
	}
	
	@Order(1)
	@Test
	@DisplayName("Reset password process initiates correctly")
	public void forgotPassword_whenValidDetails_ShouldSendEmailAndReturn200() {
		// Arrange
		HttpHeaders headers = new HttpHeaders();
	    headers.setAccept(List.of(MediaType.APPLICATION_JSON));
	    HttpEntity<Void> request = new HttpEntity<>(headers);
	    String url = Constants.FORGOT_PASSWORD_PATH + "?email=" + user.getEmail();
	    
	    token = tokenServiceImpl.createVerificationToken(username);

	    // Act
	    ResponseEntity<StandardResponse> response = testRestTemplate
	    		.postForEntity(url, request, StandardResponse.class);

		// Assert
	    
		// Extraemos el token del email. Verificamos que esté presente y que se envía el email.
		//
		// We extract the token from the email. Verify the token in the message body and
		// the email is sent.
		ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
		verify(emailService).sendEmail(anyString(), anyString(), bodyCaptor.capture());
		
		String emailBody = bodyCaptor.getValue();
		Pattern pattern = Pattern.compile("token=([\\w-]+\\.[\\w-]+\\.[\\w-]+)");
		Matcher matcher = pattern.matcher(emailBody);
		assertTrue(matcher.find(), "Token not found in email body");
		
		token = matcher.group(1);
		Optional<Claims> optionalClaims = tokenServiceImpl.getValidClaims(token);
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
		String url = Constants.RESET_PASSWORD_PATH + "?newPassword=Garcia22!";
		
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(token);
	    headers.setAccept(List.of(MediaType.APPLICATION_JSON));
	    HttpEntity<Void> request = new HttpEntity<>(headers);
	    
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
		String url = "/changePassword?newPassword=Jorge22!&oldPassword=Garcia22!";
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
		// Primero tenemos que poblar SecurityContextHolder, ya que es un método protegido.
		//
		// First, we need to populate the SecurityContextHolder because the method is protected.
		CustomUserDetails userDetails = userMapper.userToCustomUserDetailsMapper(user);
		Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, userDetails.getPassword(),
				userDetails.getAuthorities());
		SecurityContextHolder.getContext().setAuthentication(authentication);
		String accessToken = tokenServiceImpl.createAuthToken(TokenType.ACCESS);
		
		String url = "/changePassword?newPassword=Jorge22!&oldPassword=Garcia22!";
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(accessToken);
	    HttpEntity<Void> request = new HttpEntity<>(headers);

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
	
	// Por último, borramos el usuario persistido para los tests, para que estos no tengan efectos 
	// en la base de datos.
	// 
	// Finally, we delete the user persisted for testing purposes
	// to avoid leaving any data in the database.
	@Order(5)
	@Test

	// Ejecutar fuera de la transacción para evitar rollback al final del test.
	//
	// Run outside the transaction to avoid rollback at the end of the test.
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	@DisplayName("Cleans up the test user from the database")
	public void cleanupTestUser_shouldRemoveUserFromDatabase() {
	    // Act
	    userRepository.findByUsername(username).ifPresent(userRepository::delete);

	    // Assert
	    assertTrue(userRepository.findByUsername(username).isEmpty(), 
	        "User should no longer exist in the database after cleanup");
	}
}