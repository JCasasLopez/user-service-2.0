package dev.jcasaslopez.user.security.filter;

import java.io.IOException;
import java.util.Date;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import dev.jcasaslopez.user.entity.User;
import dev.jcasaslopez.user.enums.TokenType;
import dev.jcasaslopez.user.handler.StandardResponseHandler;
import dev.jcasaslopez.user.mapper.UserMapper;
import dev.jcasaslopez.user.repository.UserRepository;
import dev.jcasaslopez.user.security.CustomUserDetails;
import dev.jcasaslopez.user.service.TokenService;
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
	private UserRepository userRepository;
	private UserMapper userMapper;

	public AuthenticationFilter(StandardResponseHandler standardResponseHandler, TokenService tokenService,
			UserRepository userRepository, UserMapper userMapper) {
		this.standardResponseHandler = standardResponseHandler;
		this.tokenService = tokenService;
		this.userRepository = userRepository;
		this.userMapper = userMapper;
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

			// Verifies that the token is technically valid.
			Optional<Claims> optionalClaims = tokenService.getValidClaims(token);
			if(optionalClaims.isEmpty()) {
				logger.warn("Invalid or malformed token");
				standardResponseHandler.handleResponse(response, 401, "Token is invalid, expired or malformed", null);
				return;
			}

			Claims claims = optionalClaims.get();
			String username = claims.getSubject();
			String purposeStr = String.valueOf(claims.get("purpose"));

			// Logout
			// You have to receive the refresh token, not the access one.
			if ("POST".equalsIgnoreCase(method) && path.equals(Constants.LOGOUT_PATH)
					&& !tokenService.isTokenBlacklisted(token) && purposeStr.equals(TokenType.REFRESH.name())) {
				logger.info("Processing logout request");
				tokenService.logOut(token);
				standardResponseHandler.handleResponse(response, 200, "The user has been logged out", null);
				return;
			}

			// Refresh token
			if ("POST".equalsIgnoreCase(method) && path.equals(Constants.REFRESH_TOKEN_PATH)
					&& !tokenService.isTokenBlacklisted(token) && purposeStr.equals(TokenType.REFRESH.name())) {
				authenticateUser(token, username);

				// We have to revoke the token, since the system will issue a new one
				// in the next step (controller -> AccountOrchestrationService).
				String redisKey = Constants.REFRESH_TOKEN_REDIS_KEY + tokenService.getJtiFromToken(token);
				Date expirationTime = tokenService.parseClaims(token).getExpiration();
				Date currentTime = new Date(System.currentTimeMillis());
				long remainingMillis = expirationTime.getTime() - currentTime.getTime();
				tokenService.blacklistToken(redisKey, remainingMillis);
				logger.debug("Old refresh token revoked successfully");

				filterChain.doFilter(request, response);
				return;
			}

			// Verification token
			if (purposeStr.equals(TokenType.VERIFICATION.name())
					&& ("POST".equalsIgnoreCase(method) && path.equals(Constants.REGISTRATION_PATH)
						|| "PUT".equalsIgnoreCase(method) && path.equals(Constants.RESET_PASSWORD_PATH))) {
				request.setAttribute("token", token);
				logger.info("Valid verification token received for path: {}", path);

				filterChain.doFilter(request, response);
				return;
			}

			// Access token
			// The access token is valid for all endpoints that require authentication
			// except for 'refresh token' endpoint.
			if (!path.equals(Constants.REFRESH_TOKEN_PATH) && purposeStr.equals(TokenType.ACCESS.name())) {
				authenticateUser(token, username);
				filterChain.doFilter(request, response);
				return;
			}

			standardResponseHandler.handleResponse(response, 401, "Token is invalid, expired or blacklisted", null);
			return;
		}

		// If the endpoint does not required authentication, so there was no token in the header.
		logger.debug("No Authorization header found or token not required for path: {}", path);
		filterChain.doFilter(request, response);
	}

	public void authenticateUser(String token, String username) {
		User userJpa = userRepository.findByUsername(username)
				.orElseThrow(() -> new UsernameNotFoundException(username));
		CustomUserDetails user = userMapper.userToCustomUserDetailsMapper(userJpa);
		Authentication authentication = new UsernamePasswordAuthenticationToken(user, token, user.getAuthorities());
		SecurityContextHolder.getContext().setAuthentication(authentication);
		logger.info("Valid access token. User {} authenticated successfully", username);
	}
}