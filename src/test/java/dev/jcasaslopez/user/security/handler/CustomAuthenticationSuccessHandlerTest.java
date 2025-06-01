package dev.jcasaslopez.user.security.handler;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import dev.jcasaslopez.user.dto.LoginResponse;
import dev.jcasaslopez.user.dto.StandardResponse;
import dev.jcasaslopez.user.entity.LoginAttempt;
import dev.jcasaslopez.user.entity.Role;
import dev.jcasaslopez.user.entity.User;
import dev.jcasaslopez.user.enums.AccountStatus;
import dev.jcasaslopez.user.enums.RoleName;
import dev.jcasaslopez.user.mapper.UserMapper;
import dev.jcasaslopez.user.repository.LoginAttemptRepository;
import dev.jcasaslopez.user.repository.RoleRepository;
import dev.jcasaslopez.user.repository.UserRepository;
import dev.jcasaslopez.user.service.TokenService;
import dev.jcasaslopez.user.utilities.Constants;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class CustomAuthenticationSuccessHandlerTest {
	
	@Autowired private TestRestTemplate testRestTemplate;
	@Autowired private RedisTemplate<String, String> redisTemplate;
	@Autowired private UserRepository userRepository;
	@Autowired private RoleRepository roleRepository;
	@Autowired private LoginAttemptRepository loginAttemptRepository;
	@Autowired private PasswordEncoder passwordEncoder;
	@Autowired private UserMapper userMapper;
	@Autowired private TokenService tokenService;
	
	private static User user;
	private static final String username = "Yorch";
	private static final String password = "Yorch22!";
	private static final String redisKey = Constants.LOGIN_ATTEMPTS_REDIS_KEY + username;
	
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
		LocalDateTime startTest = LocalDateTime.now();
		
		Role roleUser = new Role(RoleName.ROLE_USER);
		roleRepository.save(roleUser);
		
		user = new User(username, password, "Jorge García", "jorgegarcia22@hotmail.com", LocalDate.of(1978, 11, 26));
		user.setAccountStatus(AccountStatus.ACTIVE);
		user.setPassword(passwordEncoder.encode(password));		
		userRepository.save(user);
		
		redisTemplate.opsForValue().set(redisKey, "2", 5, TimeUnit.MINUTES);
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);		
		String body = "username=" + username + "&password=" + password;
		HttpEntity<String> request = new HttpEntity<>(body, headers);
		
		// Act
		ResponseEntity<StandardResponse> response = testRestTemplate
				.postForEntity("/login", request, StandardResponse.class);
		
		LocalDateTime endTest = LocalDateTime.now();
		List<LoginAttempt> loginAttemptsDuringTestExecution = loginAttemptRepository.findAll();		
		
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
		        LoginResponse loginResponse = (LoginResponse) response.getBody().getDetails();
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
	}
}