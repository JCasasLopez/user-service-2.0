package dev.jcasaslopez.user.security.filter;

import java.io.IOException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import dev.jcasaslopez.user.dto.AuthenticationRequest;
import dev.jcasaslopez.user.enums.TokenType;
import dev.jcasaslopez.user.handler.StandardResponseHandler;
import dev.jcasaslopez.user.service.AuthenticationService;
import dev.jcasaslopez.user.service.TokenService;
import dev.jcasaslopez.user.utilities.Constants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AuthenticationFilter extends OncePerRequestFilter {

	private static final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);

	private final StandardResponseHandler standardResponseHandler;
	private final TokenService tokenService;
	private final AuthenticationService authService;

	public AuthenticationFilter(StandardResponseHandler standardResponseHandler, TokenService tokenService,
			AuthenticationService authService) {
		this.standardResponseHandler = standardResponseHandler;
		this.tokenService = tokenService;
		this.authService = authService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		
		// CASE 1:
		// There is no token in the header or it is malformed, which means either of 2 things:
		// 1) The endpoint needs authentication, but it is not provided, so the flow will be captured by 
		//    CustomAuthenticationEntryPoint, which will return a 401 response.
		// 2) The endpoint does NOT need authentication, so it does not matter if the token is missing or the
		//    header is malformed.
		// At any rate, we allow the flow to continue through the Security Filter Chain.
		if(!authService.verifyHeaderIsValid(request)) {
			filterChain.doFilter(request, response);
			return;		
		}
		
		Optional<AuthenticationRequest> optionalAuthRequest = authService.parseAuthenticationRequest(request, response);
		
		// CASE 2:
		if (optionalAuthRequest.isEmpty()) {
			standardResponseHandler.handleResponse(response, 401, "Token is expired, invalid or malformed", null);
			return;
		}
		
		// CASE 3:
		// optionalAuthRequest.isPresent() is present, everything is OK, and we can continue with authentication process.
		AuthenticationRequest authRequest = optionalAuthRequest.get();
		String endpointPath = authRequest.getPath();
		String method = authRequest.getMethod();
		String token = authRequest.getToken();
		String path = authRequest.getPath();
		String username = authRequest.getUsername();
		TokenType purpose = authRequest.getPurpose();

		try {
			switch(endpointPath) {

			// TokenType.REFRESH: revokes the token in the service and returns.
			case Constants.LOGOUT_PATH:  
				handleLogOutFlow(response, token, purpose, method);
				break;

				// TokenType.VERIFICATION: we set the token as a request attribute, as we will need it for the 
				// verification process, and allow the flow to continue to the endpoint.
			case Constants.REGISTRATION_PATH:
			case Constants.RESET_PASSWORD_PATH:
				handleVerificationFlow(request, response, token, purpose, method, path);
				filterChain.doFilter(request, response);
				break;

				// TokenType.REFRESH: we authenticate the user, revoke the refresh token and allow 
				// the flow to continue to the endpoint.
			case Constants.REFRESH_TOKEN_PATH:
				handleRefreshFlow(request, response, token, purpose, username, method, path);
				filterChain.doFilter(request, response);
				break;

				// If there is a token and it is valid, but the HTTP request does not match any of the above cases, 
				// then we are dealing with one of the endpoints that need a TokenType.ACCESS.
				// We authenticate the user and allow the flow to continue to the endpoint.
			default:
				handleAccessFlow(response, token, purpose, username);
				filterChain.doFilter(request, response);
				break;
			}

		} catch (IOException ex) {
			logger.warn("I/O error while writing HTTP response", ex);
			standardResponseHandler.handleResponse(response, 500, "An error occurred while processing the response", null);
		}

	}
		
	// Logout
	// You have to receive the refresh token, not the access one.
	// No need to check blacklist status - logout achieves the same end result regardless.
	private void handleLogOutFlow(HttpServletResponse response, String token, TokenType purpose, String method) 
			throws IOException {
		
		if("POST".equalsIgnoreCase(method) && purpose == TokenType.REFRESH) {
			logger.info("Processing logout request");
			tokenService.logOut(token);
			standardResponseHandler.handleResponse(response, 200, "The user has been logged out", null);
			return;
		}
		standardResponseHandler.handleResponse(response, 401, "Invalid log out request", null);
		return;
	}
	
	// Verification token
	private void handleVerificationFlow(HttpServletRequest request, HttpServletResponse response, String token, 
			TokenType purpose, String method, String path) throws IOException {
		
		if(purpose == TokenType.VERIFICATION && ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method))) {
			request.setAttribute("token", token);
			logger.info("Valid verification token received for path: {}", path);
			return;
		}
		standardResponseHandler.handleResponse(response, 401, "Invalid verification request", null);
		return;
	}
	
	// Refresh token
	private void handleRefreshFlow(HttpServletRequest request, HttpServletResponse response, String token, 
			TokenType purpose, String username, String method, String path) throws IOException {
		if ("POST".equalsIgnoreCase(method) && !tokenService.isTokenBlacklisted(token) && 
				purpose == TokenType.REFRESH) {
			authService.authenticateUser(token, username);
			// We have to revoke the token, since the system will issue a new one
			// in the next step (controller -> AccountOrchestrationService).
			tokenService.blacklistToken(token);
			return;
		}
		standardResponseHandler.handleResponse(response, 401, "Invalid token refresh request", null);
		return;
	}
	
	// Access token
	private void handleAccessFlow(HttpServletResponse response, String token, TokenType purpose, String username) 
			throws IOException {
		if (purpose == TokenType.ACCESS) {
			authService.authenticateUser(token, username);	
			return;
		}
		// No need to handle an incorrect token type here; it will be handled later in the filter chain 
		// by CustomAuthenticationEntryPoint.

	}
	
}
