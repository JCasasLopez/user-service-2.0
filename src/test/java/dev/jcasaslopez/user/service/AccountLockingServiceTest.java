package dev.jcasaslopez.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import dev.jcasaslopez.user.utilities.Constants;

@ExtendWith(MockitoExtension.class)
public class AccountLockingServiceTest {
	
	@Mock StringRedisTemplate redisTemplate;
	@Mock ValueOperations<String, String> valueOperations; 
	@InjectMocks AccountLockingServiceImpl accountLockingServiceImpl;
	
	private final String USERNAME = "Yorch22";
	private String redisKey = Constants.LOGIN_ATTEMPTS_REDIS_KEY + USERNAME;
	
	@Test
	@DisplayName("When Redis entry is null, getLoginAttemptsRedisEntry() returns 0")
	public void getLoginAttemptsRedisEntry_WhenNoRedisEntry_Returns0() {
		// Arrange
		when(redisTemplate.hasKey(redisKey)).thenReturn(false);
        
		// Act
        int numberOfAttempts = accountLockingServiceImpl.getLoginAttemptsRedisEntry(USERNAME);
		
		// Assert
        assertEquals(0, numberOfAttempts, "Number of login attempts should be 0");
	}
	
	@Test
	@DisplayName("When Redis entry is not null, getLoginAttemptsRedisEntry() returns number of failed login attempts")
	public void getLoginAttemptsRedisEntry_WhenRedisEntryPresent_ReturnsNumberOfFailedLoginAttempts() {
		// Arrange
        when(redisTemplate.hasKey(redisKey)).thenReturn(true);
        // Mock also the intermediate object valueOperations.
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(redisKey)).thenReturn("2");
        
		// Act
        int numberOfAttempts = accountLockingServiceImpl.getLoginAttemptsRedisEntry(USERNAME);
		
		// Assert
        assertEquals(2, numberOfAttempts, "Number of login attempts should be 2");
	}

}
