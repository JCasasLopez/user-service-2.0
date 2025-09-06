package dev.jcasaslopez.user.service;

import java.io.IOException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import dev.jcasaslopez.user.dto.AuthenticationRequest;
import dev.jcasaslopez.user.entity.User;
import dev.jcasaslopez.user.enums.TokenType;
import dev.jcasaslopez.user.mapper.UserMapper;
import dev.jcasaslopez.user.repository.UserRepository;
import dev.jcasaslopez.user.security.CustomUserDetails;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {
	
	private static final Logger logger = LoggerFactory.getLogger(AuthenticationServiceImpl.class);
	
	private final UserRepository userRepository;
	private final UserMapper userMapper;
	private final TokenService tokenService;
	
	public AuthenticationServiceImpl(UserRepository userRepository, UserMapper userMapper, TokenService tokenService) {
		this.userRepository = userRepository;
		this.userMapper = userMapper;
		this.tokenService = tokenService;
	}
	
	@Override
	public boolean verifyHeaderIsValid(HttpServletRequest request) {
		String authHeader = request.getHeader("Authorization");
		return !(authHeader == null || !authHeader.startsWith("Bearer "));
	}

	// 3 different cases:
	// - Token is present, but is not valid -> Returns empty Optional.
	// - Token is present and valid -> Returns Optional<AuthenticationRequest>.
	//   AuthenticationRequest is a DTO class that contains the path, method (POST, PUT...) and token included 
	//   in the HTTP request, and also the username and token purpose from the token claims. 
	// - I/O error writing HTTP response -> IOException propagated to filter (handled by servlet container).
	@Override
	public Optional<AuthenticationRequest> parseAuthenticationRequest(HttpServletRequest request, 
			HttpServletResponse response) throws IOException {
		String authHeader = request.getHeader("Authorization");
		String method = request.getMethod();
		String path = request.getServletPath();
		String token = authHeader.substring(7);
		logger.debug("Token extracted");

		// Verifies that the token is technically valid.
		Optional<Claims> optionalClaims = tokenService.getValidClaims(token);
		if (optionalClaims.isEmpty()) {
			return Optional.empty();
		}
		
		Claims claims = optionalClaims.get();
		String username = claims.getSubject();

		// JWT claims are stored as primitive types during serialization.
		// The enum value was stored as a string and needs to be converted back.
		TokenType purpose = TokenType.valueOf(claims.get("purpose", String.class));
		return Optional.of(new AuthenticationRequest(username, token, purpose, path, method));	
		
	}

	@Override
	public void authenticateUser(String token, String username) {
		User userJpa = userRepository.findByUsername(username)
				.orElseThrow(() -> new UsernameNotFoundException(username));
		CustomUserDetails user = userMapper.userToCustomUserDetailsMapper(userJpa);
		Authentication authentication = new UsernamePasswordAuthenticationToken(user, token, user.getAuthorities());
		SecurityContextHolder.getContext().setAuthentication(authentication);
		logger.info("Valid access token. User {} authenticated successfully", username);
	}

}
