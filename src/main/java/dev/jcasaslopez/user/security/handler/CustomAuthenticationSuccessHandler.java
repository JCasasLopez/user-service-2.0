package dev.jcasaslopez.user.security.handler;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import dev.jcasaslopez.user.service.AccountLockingService;
import dev.jcasaslopez.user.service.LoginAttemptService;
import dev.jcasaslopez.user.service.TokenService;
import dev.jcasaslopez.user.service.UserAccountService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
	
    private static final Logger logger = LoggerFactory.getLogger(CustomAuthenticationSuccessHandler.class);

	private final StandardResponseHandler standardResponseHandler;
	private final LoginAttemptService loginAttemptService;
	private final TokenService tokenService;
	private final UserAccountService userAccountService;
	private final UserMapper userMapper;
	private final AccountLockingService accountLockingService;

	public CustomAuthenticationSuccessHandler(StandardResponseHandler standardResponseHandler,
			LoginAttemptService loginAttemptService, TokenService tokenService, UserAccountService userAccountService,
			UserMapper userMapper, AccountLockingService accountLockingService) {
		this.standardResponseHandler = standardResponseHandler;
		this.loginAttemptService = loginAttemptService;
		this.tokenService = tokenService;
		this.userAccountService = userAccountService;
		this.userMapper = userMapper;
		this.accountLockingService = accountLockingService;
	}

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {
		
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		User user = userAccountService.findUser(username);
		
		// Reset the failed login attempts counter by deleting its Redis entry.
		accountLockingService.deleteLoginAttemptsRedisEntry(username);
		
		loginAttemptService.recordAttempt(true, request.getRemoteAddr(), null, user);
		
		String refreshToken = tokenService.createAuthToken(TokenType.REFRESH, username);
		String accessToken = tokenService.createAuthToken(TokenType.ACCESS, username);
		logger.info("Login successful for user '{}'. Attempts reset and login attempt persisted.", username);
		
		UserDto userDto = userMapper.userToUserDtoMapper(user);
		LoginResponse loginResponse = new LoginResponse (userDto, refreshToken, accessToken);
		standardResponseHandler.handleResponse(response, 200, "Login attempt successful", loginResponse);
	}
}
