package dev.jcasaslopez.user.security.filter;

import java.io.IOException;
import java.util.Date;
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
					
					// Tenemos que revocar el token, ya que se va a emitir otro nuevo 
					// en el siguiente paso (controller -> AccountOrchestrationService).
					//
					// We have to revoke the token, since the system will issue a new one
					// in the next step (controller -> AccountOrchestrationService).
					String redisKey = Constants.REFRESH_TOKEN_REDIS_KEY + tokenService.getJtiFromToken(token);					
					Date expirationTime = tokenService.parseClaims(token).getExpiration();
					Date currentTime = new Date(System.currentTimeMillis());
				    long remainingMillis = expirationTime.getTime() - currentTime.getTime();
					tokenService.blacklistToken(redisKey, remainingMillis);
					logger.info("Old refresh token revoked successfully");
					
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