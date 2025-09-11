package dev.jcasaslopez.user.handler;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import dev.jcasaslopez.user.enums.TokenType;
import dev.jcasaslopez.user.service.AccountOrchestrationService;
import dev.jcasaslopez.user.service.AuthenticationService;
import dev.jcasaslopez.user.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AuthenticationFlowHandler {
	
	private static final Logger logger = LoggerFactory.getLogger(AuthenticationFlowHandler.class);

	private final StandardResponseHandler standardResponseHandler;
	private final TokenService tokenService;
	private final AuthenticationService authService;
	private final AccountOrchestrationService accountOrchestrationService;
	
	public AuthenticationFlowHandler(StandardResponseHandler standardResponseHandler, TokenService tokenService,
			AuthenticationService authService, AccountOrchestrationService accountOrchestrationService) {
		this.standardResponseHandler = standardResponseHandler;
		this.tokenService = tokenService;
		this.authService = authService;
		this.accountOrchestrationService = accountOrchestrationService;
	}

	// Logout
	// You have to receive the refresh token, not the access one.
	// No need to check blacklist status - logout achieves the same end result regardless.
	public void handleLogOutFlow(HttpServletResponse response, String token, TokenType purpose, String method) throws IOException {

		if(purpose != TokenType.REFRESH) {
			logger.warn("Expected REFRESH token, but received {}", purpose.name());
			standardResponseHandler.handleResponse(response, 401, "Access denied: invalid or missing token", null);
			return;
		}
		
		logger.info("Valid REFRESH token received for logging out path");
		tokenService.logOut(token);
		standardResponseHandler.handleResponse(response, 200, "The user has been logged out", null);
	}

	// Verification token
	public void handleVerificationFlow(HttpServletRequest request, HttpServletResponse response, String token, 
			TokenType purpose, String method, String username, String path) throws IOException {

		if(purpose == TokenType.VERIFICATION) {
			request.setAttribute("token", token);
			logger.info("Valid verification token received for path: {}", path);
			return;
		}

		logger.warn("Expected VERIFICATION token, but received {}", purpose.name());
		standardResponseHandler.handleResponse(response, 401, "Access denied: invalid or missing token", null);
	}

	// Refresh token
	public void handleRefreshFlow(HttpServletRequest request, HttpServletResponse response, String token,
			TokenType purpose, String username, String path) throws IOException {
		
		boolean isTokenBlacklisted = tokenService.isTokenBlacklisted(token);

		if(purpose == TokenType.REFRESH && !isTokenBlacklisted) {
			logger.info("Processing token refresh for user: {}", username);
			tokenService.blacklistToken(token);
			List<String> tokens = accountOrchestrationService.refreshToken();
			standardResponseHandler.handleResponse(response, 201, "New refresh and access tokens sent successfully", 
					tokens);
			return;
		}
		
		if(purpose != TokenType.REFRESH) {
			logger.warn("Expected REFRESH token, but received {}", purpose.name());
		} else if (isTokenBlacklisted){
			logger.warn("Refresh token is blacklisted for user: {}", username);
		}
		standardResponseHandler.handleResponse(response, 401, "Access denied: invalid or missing token", null);
	}

	// Access token
	public void handleAccessFlow(HttpServletResponse response, String token, TokenType purpose, String username) throws IOException {
		if (purpose != TokenType.ACCESS) {
			logger.warn("Expected ACCESS token, but received {}", purpose.name());
		    return;
		  }
		  authService.authenticateUser(token, username);
	}

}
