package dev.jcasaslopez.user.filter;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import dev.jcasaslopez.user.dto.StandardResponse;
import dev.jcasaslopez.user.entity.Role;
import dev.jcasaslopez.user.entity.User;
import dev.jcasaslopez.user.enums.AccountStatus;
import dev.jcasaslopez.user.enums.RoleName;
import dev.jcasaslopez.user.repository.LoginAttemptRepository;
import dev.jcasaslopez.user.repository.RoleRepository;
import dev.jcasaslopez.user.repository.UserRepository;
import dev.jcasaslopez.user.service.EmailService;
import dev.jcasaslopez.user.service.UserAccountService;
import dev.jcasaslopez.user.utilities.Constants;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CustomUsernamePasswordAuthenticationFilterTest {
	
	@Autowired private TestRestTemplate testRestTemplate;
	@Autowired private RedisTemplate<String, String> redisTemplate;
	@Autowired private UserAccountService userAccountService;
	@Autowired private UserRepository userRepository;
	@Autowired private RoleRepository roleRepository;
	@Autowired private LoginAttemptRepository loginAttemptRepository;
	@Autowired private PasswordEncoder passwordEncoder;
	@MockBean private EmailService emailService;
	
	private static User user;
	private static final String username = "Yorch";
	private static final String password = "Yorch22!";

	@BeforeAll
	void setUp() {
		Role roleUser = new Role(RoleName.ROLE_USER);
		roleRepository.save(roleUser);
		
		user = new User(username, password, "Jorge García", "jorgegarcia22@hotmail.com", LocalDate.of(1978, 11, 26));
		user.setAccountStatus(AccountStatus.TEMPORARILY_BLOCKED);
		user.setPassword(passwordEncoder.encode(password));	
		user.setRoles(Set.of(roleUser));
		userRepository.save(user);
		userRepository.flush();
		
		redisTemplate.delete(Constants.LOGIN_ATTEMPTS_REDIS_KEY + username);
	}
	
	@AfterAll
	void cleanDatabase() {
		loginAttemptRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        redisTemplate.getConnectionFactory().getConnection().flushAll();
	}

	@Test
	@DisplayName("Switches account to active when lock timeout is over")
	@Order(1)
	void WhenLockTimeoutOver_ShouldSwitchAccountToActive() {
		// Arrange	
		HttpEntity<String> request = setHttpRequest();
		
		// Act
		ResponseEntity<StandardResponse> response = testRestTemplate
				.postForEntity("/login", request, StandardResponse.class);
				
		ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
		verify(emailService).sendEmail(anyString(), anyString(), bodyCaptor.capture());
		String emailBody = bodyCaptor.getValue();
	
		// Assert
		assertAll(
				() -> assertEquals(AccountStatus.ACTIVE, userAccountService.findUser(username).getAccountStatus()),
				() -> assertTrue(emailBody.contains("Your account is active again.")),
				() -> assertEquals(HttpStatus.OK, response.getStatusCode()),
				() -> assertEquals("Login attempt successful", response.getBody().getMessage()),
				() -> assertNotNull(response.getBody().getDetails())
		);
	}
	
	@Test
	@DisplayName("If the Redis entry is still present should return 403 FORBIDDEN")
	@Order(2)
	void WhenLockTimeoutIsNotOver_ShoulddReturn403Forbidden() {
		// Arrange
		user.setAccountStatus(AccountStatus.TEMPORARILY_BLOCKED);
		userRepository.save(user);
		userRepository.flush();
		
    	String redisKey = Constants.LOGIN_ATTEMPTS_REDIS_KEY + username;
		redisTemplate.opsForValue().set(redisKey, "3", 5, TimeUnit.MINUTES);
		
		HttpEntity<String> request = setHttpRequest();
		
		// Act
		ResponseEntity<StandardResponse> response = testRestTemplate
				.postForEntity("/login", request, StandardResponse.class);
				
		// Assert
		assertAll(
				() -> assertEquals(AccountStatus.TEMPORARILY_BLOCKED, userAccountService.findUser(username).getAccountStatus()),
				() -> assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode()),
				() -> assertEquals("Account is locked", response.getBody().getMessage())
		);
	}
	
	@Test
	@DisplayName("If request has no username/password, it returns 400 BAD CREDENTIALS")
	void WhenNoUsernameOrPassword_ShouldThrowException() {
		// Arrange
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		
		String body = "username=Yorch22"; 
		HttpEntity<String> request = new HttpEntity<>(body, headers);
		
		// Act
		ResponseEntity<StandardResponse> response = testRestTemplate
				.postForEntity("/login", request, StandardResponse.class);
		
		// Assert
		assertAll(
				() -> assertEquals(HttpStatus.BAD_REQUEST, response.getBody().getStatus()),
				() -> assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode()),
				() -> assertEquals("Username and password are required", response.getBody().getMessage())
		);
	}
	
	private HttpEntity<String> setHttpRequest() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);		
		String body = "username=" + username + "&password=" + password;
		return new HttpEntity<>(body, headers);
	}
}