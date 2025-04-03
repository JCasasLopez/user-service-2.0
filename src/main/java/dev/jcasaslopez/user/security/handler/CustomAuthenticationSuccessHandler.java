package dev.jcasaslopez.user.security.handler;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import dev.jcasaslopez.user.handler.StandardResponseHandler;
import dev.jcasaslopez.user.service.LoginAttemptService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
	
    private static final Logger logger = LoggerFactory.getLogger(CustomAuthenticationSuccessHandler.class);

	private StringRedisTemplate redisTemplate;
	private StandardResponseHandler standardResponseHandler;
	private LoginAttemptService loginAttemptService;

	public CustomAuthenticationSuccessHandler(StringRedisTemplate redisTemplate,
			StandardResponseHandler standardResponseHandler, LoginAttemptService loginAttemptService) {
		this.redisTemplate = redisTemplate;
		this.standardResponseHandler = standardResponseHandler;
		this.loginAttemptService = loginAttemptService;
	}

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {
		// Reiniciamos el contador de intentos fallidos eliminando su entrada en Redis.
		// 
		// Reset the failed login attempts counter by deleting its Redis entry.
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		redisTemplate.delete("login_attempts:" + username);
		
		loginAttemptService.recordAttempt(true, request.getRemoteAddr(), null);
		
		logger.info("Login successful for user '{}'. Attempts reset and login attempt persisted.", username);
		standardResponseHandler.handleResponse(response, 200, "Login attempt successful", null);
	}
}
