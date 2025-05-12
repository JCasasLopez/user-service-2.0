package dev.jcasaslopez.user.controller;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import dev.jcasaslopez.user.dto.StandardResponse;
import dev.jcasaslopez.user.entity.User;
import dev.jcasaslopez.user.enums.TokenType;
import dev.jcasaslopez.user.service.TokenService;
import dev.jcasaslopez.user.testhelper.TestHelper;
import dev.jcasaslopez.user.utilities.Constants;

// Verificamos el endpoint 'logout' porque su lógica es personalizada y no forma parte del flujo 
// estándar de Spring Security. En cambio, endpoints como 'login' son manejados directamente por 
// Spring y no requieren tests de integración propios.
//
// We verify the 'logout' endpoint because its logic is custom and does not follow Spring Security 
// standard flow. In contrast, endpoints like 'login' are handled directly by Spring 
// and do not require dedicated integration tests.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class LogOutIntegrationTest {
	
	@Autowired private TestHelper testHelper;
	@Autowired private TokenService tokenService;
	@Autowired private TestRestTemplate testRestTemplate;
	@Autowired private RedisTemplate<String, String> redisTemplate;
	
	@Test
	@DisplayName("User logs out correctly when valid refresh token is provided")
	void logOut_WhenRefreshTokenProvided_ShouldLogOutUserAndRevokeToken() {
		// Arrange
		User user = new User();
		String refreshToken = testHelper.loginUser(user, TokenType.REFRESH);
		String tokenJti = tokenService.getJtiFromToken(refreshToken);
		String tokenRedisKey = Constants.REFRESH_TOKEN_REDIS_KEY + tokenJti;
		
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(refreshToken);
		HttpEntity<String> request = new HttpEntity<>(headers);
		
		// Act
		ResponseEntity<StandardResponse> response = testRestTemplate
		        .postForEntity(Constants.LOGOUT_PATH, request, StandardResponse.class);
		
		// Assert
		String redisEntryValue = redisTemplate.opsForValue().get(tokenRedisKey);
		
		// Spring Security maneja la petición en un hilo distinto al del test, y como SecurityContextHolder 
		// es específico de cada hilo (ThreadLocal), no podemos verificar desde aquí los cambios que ocurren
		// en el filtro. Esta verificación se realiza en los tests unitarios específicos de TokenService.logOut().
		//
		// Spring Security handles the request in a different thread than the test, and since SecurityContextHolder
		// is thread-local, we can't verify in the test the changes made inside the filter. That
		// verification is done in the dedicated unit tests for TokenService.logOut().
		assertAll(
				() -> assertEquals(HttpStatus.OK, response.getStatusCode(), 
						"Expected 200 OK status code"),
				() -> assertNotNull(response.getBody(), 
						"Response body should not be null"),
				() -> assertEquals("The user has been logged out", response.getBody().getMessage(), 
						"Logout message mismatch"),
				() -> assertEquals("blacklisted", redisEntryValue, 
						"Refresh token should be blacklisted in Redis")
				);
			}
	
	@Test
	@DisplayName("User cannot log out when access or invalid is provided")
	void logOut_WhenAccessOrInvalidTokenProvided_ShouldNotLogOutUser() {
		// Arrange
		User user = new User();
		String accessToken = testHelper.loginUser(user, TokenType.ACCESS);
		
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(accessToken);
		HttpEntity<String> request = new HttpEntity<>(headers);
		
		// Act
		ResponseEntity<StandardResponse> response = testRestTemplate
		        .postForEntity(Constants.LOGOUT_PATH, request, StandardResponse.class);
		
		// Assert
		assertAll(
				() -> assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode(), 
						"Expected 401 UNAUTHORIZED status code"),
				() -> assertNotNull(response.getBody(), 
						"Response body should not be null"),
				() -> assertTrue(response.getBody().getMessage().contains("Access denied"), 
						"Unexpected response message")
				);
		}
}