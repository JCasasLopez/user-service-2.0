package dev.jcasaslopez.user.security.filter;

import java.io.IOException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import dev.jcasaslopez.user.enums.TokenType;
import dev.jcasaslopez.user.handler.StandardResponseHandler;
import dev.jcasaslopez.user.service.TokenService;
import dev.jcasaslopez.user.token.TokenValidator;
import dev.jcasaslopez.user.utilities.Constants;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AuthenticationFilter extends OncePerRequestFilter {

	private static final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);

	private StandardResponseHandler standardResponseHandler;
	private TokenService tokenService;
	private TokenValidator tokenValidator;

	public AuthenticationFilter(StandardResponseHandler standardResponseHandler, TokenService tokenService,
			TokenValidator tokenValidator) {
		this.standardResponseHandler = standardResponseHandler;
		this.tokenService = tokenService;
		this.tokenValidator = tokenValidator;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		
		String authHeader = request.getHeader("Authorization");
		String method = request.getMethod();
	    String path = request.getServletPath();

		if (authHeader != null && authHeader.startsWith("Bearer ")) {
			String token = authHeader.substring(7);
			logger.debug("Authorization header found, token extracted");

			// logout
			if ("POST".equalsIgnoreCase(method) && path.equals(Constants.LOGOUT_PATH)) {
				logger.info("Processing logout request");
				tokenService.logOut(token);
				standardResponseHandler.handleResponse(response, 200, "The user has been logged out", null);
				return;
			}
			
			Optional<Claims> optionalClaims = tokenValidator.getValidClaims(token);

			if (optionalClaims.isPresent()) {
			    String purposeStr = optionalClaims.get().get("purpose").toString();
				logger.debug("Token purpose: {}", purposeStr);
			  
			    // Refresh token
			    if ("POST".equalsIgnoreCase(method) && path.equals(Constants.REFRESH_TOKEN_PATH) &&
			        !tokenValidator.isTokenBlacklisted(token) &&
			        purposeStr.equals(TokenType.REFRESH.name())) {
					logger.info("Valid refresh token received");
			        filterChain.doFilter(request, response);
			        return;
			    }

			    // Verification token 
			    if ("POST".equalsIgnoreCase(method) && 
			    		(path.equals(Constants.REGISTRATION_PATH) || path.equals(Constants.FORGOT_PASSWORD_PATH))
			    		&& purposeStr.equals(TokenType.VERIFICATION.name())) {
			    	request.setAttribute("token", token);
					logger.info("Valid verification token received for path: {}", path);
			        filterChain.doFilter(request, response);
			        return;
			    }
			    
			    // Access token
			    if (purposeStr.equals(TokenType.ACCESS.name())) {
			    	logger.info("Valid access token, proceeding with request");
			        filterChain.doFilter(request, response);
			        return;
			    }
			}

			logger.warn("Token did not pass validation or was invalid/blacklisted");
			standardResponseHandler.handleResponse(response, 401, "Token is invalid, expired or blacklisted", null);
			return;
		}
		
		// Si el endpoint no requiere estar autenticado y, por tanto, el encabezado no tenía ningún token.
		//
		// If the endpoint does not required authentication, so there was no token in the header.
		logger.debug("No Authorization header found or token not required for path: {}", path);
		filterChain.doFilter(request, response);
	}
}