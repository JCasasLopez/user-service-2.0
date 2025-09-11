package dev.jcasaslopez.user.security.filter;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import dev.jcasaslopez.user.dto.AuthenticationRequest;
import dev.jcasaslopez.user.enums.TokenType;
import dev.jcasaslopez.user.handler.AuthenticationFlowHandler;
import dev.jcasaslopez.user.handler.StandardResponseHandler;
import dev.jcasaslopez.user.service.AuthenticationService;
import dev.jcasaslopez.user.utilities.Constants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Endpoints are grouped into:
// a) Public: no authentication or token required.
// b) Action-token: require a valid token (verification, logout, refresh), but do not need an authenticated user (populate SecurityContext).
// c) Protected: require both a valid token and authentication.
//
// Flow:
// - Public endpoints → always pass through.
// - Action-token / Protected endpoints → validate step by step:
//   1) Header format (AuthenticationService).
//   2) Token validity (parsed in AuthenticationService).
//   3) Token type matches the endpoint (AuthenticationFlowHandler).
//
// Behavior:
// - Action-token endpoints: 
//   • On success → continue in the Security Filter Chain.  
//   • On failure → return HTTP response here.
// - Protected endpoints: 
//   • On success → authenticate user (populate SecurityContext) and continue.  
//   • On failure → continue without authentication (SecurityContext remains empty), letting Spring Security's AuthenticationEntryPoint reject the request.

@Component
public class AuthenticationFilter extends OncePerRequestFilter {

	private static final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);

	private final StandardResponseHandler standardResponseHandler;
	private final AuthenticationService authService;
	private final AuthenticationFlowHandler authFlowHandler;
	
	private static final Set<String> PUBLIC_ENDPOINTS = Set.of(
			Constants.LOGIN_PATH,
			Constants.INITIATE_REGISTRATION_PATH,
			Constants.FORGOT_PASSWORD_PATH
		  );
	
	// Endpoints that require token, but the user does not need to be authenticated (as in SecurityContext populated).
	private static final Set<String> ACTION_TOKEN_ENDPOINTS = Set.of(
		      Constants.LOGOUT_PATH,        // TokenType.REFRESH
		      Constants.REFRESH_TOKEN_PATH, // TokenType.REFRESH
		      Constants.REGISTRATION_PATH,  // TokenType.VERIFICATION
		      Constants.RESET_PASSWORD_PATH // TokenType.VERIFICATION
		  );
	
	public AuthenticationFilter(StandardResponseHandler standardResponseHandler, AuthenticationService authService,
			AuthenticationFlowHandler authFlowHandler) {
		this.standardResponseHandler = standardResponseHandler;
		this.authService = authService;
		this.authFlowHandler = authFlowHandler;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
				
		// Public → passthrough
		boolean isPublicEndpoint = PUBLIC_ENDPOINTS.contains(request.getServletPath());
		
		if(isPublicEndpoint) {
			logger.debug("The endpoint is public. Continues with Security Filter Chain...");
			filterChain.doFilter(request, response);
			return;	
		}
		
		// Non-public + missing/invalid header
		boolean isAuthHeaderInvalid = !authService.verifyHeaderIsValid(request);
		boolean isActionTokenEndpoint = ACTION_TOKEN_ENDPOINTS.contains(request.getServletPath());
		
		if(isAuthHeaderInvalid) {
			if(isActionTokenEndpoint) {
				standardResponseHandler.handleResponse(response, 401, "Access denied: invalid or missing token", null);
				return;
			}
			logger.debug("Invalid or empty header. Protected endpoint. Continues with Security Filter Chain...");
			filterChain.doFilter(request, response);
			return;		
		}
		
		Optional<AuthenticationRequest> optionalAuthRequest = authService.parseAuthenticationRequest(request, response);
		
		// Non-public + parse token
		boolean isTokenInvalid = optionalAuthRequest.isEmpty();
		if (isTokenInvalid) {
			if(isActionTokenEndpoint) {
				standardResponseHandler.handleResponse(response, 401, "Access denied: invalid or missing token", null);
				return;
			}
			logger.warn("Token is expired, malformed or the signature is invalid. Continues with Security Filter Chain...");
			filterChain.doFilter(request, response);
			return;
		}
		
		// Route by requestPath (valid token)
		AuthenticationRequest authRequest = optionalAuthRequest.get();
		String method = authRequest.getMethod();
		String token = authRequest.getToken();
		String path = authRequest.getPath();
		String username = authRequest.getUsername();
		TokenType purpose = authRequest.getPurpose();

		try {
			switch(path) {
			
			case Constants.LOGOUT_PATH:  
				authFlowHandler.handleLogOutFlow(response, token, purpose, method);
			    if (response.isCommitted()) {
			        return; 
			    }
			    break;

			case Constants.REGISTRATION_PATH:
			case Constants.RESET_PASSWORD_PATH:
				authFlowHandler.handleVerificationFlow(request, response, token, purpose, method, username, path);
				if (response.isCommitted()) {
					return; 
				}
				filterChain.doFilter(request, response);
				return;
			
			case Constants.REFRESH_TOKEN_PATH:
				authFlowHandler.handleRefreshFlow(request, response, token, purpose, username, path);
				if (response.isCommitted()) {
					return;  
				}
				// Just in case there is no answer.
			    filterChain.doFilter(request, response);
			    return;
				
			default:
				authFlowHandler.handleAccessFlow(response, token, purpose, username);
				if (response.isCommitted()) {
					return;  
				}
				filterChain.doFilter(request, response);
			    return;
			}

		} catch (IOException ex) {
			logger.warn("I/O error while writing HTTP response", ex);
			standardResponseHandler.handleResponse(response, 500, "An error occurred while processing the response", null);
		}
	}	
}