package dev.jcasaslopez.user.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import dev.jcasaslopez.user.entity.User;
import dev.jcasaslopez.user.enums.TokenType;
import dev.jcasaslopez.user.testhelper.TestHelper;
import dev.jcasaslopez.user.utilities.Constants;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class TokenServiceLogOutTest {
	
	@Autowired private TokenService tokenService;
	@Autowired private TestHelper testHelper;
    @Autowired private RedisTemplate<String, String> redisTemplate;

    @Test
    void logOut_ShouldBlacklistTokenAndClearSecurityContext() {
        // Arrange
    	User user = new User();
        String refreshToken = testHelper.loginUser(user, TokenType.REFRESH);
        String jti = tokenService.getJtiFromToken(refreshToken);
        String redisKey = Constants.REFRESH_TOKEN_REDIS_KEY + jti;

        // Act
        tokenService.logOut(refreshToken);

        // Assert
        String redisValue = redisTemplate.opsForValue().get(redisKey);
        assertAll(
        		() -> assertEquals("blacklisted", redisValue, 
        				"Redis entry value should be 'blacklisted'"),
        		() -> assertNull(SecurityContextHolder.getContext().getAuthentication(), 
        				"SecurityContextHolder should not be populated")
        		);
    }
}