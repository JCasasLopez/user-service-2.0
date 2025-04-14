package dev.jcasaslopez.user.security.handler;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import dev.jcasaslopez.user.entity.User;
import dev.jcasaslopez.user.enums.TokenType;
import dev.jcasaslopez.user.handler.StandardResponseHandler;
import dev.jcasaslopez.user.model.TokensLifetimes;
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
	private TokensLifetimes tokensLifetimes;
	private UserAccountService userAccountService;

	public CustomAuthenticationSuccessHandler(StringRedisTemplate redisTemplate,
			StandardResponseHandler standardResponseHandler, LoginAttemptService loginAttemptService,
			TokenService tokenService, TokensLifetimes tokensLifetimes, UserAccountService userAccountService) {
		this.redisTemplate = redisTemplate;
		this.standardResponseHandler = standardResponseHandler;
		this.loginAttemptService = loginAttemptService;
		this.tokenService = tokenService;
		this.tokensLifetimes = tokensLifetimes;
		this.userAccountService = userAccountService;
	}

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {
		// Reiniciamos el contador de intentos fallidos eliminando su entrada en Redis.
		// 
		// Reset the failed login attempts counter by deleting its Redis entry.
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		User user = userAccountService.findUser(username);
		String redisKey = Constants.LOGIN_ATTEMPTS_REDIS_KEY + username;
		
		// Si no existe esta entrada, no hay error.
		//
		// If there is no such entry, no error/exception is thrown.
		redisTemplate.delete(redisKey);
		
		loginAttemptService.recordAttempt(true, request.getRemoteAddr(), null, user);
		
		String refreshToken = tokenService.createAuthToken(TokenType.REFRESH);
		String accessToken = tokenService.createAuthToken(TokenType.ACCESS);
		List<String> refreshAndAccessTokens = List.of(refreshToken, accessToken);

		// Sube el token de refresco a Redis -> clave: refresh_token:jti.
		//
		// Persists refresh token in Redis -> key: refresh_token:jti.
		String refreshTokenJti = tokenService.getJtiFromToken(refreshToken);
		String refreshTokenRedisKey = Constants.REFRESH_TOKEN_REDIS_KEY + refreshTokenJti;
		int expirationInSeconds = tokensLifetimes.getTokensLifetimes().get(TokenType.REFRESH) * 60;		
		redisTemplate.opsForValue().set(refreshTokenRedisKey, TokenType.REFRESH.prefix(), 
				expirationInSeconds, TimeUnit.SECONDS);

		logger.info("Login successful for user '{}'. Attempts reset and login attempt persisted.", username);
		
		// Devuelve los tokens en la respuesta.
		//
		// Returns tokens in the response.
		standardResponseHandler.handleResponse(response, 200, "Login attempt successful", 
				refreshAndAccessTokens);
	}
}
