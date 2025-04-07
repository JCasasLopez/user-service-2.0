package dev.jcasaslopez.user.service;

import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import dev.jcasaslopez.user.enums.TokenType;
import dev.jcasaslopez.user.model.TokensLifetimes;
import dev.jcasaslopez.user.token.TokenValidator;
import dev.jcasaslopez.user.utilities.Constants;
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

	private TokensLifetimes tokensLifetimes;
    private StringRedisTemplate redisTemplate;
    private TokenValidator tokenValidator;

	public TokenServiceImpl(TokensLifetimes tokensLifetimes, StringRedisTemplate redisTemplate,
			TokenValidator tokenValidator) {
		this.tokensLifetimes = tokensLifetimes;
		this.redisTemplate = redisTemplate;
		this.tokenValidator = tokenValidator;
	}

	@PostConstruct
	public void init() {
		keyBytes = Base64.getDecoder().decode(secretKey);
		key = Keys.hmacShaKeyFor(keyBytes);
		logger.info("TokenService initialized with decoded secret key");
	}
	
	// Se usa tanto para tokens de acceso como para los de refrescado.
	//
    // Used both for access and refreshed tokens.
	@Override
	public String createAuthToken(TokenType tokenType) {
		logger.info("Creating token type: {}", tokenType);
		
		// tokensLifetimes.getTokensLifetimes() -> Map<TokenType, Integer>.
		int expirationInMilliseconds = tokensLifetimes.getTokensLifetimes().get(tokenType) * 60 * 1000;		
		Authentication authenticated = SecurityContextHolder.getContext().getAuthentication();
		
		String jti = UUID.randomUUID().toString();
		logger.debug("Authenticated user: {}, JTI: {}", authenticated.getName(), jti);
		
		String token = Jwts.builder().header().type("JWT").and().subject(authenticated.getName())
				.id(jti)
				.claim("roles",
						authenticated.getAuthorities().stream().map(GrantedAuthority::getAuthority)
								.collect(Collectors.toList()))
				.claim("purpose", tokenType)
				.issuedAt(new Date(System.currentTimeMillis()))
				.expiration(new Date(System.currentTimeMillis() + expirationInMilliseconds)).signWith(key, Jwts.SIG.HS256)
				.compact();
		
		logger.info("Token issued successfully for user: {}", authenticated.getName());
		return token;
	}
	
	@Override
	public String createVerificationToken() {
		logger.debug("Creating verification token");
	
		int expirationInMilliseconds = tokensLifetimes.getTokensLifetimes().get(TokenType.VERIFICATION) * 60 * 1000;		
		String jti = UUID.randomUUID().toString();		
		String token = Jwts.builder().header().type("JWT").and().subject(null)
				.id(jti)
				.claim("purpose", TokenType.VERIFICATION)
				.issuedAt(new Date(System.currentTimeMillis()))
				.expiration(new Date(System.currentTimeMillis() + expirationInMilliseconds)).signWith(key, Jwts.SIG.HS256)
				.compact();
		
		logger.debug("Verification token issued successfully. jti: {}", jti);
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
	public void logOut(String token) {
		logger.info("Processing logout...");
		
		Optional<Claims> optionalClaims = tokenValidator.getValidClaims(token);
		String tokenJti = getJtiFromToken(token);
		
		if(optionalClaims.isPresent()) {
		    Date expirationTime = optionalClaims.get().getExpiration();
		    Date currentTime = new Date(System.currentTimeMillis());
		    long remainingMillis = expirationTime.getTime() - currentTime.getTime();
		    // Asegura al menos 1 segundo para evitar que Redis rechace TTL cero o negativo 
		    // (redondea hacia abajo).
		    //
		    // Ensure at least 1 second to avoid Redis rejecting zero/negative TTL (rounds down).
		    long expirationInSeconds = Math.max(1, remainingMillis / 1000); 
			logger.debug("Blacklisting token with JTI {} for {} seconds", tokenJti, expirationInSeconds);
		 	String tokenRedisKey = Constants.REFRESH_TOKEN_REDIS_KEY + tokenJti;
			blacklistToken(tokenRedisKey, expirationInSeconds);
		}
		
		// Si el token no es válido no hay que revocarlo, y se continúa con el logout igualmente.
		//
		// If the token is not valid, does not have to revoked, and we continue with the logout process.
	    SecurityContextHolder.getContext().setAuthentication(null);
		logger.info("User has been logged out");
	}
	
	@Override
	public String getJtiFromToken(String token) {
		String jti = parseClaims(token).getId();
		logger.debug("Extracted JTI from token: {}", jti);
		return jti;
	}
	
	@Override
	public void blacklistToken(String redisKey, long expirationInSeconds) {
        logger.info("Blacklisting token with key: {} for {} seconds", redisKey, expirationInSeconds);
        redisTemplate.opsForValue().set(redisKey, "blacklisted", expirationInSeconds, TimeUnit.SECONDS);
    }		
}