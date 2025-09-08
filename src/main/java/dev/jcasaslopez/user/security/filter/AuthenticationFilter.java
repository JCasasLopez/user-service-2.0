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
		
		// CASE 1:
		// There is no token in the header or it is malformed, which means either of 2 things:
		// 1) The endpoint needs authentication, but it is not provided, so the flow will be captured by 
		//    CustomAuthenticationEntryPoint, which will return a 401 response.
		// 2) The endpoint does NOT need authentication, so it does not matter if the token is missing or the
		//    header is malformed.
		// At any rate, we allow the flow to continue through the Security Filter Chain.
		if(!authService.verifyHeaderIsValid(request)) {
			logger.warn("Header is empty or invalid. Security Filter Chain continues...");
			filterChain.doFilter(request, response);
			return;		
		}
		
		Optional<AuthenticationRequest> optionalAuthRequest = authService.parseAuthenticationRequest(request, response);
		
		// CASE 2:
		// Token is present in the request, but it is not valid.
		if (optionalAuthRequest.isEmpty()) {
			logger.warn("Token is expired, malformed or the signature is invalid");
			standardResponseHandler.handleResponse(response, 401, "Access denied: invalid or missing token", null);
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
			
			// TokenType.REFRESH
			case Constants.LOGOUT_PATH:  
				authFlowHandler.handleLogOutFlow(response, token, purpose, method);
				break;

			// TokenType.VERIFICATION
			case Constants.REGISTRATION_PATH:
			case Constants.RESET_PASSWORD_PATH:
				if(authFlowHandler.handleVerificationFlow(request, response, token, purpose, method, path)) {
					filterChain.doFilter(request, response);
				}
				break;

			// TokenType.REFRESH
			case Constants.REFRESH_TOKEN_PATH:
				if(authFlowHandler.handleRefreshFlow(request, response, token, purpose, username, method, path)) {
					filterChain.doFilter(request, response);
				}
				break;

			// TokenType.ACCESS
			default:
				if(authFlowHandler.handleAccessFlow(response, token, purpose, username)) {
					filterChain.doFilter(request, response);
				}
				break;
			}

		} catch (IOException ex) {
			logger.warn("I/O error while writing HTTP response", ex);
			standardResponseHandler.handleResponse(response, 500, "An error occurred while processing the response", null);
		}
	}	
}