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
import dev.jcasaslopez.user.utilities.Constants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class TokenServiceImpl implements TokenService {

    private static final Logger logger = LoggerFactory.getLogger(TokenServiceImpl.class);

    private final TokensLifetimes tokensLifetimes;
    private final StringRedisTemplate redisTemplate;
    private final SecretKey key;

    public TokenServiceImpl(TokensLifetimes tokensLifetimes, 
                            StringRedisTemplate redisTemplate, 
                            @Value("${jwt.secretKey}") String base64SecretKey) {
        this.tokensLifetimes = tokensLifetimes;
        this.redisTemplate = redisTemplate;

        byte[] keyBytes = Base64.getDecoder().decode(base64SecretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        logger.info("TokenService initialized with decoded secret key");
    }

	// Creates an authentication token (ACCESS or REFRESH) for an authenticated user.
	// Uses the security context to retrieve the current user and their roles.
	// Kept separate from verification token logic to avoid mixing different concerns.
	@Override
	public String createAuthToken(TokenType tokenType) {
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
	
	// Creates a verification token (e.g., for account activation or password reset).
	// Does not require a security context: only the username is needed.
	// Separated from authentication tokens to keep the logic simple and purpose-specific.
	@Override
	public String createVerificationToken(String username) {
		int expirationInMilliseconds = tokensLifetimes.getTokensLifetimes().get(TokenType.VERIFICATION) * 60 * 1000;		
		String jti = UUID.randomUUID().toString();	
		
		String token = Jwts.builder().header().type("JWT").and().subject(username)
				.id(jti)
				.claim("purpose", TokenType.VERIFICATION)
				.issuedAt(new Date(System.currentTimeMillis()))
				.expiration(new Date(System.currentTimeMillis() + expirationInMilliseconds)).signWith(key, Jwts.SIG.HS256)
				.compact();
		
		logger.debug("Verification token issued successfully. jti: {}", jti);
		return token;
	}
	
	@Override
	public Claims parseClaims(String token) {
		try {
			// Configures how we want to verify the token.
			return Jwts.parser()
			
					// Sets the key that will be used to verify the signature.
					.verifyWith(key) 
					
					// Builds the JWT parser with the specified configuration.
					.build() 	
					
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
	
	// Parses and validates the token claims (signature, expiration, etc.).
	// Returns Optional.empty() if the token is not technically valid.
	@Override
	public Optional<Claims> getValidClaims(String token) {
		try {
			Claims claims = parseClaims(token);
			return Optional.of(claims);
		} catch (JwtException ex) {
			return Optional.empty();
		}
	}
	
	@Override
	public String getJtiFromToken(String token) {
		String jti = parseClaims(token).getId();
		return jti;
	}
	
	@Override
	public void logOut(String token) {
		logger.info("Processing logout...");
		
		Optional<Claims> optionalClaims = getValidClaims(token);
		String tokenJti = getJtiFromToken(token);
		
		// If the optional is empty, a JwtException is thrown in getValidClaims(), and the
		// flow would not reach this point.
		Date expirationTime = optionalClaims.get().getExpiration();
		Date currentTime = new Date(System.currentTimeMillis());
		long remainingMillis = expirationTime.getTime() - currentTime.getTime();

		// Ensure at least 1 second to avoid Redis rejecting zero/negative TTL (rounds down).
		long expirationInSeconds = Math.max(1, remainingMillis / 1000);
		logger.debug("Blacklisting token with JTI {} for {} seconds", tokenJti, expirationInSeconds);
		String tokenRedisKey = Constants.REFRESH_TOKEN_REDIS_KEY + tokenJti;
		blacklistToken(tokenRedisKey, expirationInSeconds);

		// Instead of setting the authentication to null, we use clearContext()
		// to completely remove the SecurityContext from the current thread. This is crucial
		// to prevent residual authentication data from persisting in reused threads,
		// which could lead to unexpected behaviors in concurrent environments or during testing.
		// Reference: https://master-spring-ter.medium.com/understanding-clearcontext-in-spring-security-enhancing-application-security-17407ea55b4d
		SecurityContextHolder.clearContext();
		logger.info("User has been logged out");
	}
	
	@Override
	public void blacklistToken(String redisKey, long expirationInSeconds) {
        logger.info("Blacklisting token with key: {} for {} seconds", redisKey, expirationInSeconds);
        redisTemplate.opsForValue().set(redisKey, "blacklisted", expirationInSeconds, TimeUnit.SECONDS);
    }		
	
	@Override
	public boolean isTokenBlacklisted(String token) {
		String tokenJti = getJtiFromToken(token);
		String redisKey = Constants.REFRESH_TOKEN_REDIS_KEY + tokenJti;
		String redisValue = redisTemplate.opsForValue().get(redisKey);
		
		boolean result;
		if ("blacklisted".equals(redisValue)) {
		    result = true;
		} else {
			
			// Programs flow would reach this point also if no Redis entry is found (redisValue == null).
		    result = false;
		}
		
		logger.debug("Blacklist check for jti {}: {}", tokenJti, result);
		return result;
	}
}