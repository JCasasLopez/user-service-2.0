package dev.jcasaslopez.user.security.filter;

import java.io.IOException;
import java.util.Optional;

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
// - Non-public endpoints → validate step by step:
//   1) Header format (AuthenticationService).
//   2) Token validity (parsed in AuthenticationService).
//   3) Token type matches the endpoint (AuthenticationFlowHandler).
//
// Behavior:
// - Action-token endpoints: 
//   • On success → continue in the Security Filter Chain.  
//   • On failure → return HTTP 401 in AuthenticationFlowHandler.
// - Protected endpoints: 
//   • On success → authenticate user (populate SecurityContext) and continue in the Security Filter Chain.   
//   • On failure → allow the request to continue through the filter chain without populating the SecurityContext. 
//     Spring Security (through AuthenticationEntryPoint) will automatically return a 401 Unauthorized response.
//     This approach prevents duplicate 401 responses that would occur if both this filter and Spring Security
//     failure handler attempted to write error responses.

@Component
public class AuthenticationFilter extends OncePerRequestFilter {

	private static final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);

	private final StandardResponseHandler standardResponseHandler;
	private final AuthenticationService authService;
	private final AuthenticationFlowHandler authFlowHandler;
	
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
		boolean isPublicEndpoint = Constants.PUBLIC_ENDPOINTS.contains(request.getServletPath());
		
		if(isPublicEndpoint) {
			logger.debug("The endpoint is public. Continues with Security Filter Chain...");
			filterChain.doFilter(request, response);
		    return;
		}
		
		// Invalid header → 401
		boolean isAuthHeaderInvalid = !authService.verifyHeaderIsValid(request);
		
		if (isAuthHeaderInvalid) {
			logger.debug("Invalid or empty header");
		    standardResponseHandler.handleResponse(response, 401, "Access denied: invalid or missing token", null);
		    return;
		}
		
		// Invalid token → 401.
		// We cannot group header validation and token parsing together because calling parseAuthenticationRequest() 
		// without header validation would throw an exception.
		Optional<AuthenticationRequest> optionalAuthRequest = authService.parseAuthenticationRequest(request, response);
		boolean isTokenInvalid = optionalAuthRequest.isEmpty();

		if (isTokenInvalid) {
		    logger.warn("Token is expired, malformed or the signature is invalid. Continues with Security Filter Chain...");
		    standardResponseHandler.handleResponse(response, 401, "Access denied: invalid or missing token", null);
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
				break;
			
			case Constants.REFRESH_TOKEN_PATH:
				authFlowHandler.handleRefreshFlow(request, response, token, purpose, username, path);
				if (response.isCommitted()) {
					return;  
				}
				break;
				
			default:
				authFlowHandler.handleAccessFlow(response, token, purpose, username);
				if (response.isCommitted()) {
					return;  
				}
				filterChain.doFilter(request, response);
			    break;
			}

		} catch (IOException ex) {
			logger.warn("I/O error while writing HTTP response", ex);
			standardResponseHandler.handleResponse(response, 500, "An error occurred while processing the response", null);
		}
	}	
}