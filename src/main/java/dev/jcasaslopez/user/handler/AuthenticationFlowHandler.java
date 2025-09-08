package dev.jcasaslopez.user.handler;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import dev.jcasaslopez.user.enums.TokenType;
import dev.jcasaslopez.user.security.filter.AuthenticationFilter;
import dev.jcasaslopez.user.service.AuthenticationService;
import dev.jcasaslopez.user.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AuthenticationFlowHandler {
	
	private static final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);

	private final StandardResponseHandler standardResponseHandler;
	private final TokenService tokenService;
	private final AuthenticationService authService;
	
	public AuthenticationFlowHandler(StandardResponseHandler standardResponseHandler, TokenService tokenService,
			AuthenticationService authService) {
		this.standardResponseHandler = standardResponseHandler;
		this.tokenService = tokenService;
		this.authService = authService;
	}

	// Logout
	// You have to receive the refresh token, not the access one.
	// No need to check blacklist status - logout achieves the same end result regardless.
	public void handleLogOutFlow(HttpServletResponse response, String token, TokenType purpose, String method) throws IOException {

		if(!"POST".equalsIgnoreCase(method)) {
			logger.warn("Expected HTTP method POST, but received {}", method);
			standardResponseHandler.handleResponse(response, 405, "HTTP method not allowed", null);
			return;
		}

		if(purpose != TokenType.REFRESH) {
			logger.warn("Expected token type REFRESH, but received {}", purpose.name());
			standardResponseHandler.handleResponse(response, 401, "Access denied: invalid or missing token", null);
			return;
		}
		
		logger.info("Valid refresh token received for logging out path");
		tokenService.logOut(token);
		standardResponseHandler.handleResponse(response, 200, "The user has been logged out", null);
		return;
	}

	// Verification token
	public boolean handleVerificationFlow(HttpServletRequest request, HttpServletResponse response, String token, 
			TokenType purpose, String method, String path) throws IOException {

		if(!"POST".equalsIgnoreCase(method) && !"PUT".equalsIgnoreCase(method)) {
			logger.warn("Expected HTTP method POST/PUT, but received {}", method);
			standardResponseHandler.handleResponse(response, 405, "HTTP method not allowed", null);
			return false;
		}

		if(purpose == TokenType.VERIFICATION) {
			request.setAttribute("token", token);
			logger.info("Valid verification token received for path: {}", path);
			return true;
		}

		logger.warn("Expected token type VERIFICATION, but received {}", purpose.name());
		standardResponseHandler.handleResponse(response, 401, "Access denied: invalid or missing token", null);
		return false;		
	}

	// Refresh token
	public boolean handleRefreshFlow(HttpServletRequest request, HttpServletResponse response, String token,
			TokenType purpose, String username, String method, String path) throws IOException {

		if(!"POST".equalsIgnoreCase(method)) {
			logger.warn("Expected HTTP method POST, but received {}", method);
			standardResponseHandler.handleResponse(response, 405, "HTTP method not allowed", null);
			return false;
		}

		if(purpose != TokenType.REFRESH) {
			logger.warn("Expected token type REFRESH, but received {}", purpose.name());
			standardResponseHandler.handleResponse(response, 401, "Access denied: invalid or missing token", null);
			return false;
		}

		if(tokenService.isTokenBlacklisted(token)) {
			logger.warn("Refresh token is blacklisted for user: {}", username);
			standardResponseHandler.handleResponse(response, 401, "Access denied: invalid or missing token", null);
			return false;
		}

		logger.info("Processing token refresh for user: {}", username);
		authService.authenticateUser(token, username);
		tokenService.blacklistToken(token);
		return true;
	}

	// Access token
	public boolean handleAccessFlow(HttpServletResponse response, String token, TokenType purpose, String username) throws IOException {
		if (purpose == TokenType.ACCESS) {
			authService.authenticateUser(token, username);	
			return true;
		}
		
		logger.warn("Expected token type ACCESS, but received {}", purpose.name());
		standardResponseHandler.handleResponse(response, 401, "Access denied: invalid or missing token", null);
		return false;
	}

}
