package dev.jcasaslopez.user.security.handler;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
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
import dev.jcasaslopez.user.entity.User;
import dev.jcasaslopez.user.mapper.UserMapper;
import dev.jcasaslopez.user.service.TokenService;
import dev.jcasaslopez.user.testhelper.TestHelper;
import dev.jcasaslopez.user.testhelper.UserTestBuilder;
import dev.jcasaslopez.user.utilities.Constants;

//@AutoConfigureMockMvc is needed because AuthenticationTestHelper requires MockMvc bean,
//which is not available by default in @SpringBootTest with RANDOM_PORT configuration.

@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AuthSuccessHandlerSystemIntegrationTest {
	
	@Value ("${auth.maxFailedAttempts}") int maxNumberFailedAttempts;
	
	@Autowired private TestRestTemplate testRestTemplate;
	@Autowired private RedisTemplate<String, String> redisTemplate;
	@Autowired private UserMapper userMapper;
	@Autowired private TokenService tokenService;
	@Autowired private ObjectMapper objectMapper;
	@Autowired private TestHelper testHelper;
	
	// Static variable to share state between tests.
	private static User user;
	
	// Immutable test constants defining the input data.
	private static final String USERNAME = "Yorch22";
	private static final String PASSWORD = "Jorge22!";
	
	@BeforeEach
	void setUp() {
			UserTestBuilder builder = new UserTestBuilder(USERNAME, PASSWORD);
			user = testHelper.createAndPersistUser(builder);
	}
	
	@AfterEach
	void cleanAfterTest() {
		testHelper.cleanDataBaseAndRedis();
	}
	
	@Test
	@DisplayName("If login attempt is successful, Redis entry is deleted and 200 OK returned")
	void CustomAuthenticationSuccessHandler_WhenLoginSuccessful_ShouldReturn200OkAndDeleteRedisEntry() {
		// Arrange		
		// Reads maximum number of attempts allowed, and sets Redis to that value minus 1.
		String maxAttemptsMinusOne = String.valueOf(maxNumberFailedAttempts - 1);
		
		String redisKey = Constants.LOGIN_ATTEMPTS_REDIS_KEY + USERNAME;
		redisTemplate.opsForValue().set(redisKey, maxAttemptsMinusOne, 5, TimeUnit.MINUTES);
		
		HttpEntity<String> request = configHttpRequest();
		
		// Act
		ResponseEntity<StandardResponse> response = testRestTemplate.postForEntity(Constants.LOGIN_PATH, request, StandardResponse.class);
		
		// Assert
		assertAll(
		    () -> assertNull(redisTemplate.opsForValue().get(redisKey),  "Redis entry for that should have been deleted"),
		    () -> assertEquals(HttpStatus.OK, response.getStatusCode(), "Response HTTP status should be 200 OK"),
		    () -> assertEquals("Login attempt successful", response.getBody().getMessage(), "Unexpected response message"),
		    () -> {
		    	LoginResponse loginResponse = objectMapper.convertValue(response.getBody().getDetails(), LoginResponse.class);
		        assertNotNull(loginResponse, "Response should contain details (user & tokens)");
		        assertAll(
		            () -> assertEquals(userMapper.userToUserDtoMapper(user).getIdUser(), 
		                    loginResponse.getUser().getIdUser(), "Authenticated user and returned user should be the same"),
		            () -> assertDoesNotThrow(() -> tokenService.getValidClaims(loginResponse.getRefreshToken()),
		                    "Returned refresh token should be a valid token"),
		            () -> assertDoesNotThrow(() -> tokenService.getValidClaims(loginResponse.getAccessToken()),
		                    "Returned access token should be a valid token")
		        );
		    }
		);
	}
	
	private HttpEntity<String> configHttpRequest(){
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);		
		String body = "username=" + USERNAME + "&password=" + PASSWORD;
		return new HttpEntity<>(body, headers);
	}
}