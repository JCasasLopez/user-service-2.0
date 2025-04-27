package dev.jcasaslopez.user.controller;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import dev.jcasaslopez.user.dto.StandardResponse;
import dev.jcasaslopez.user.dto.UserDto;
import dev.jcasaslopez.user.entity.User;
import dev.jcasaslopez.user.enums.TokenType;
import dev.jcasaslopez.user.mapper.UserMapper;
import dev.jcasaslopez.user.model.TokensLifetimes;
import dev.jcasaslopez.user.repository.UserRepository;
import dev.jcasaslopez.user.security.CustomUserDetails;
import dev.jcasaslopez.user.service.EmailService;
import dev.jcasaslopez.user.service.TokenServiceImpl;
import dev.jcasaslopez.user.service.UserDetailsManagerImpl;
import dev.jcasaslopez.user.utilities.Constants;
import dev.jcasaslopez.user.utilities.TestHelper;
import io.jsonwebtoken.Claims;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

// Estos tests verifican exclusivamente el happy path del flujo de creación de cuenta.
// Los escenarios relacionados con la validez, expiración o firma del token
// se testean de forma aislada en AuthenticationFilterTest.
// La validación de campos únicos (username, email, etc.) se cubre en los tests de entidad,
// específicamente en UniquenessUserFieldsTest.
//
// Esta clase contiene un test de integración completo que cubre el ciclo de vida de una cuenta de usuario:
// 1) Registro: se inicia correctamente y se almacena la entrada en Redis.
// 2) Creación de cuenta: la cuenta se persiste en la base de datos con los datos correctos.
// 3) Eliminación fallida: si no hay usuario autenticado, se devuelve un error 401.
// 4) Eliminación exitosa: un usuario autenticado puede eliminar su cuenta y se borra de la base de datos.
//
//
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

@Transactional
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles("prod")
public class FullRegistrationAndDeleteAccountTest {
	
	@Autowired TestRestTemplate testRestTemplate;
	@Autowired TokenServiceImpl tokenServiceImpl;
	@Autowired RedisTemplate<String, String> redisTemplate;
	@Autowired TokensLifetimes tokensLifetimes;
	@Autowired UserRepository userRepository;
	@Autowired PasswordEncoder passwordEncoder;
	@Autowired ObjectMapper mapper;
	@Autowired UserMapper userMapper;
	@Autowired EntityManager entityManager;
	@Autowired TestHelper testHelper;
	@MockBean private EmailService emailService;

	private static String token;
	private static User user;
	private static String userJson;
	private static String username;
	
	@BeforeAll
	void setup() throws JsonProcessingException {
		// Jackson no puede serializar o deserializar java.time.LocalDate por defecto.
		// Debes registrar el módulo JavaTimeModule en el ObjectMapper.
		//
		// Jackson cannot serialize or deserialize java.time.LocalDate by default.
		// You need to register the JavaTimeModule with the ObjectMapper.
		mapper.registerModule(new JavaTimeModule());
		
		username = "Yorch123";
		user = new User(username, "Jorge22!", "Jorge García", "jorgecasas22@hotmail.com", LocalDate.of(1978, 11, 26));
		userJson = mapper.writeValueAsString(user);
	}
	
	// El método que estamos sometiendo a test:
	// - Crea un token de verificación.
	// - Se crea una entrada en Redis con: 
	//       CLAVE: create_account:f8a65da7-e8c9-41d2-a5f3-b8d1a2e7c0b9 (JTI del token). 
	//       VALOR: usuario como JSON.
	// - Mediante la publicación de un evento, manda un correo al usuario con el token.
	//
	// Este test de integración verifica que:
	// - Se devuelve un 200 OK.
	// - El token está presente en el mensaje del email y es válido.
	// - Los campos "username" y "email" extraídos del valor de la entrada de Redis, coinciden con los
	//   del usuario. La contraseña también coincide y ha sido codificada correctamente.
	// - Se manda un email al usuario (aunque solo se verifica el mensaje).
	//
	// The method under test performs the following:
	// - Creates a verification token.
	// - Stores a Redis entry with:
	//     KEY: create_account:f8a65da7-e8c9-41d2-a5f3-b8d1a2e7c0b9 (token JTI). 
	//	   VALUE: user data as JSON.
	// - Sends a verification email to the user by publishing an event.
	//
	// This integration test verifies:
	// - That a 200 OK response is returned.
	// - That the token is included in the email body and is valid.
	// - That the username and email extracted from the Redis entry match the input.
	//   Also verifies that the password was properly encoded.
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

		// Extraemos el token del email. Verificamos que esté presente y que se envía el
		// email.
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

		// Construimos la clave de la entrada de Redis y verificamos que el token sea
		// válido.
		//
		// We build the key for the Redis entry and verify the token is valid.
		String tokenJti = tokenServiceImpl.getJtiFromToken(token);
		Optional<Claims> optionalClaims = tokenServiceImpl.getValidClaims(token);
		assertTrue(optionalClaims.isPresent());
		String redisKey = Constants.CREATE_ACCOUNT_REDIS_KEY + tokenJti;

		// Si encontramos al usuario correcto, estamos verificando indirectamente que la
		// clave de Redis también lo es.
		//
		// If the user is correct, we are verifying indirectly that the Redis entry is
		// also.
		String storedUserAsJson = redisTemplate.opsForValue().get(redisKey);
		assertNotNull(storedUserAsJson, "Redis entry not found for token");
		UserDto storedUser = mapper.readValue(storedUserAsJson, UserDto.class);

		assertAll(
				() -> assertEquals(HttpStatus.OK, response.getStatusCode(), 
						"Expected HTTP status 200 OK"),
				() -> assertNotNull(response.getBody(), "Response body should not be null"),
			    () -> assertEquals("Token created successfully and sent to the user to verify email",
			    		response.getBody().getMessage(), "Unexpected response message"),
				() -> assertEquals(username, storedUser.getUsername(), "Username does not match"),
				() -> assertEquals(user.getEmail(), storedUser.getEmail(), "Email does not match"),
				() -> assertTrue(passwordEncoder.matches(user.getPassword(), storedUser.getPassword()),
						"Encoded password does not match")
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
	   	    
	    // Sin token de autenticación en el encabezado = no hay usuario autenticado.
	    //
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
		String accessToken = testHelper.logUserIn(user);
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