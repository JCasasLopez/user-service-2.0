package dev.jcasaslopez.user.security.handler;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

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

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.jcasaslopez.user.dto.LoginResponse;
import dev.jcasaslopez.user.dto.StandardResponse;
import dev.jcasaslopez.user.entity.LoginAttempt;
import dev.jcasaslopez.user.entity.User;
import dev.jcasaslopez.user.mapper.UserMapper;
import dev.jcasaslopez.user.repository.LoginAttemptRepository;
import dev.jcasaslopez.user.service.TokenService;
import dev.jcasaslopez.user.testhelper.TestHelper;
import dev.jcasaslopez.user.utilities.Constants;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CustomAuthenticationSuccessHandlerTest {
	
	@Autowired private TestRestTemplate testRestTemplate;
	@Autowired private RedisTemplate<String, String> redisTemplate;
	@Autowired private LoginAttemptRepository loginAttemptRepository;
	@Autowired private UserMapper userMapper;
	@Autowired private TokenService tokenService;
	@Autowired private ObjectMapper objectMapper;
	@Autowired private TestHelper testHelper;
	
	private static final String username = "Yorch22";
	private static final String password = "Jorge22!";
	
	// Persistimos un usuario que usaremos posteriormente para el login y simulamos que su
	// cuenta de intentos fallidos en Redis está a 2. Si el handler funciona correctamente,
	// se espera que esta entrada en Redis se elimine (según la lógica de negocio) y que la
	// respuesta HTTP sea correcta. Además, usamos un método que busca intentos de login 
	// entre dos instantes de tiempo para localizar el intento generado durante el test,
	// y verificamos que sea exitoso y que pertenezca al usuario autenticado.
	//
	// We persist a user that will be used later for login and simulate that their failed login 
	// attempt count in Redis is set to 2. If the handler works correctly, this Redis entry 
	// should be deleted (according to the business logic) and the HTTP response should be valid.
	// Additionally, we use a method that searches for login attempts between two points in time 
	// to locate the one generated during the test, and we verify that it was successful and 
	// belongs to the authenticated user.
	@Test
	@DisplayName("If login attempt is successful, Redis entry is deleted and 200 OK returned")
	void CustomAuthenticationSuccessHandler_WhenLoginSuccessful_ShouldReturn200OkAndDeleteRedisEntry() {
		// Arrange
		User user = testHelper.createUser(username, password);
		String redisKey = Constants.LOGIN_ATTEMPTS_REDIS_KEY + username;
		redisTemplate.delete(redisKey);
		LocalDateTime startTest = LocalDateTime.now().minusSeconds(1);;
		
		// Simulamos 2 intentos fallidos previos para el usuario almacenando la clave 
		// 'redisKey' con valor 2 en Redis
		//
		// Simulate 2 previous failed login attempts by setting the 'redisKey' entry 
		// to 2 in Redis
		redisTemplate.opsForValue().set(redisKey, "2", 5, TimeUnit.MINUTES);
		HttpEntity<String> request = configHttpRequest();
		
		// Act
		ResponseEntity<StandardResponse> response = testRestTemplate
				.postForEntity("/login", request, StandardResponse.class);
		
		LocalDateTime endTest = LocalDateTime.now().plusSeconds(1);;
		List<LoginAttempt> loginAttemptsDuringTestExecution = loginAttemptRepository.findAll();		
		
		// Buscamos el intento de login entre 'startTest' y 'endTest'
		//
		// We search for loginAttempts between 'startTest' and 'endTest'
		LoginAttempt matchingAttempt = loginAttemptsDuringTestExecution.stream()
			    .filter(a -> a.getTimestamp().isAfter(startTest) && a.getTimestamp().isBefore(endTest))
			    .findFirst()
			    .orElseThrow(() -> new AssertionError("No login attempt found for that time period"));
		
		// Assert
		assertAll(
		    () -> assertNull(redisTemplate.opsForValue().get(redisKey), 
		            "Redis entry for that should have been deleted"),
		    () -> assertEquals(HttpStatus.OK, response.getStatusCode(),
		            "Response HTTP status should be 200 OK"),
		    () -> assertEquals("Login attempt successful", response.getBody().getMessage(),
		            "Unexpected response message"),
		    () -> {
		    	LoginResponse loginResponse = objectMapper.convertValue(
		    		    response.getBody().getDetails(),
		    		    LoginResponse.class
		    		);
		        assertNotNull(loginResponse, "Response should contain details (user & tokens)");
		        assertAll(
		            () -> assertEquals(userMapper.userToUserDtoMapper(user).getIdUser(), 
		                    loginResponse.getUser().getIdUser(),
		                    "Authenticated user and returned user should be the same"),
		            () -> assertDoesNotThrow(() -> tokenService.getValidClaims(loginResponse.getRefreshToken()),
		                    "Returned refresh token should be a valid token"),
		            () -> assertDoesNotThrow(() -> tokenService.getValidClaims(loginResponse.getAccessToken()),
		                    "Returned access token should be a valid token")
		        );
		    },
		    () -> assertEquals(user.getIdUser(), matchingAttempt.getUser().getIdUser(),
		            "User's ID in persisted login attempt should be user's ID"),
		    () -> assertTrue(matchingAttempt.isSuccessful(),
		            "Persisted login attempt should be successful")
		);
		
		// Cleanup
	    testHelper.cleanDataBaseAndRedis();
	}
	
	private HttpEntity<String> configHttpRequest(){
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);		
		String body = "username=" + username + "&password=" + password;
		return new HttpEntity<>(body, headers);
	}
}