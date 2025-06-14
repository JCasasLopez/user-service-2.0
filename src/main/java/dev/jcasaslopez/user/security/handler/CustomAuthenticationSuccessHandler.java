package dev.jcasaslopez.user.security.handler;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import dev.jcasaslopez.user.dto.LoginResponse;
import dev.jcasaslopez.user.dto.UserDto;
import dev.jcasaslopez.user.entity.User;
import dev.jcasaslopez.user.enums.TokenType;
import dev.jcasaslopez.user.handler.StandardResponseHandler;
import dev.jcasaslopez.user.mapper.UserMapper;
import dev.jcasaslopez.user.service.LoginAttemptService;
import dev.jcasaslopez.user.service.TokenService;
import dev.jcasaslopez.user.service.UserAccountService;
import dev.jcasaslopez.user.utilities.Constants;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
	
    private static final Logger logger = LoggerFactory.getLogger(CustomAuthenticationSuccessHandler.class);

	private StringRedisTemplate redisTemplate;
	private StandardResponseHandler standardResponseHandler;
	private LoginAttemptService loginAttemptService;
	private TokenService tokenService;
	private UserAccountService userAccountService;
	private UserMapper userMapper;

	public CustomAuthenticationSuccessHandler(StringRedisTemplate redisTemplate,
			StandardResponseHandler standardResponseHandler, LoginAttemptService loginAttemptService,
			TokenService tokenService, UserAccountService userAccountService, UserMapper userMapper) {
		this.redisTemplate = redisTemplate;
		this.standardResponseHandler = standardResponseHandler;
		this.loginAttemptService = loginAttemptService;
		this.tokenService = tokenService;
		this.userAccountService = userAccountService;
		this.userMapper = userMapper;
	}

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {
		
		// Reset the failed login attempts counter by deleting its Redis entry.
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		User user = userAccountService.findUser(username);
		String redisKey = Constants.LOGIN_ATTEMPTS_REDIS_KEY + username;
		redisTemplate.delete(redisKey);
		
		loginAttemptService.recordAttempt(true, request.getRemoteAddr(), null, user);
		
		String refreshToken = tokenService.createAuthToken(TokenType.REFRESH);
		String accessToken = tokenService.createAuthToken(TokenType.ACCESS);
		
		logger.info("Login successful for user '{}'. Attempts reset and login attempt persisted.", username);
		
		UserDto userDto = userMapper.userToUserDtoMapper(user);
		LoginResponse loginResponse = new LoginResponse (userDto, refreshToken, accessToken);
		standardResponseHandler.handleResponse(response, 200, "Login attempt successful", 
				loginResponse);
	}
}
