package dev.jcasaslopez.user.controller;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
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
import dev.jcasaslopez.user.enums.TokenType;
import dev.jcasaslopez.user.model.TokensLifetimes;
import dev.jcasaslopez.user.repository.UserRepository;
import dev.jcasaslopez.user.service.TokenServiceImpl;
import dev.jcasaslopez.user.utilities.Constants;
import jakarta.transaction.Transactional;

// Este test verifica exclusivamente el happy path del flujo de creación de cuenta.
// Los escenarios relacionados con la validez, expiración o firma del token
// se testean de forma aislada en AuthenticationFilterTest.
// La validación de campos únicos (username, email, etc.) se cubre en los tests de entidad,
// específicamente en UniquenessUserFieldsTest.
//
// This test exclusively verifies the happy path of the account creation flow.
// Scenarios related to token validity, expiration, or signature are tested
// separately in AuthenticationFilterTest.
// Validation of unique fields (username, email, etc.) is covered in the entity tests,
// specifically in UniquenessUserFieldsTest.

@Transactional
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CreateAccountIntegrationTest {
	
	@Autowired TestRestTemplate testRestTemplate;
	@Autowired TokenServiceImpl tokenServiceImpl;
	@Autowired RedisTemplate<String, String> redisTemplate;
	@Autowired TokensLifetimes tokensLifetimes;
	@Autowired UserRepository userRepository;
	@Autowired PasswordEncoder passwordEncoder;
	
	// Eliminamos el usuario después de cada test para evitar errores por campos únicos,
	// ya que el 'username' y el 'email' no pueden repetirse en la base de datos.
	//
	// We delete the user after each test to avoid errors due to unique constraints,
	// since 'username' and 'email' must not be duplicated in the database.
	@AfterEach
	void tearDown() {
	    userRepository.deleteByUsername("Yorch123");
	}
	
	@Test
	@DisplayName("Creates account correctly")
	public void createAccount_whenValidDetails_ShouldWorkCorrectly() throws JsonProcessingException {
		// Arrange
		String token = tokenServiceImpl.createVerificationToken("Yorch123");
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setAccept(List.of(MediaType.APPLICATION_JSON));
		headers.setBearerAuth(token);
		HttpEntity<Void> request = new HttpEntity<>(headers); 
	
		// Tenemos que subir la entrada correspondiente a Redis, ya que es de donde el método correspondiente de 
		// AccountOrchestrationService toma los datos del usuario para persistirlo. Por tanto,
		// creamos primero el usuario, lo convertimos a JSON y luego creamos la clave de Redis: 
		// Constants.CREATE_ACCOUNT_REDIS_KEY + JTI del token.
		//
		// We need to upload the corresponding entry to Redis, since that's where the relevant method in
		// AccountOrchestrationService retrieves the user data from to persist it. Therefore,
		// we first create the user, convert it to JSON, and then create the Redis key:
		// Constants.CREATE_ACCOUNT_REDIS_KEY + token JTI.
		UserDto user = new UserDto("Yorch123", "Jorge22!", "Jorge García", "jorgecasas22@hotmail.com",
				LocalDate.of(1978, 11, 26));
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		String encodedPassword = passwordEncoder.encode(user.getPassword());
		user.setPassword(encodedPassword);
		String userAsJson = mapper.writeValueAsString(user);
		String tokenJti = tokenServiceImpl.getJtiFromToken(token);
		String redisKey = Constants.CREATE_ACCOUNT_REDIS_KEY + tokenJti;
		int expirationInSeconds = tokensLifetimes.getTokensLifetimes().get(TokenType.VERIFICATION) * 60;
		redisTemplate.opsForValue().set(redisKey, userAsJson, expirationInSeconds, TimeUnit.SECONDS);

		// Act
		ResponseEntity<StandardResponse> response = testRestTemplate
		        .postForEntity(Constants.REGISTRATION_PATH, request, StandardResponse.class);
		
		Optional<User> optionalUserJPA = userRepository.findByUsername("Yorch123");
		
		// Assert
		assertAll(
			    () -> assertEquals(HttpStatus.CREATED, response.getStatusCode(), 
			    		"Expected HTTP status to be 201 CREATED"),
			    () -> assertTrue(optionalUserJPA.isPresent(), 
			    		"Expected user to be present in the database"),
			    () -> assertEquals(user.getUsername(), optionalUserJPA.get().getUsername(), 
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
}