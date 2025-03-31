package dev.jcasaslopez.user.service;

import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import dev.jcasaslopez.user.enums.TokenType;
import dev.jcasaslopez.user.model.TokensLifetimes;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

@Service
public class TokenServiceImpl implements TokenService {
	
	private static final Logger logger = LoggerFactory.getLogger(TokenServiceImpl.class);

	@Value("${jwt.secretKey}")
	private String secretKey;

	private byte[] keyBytes;
	private SecretKey key;

	@Autowired
	private TokensLifetimes tokensLifetimes;

	@Autowired
	private TokenBlacklistService tokenBlacklistService;
	
	@PostConstruct
	public void init() {
		keyBytes = Base64.getDecoder().decode(secretKey);
		key = Keys.hmacShaKeyFor(keyBytes);
		logger.info("TokenService initialized with decoded secret key");
	}
    
	@Override
	public String createTokenUserAuthenticated(TokenType tokenType) {
		logger.info("Creating token for type: {}", tokenType);
		
		// tokensLifetimes.getTokensLifetimes() devuelve un Map<TokenType, Integer>.
		//
		// tokensLifetimes.getTokensLifetimes() returns a Map<TokenType, Integer>.
		int expirationInMilliseconds = tokensLifetimes.getTokensLifetimes().get(tokenType) * 60 * 1000;		
		Authentication authenticated = SecurityContextHolder.getContext().getAuthentication();
		
		// ID único que identifica el token (más práctico que el token completo).
		//
		// Unique ID to identify the token (more practical than the token itself).
		String jti = UUID.randomUUID().toString();
		logger.debug("Authenticated user: {}, JTI: {}", authenticated.getName(), jti);
		
		String token = Jwts.builder().header().type("JWT").and().subject(authenticated.getName())
				.id(jti)
				.claim("roles",
						authenticated.getAuthorities().stream().map(GrantedAuthority::getAuthority)
								.collect(Collectors.toList()))
				.issuedAt(new Date(System.currentTimeMillis()))
				.expiration(new Date(System.currentTimeMillis() + expirationInMilliseconds)).signWith(key, Jwts.SIG.HS256)
				.compact();
		
		logger.info("Token created successfully for user: {}", authenticated.getName());
		return token;
	}
	
	@Override
	public String createTokenUserNotAuthenticated(TokenType tokenType, String username) {
		logger.info("Creating token for type: {}", tokenType);
	
		int expirationInMilliseconds = tokensLifetimes.getTokensLifetimes().get(tokenType) * 60 * 1000;		
		String jti = UUID.randomUUID().toString();
		logger.debug("Authenticated user: {}, JTI: {}", username, jti);
		
		String token = Jwts.builder().header().type("JWT").and().subject(username)
				.id(jti)
				.issuedAt(new Date(System.currentTimeMillis()))
				.expiration(new Date(System.currentTimeMillis() + expirationInMilliseconds)).signWith(key, Jwts.SIG.HS256)
				.compact();
		
		logger.info("Token created successfully for user: {}", username);
		return token;
	}

	public Claims parseClaims(String token) {
		try {
			logger.debug("Parsing token claims...");
			
			// Configura cómo queremos verificar el token.
			//
			// Configures how we want to verify the token.
			return Jwts.parser()
					// Establece la clave que se usará para verificar la firma.
					//
					// Sets the key that will be used to verify the signature.
					.verifyWith(key) 
					// Construye el parser JWT con la configuración especificada.
					//
					// Builds the JWT parser with the specified configuration.
					.build() 	
					// Aquí es donde ocurren todas las verificaciones.
					//
					// This is where all verifications happen.
					.parseSignedClaims(token)
					.getPayload();

		} catch (JwtException ex) {
			if (ex instanceof io.jsonwebtoken.ExpiredJwtException) {
				logger.warn("Token has expired");
				throw new JwtException("Expired token");
			} else if (ex instanceof io.jsonwebtoken.MalformedJwtException) {
				logger.error("Malformed token");
				throw new JwtException("Malformed token");
			} else if (ex instanceof io.jsonwebtoken.security.SecurityException) {
				logger.error("Invalid token signature");
				throw new JwtException("Invalid signature");
			}
			logger.error("Error verifying the token: {}", ex.getMessage());
			throw new JwtException("Error verifying the token: " + ex.getMessage());
		}
	} 
	
	@Override
	public String logOut(String token) {
		logger.info("Processing logout...");

		if (tokenBlacklistService.isTokenBlacklisted(getJtiFromToken(token))) {
	        logger.info("Token already blacklisted");
	        return "The user is already logged out";
	    }

	    if (!isTokenValid(token)) {
	        logger.info("Token is not valid (probably expired)");
	        return "The session has expired";
	    }

	    String jti = getJtiFromToken(token);
	    Date expirationTime = parseClaims(token).getExpiration();
	    Date currentTime = new Date(System.currentTimeMillis());
	    long remainingMillis = expirationTime.getTime() - currentTime.getTime();
	    // Asegura al menos 1 segundo para evitar que Redis rechace TTL cero o negativo 
	    // (redondea hacia abajo).
	    //
	    // Ensure at least 1 second to avoid Redis rejecting zero/negative TTL (rounds down).
	    long expirationInSeconds = Math.max(1, remainingMillis / 1000); 
		logger.debug("Blacklisting token with JTI {} for {} seconds", jti, expirationInSeconds);

	 	tokenBlacklistService.blacklistToken(jti, expirationInSeconds);

	    SecurityContextHolder.getContext().setAuthentication(null);
		logger.info("User has been logged out");
	    return "The user has logged out";
	}

	@Override
	public boolean isTokenValid(String token) {
		try {
			// Si este método no lanza excepción, el token es válido.
			//
			// If this method does not throw an exception, then the token is valid.
			logger.debug("Validating token...");
			parseClaims(token); 
			logger.debug("Token is valid");
			return true;
		} catch (JwtException ex) {
			logger.warn("Token validation failed: {}", ex.getMessage());
			throw ex; 
		}
	}

	@Override
	public String getJtiFromToken(String token) {
		String jti = parseClaims(token).getId();
		logger.debug("Extracted JTI from token: {}", jti);
		return jti;
	}
	
}
