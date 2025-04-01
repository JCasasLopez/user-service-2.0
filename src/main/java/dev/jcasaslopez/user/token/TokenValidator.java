package dev.jcasaslopez.user.token;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import dev.jcasaslopez.user.service.TokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

public class TokenValidator {
	
	private static final Logger logger = LoggerFactory.getLogger(TokenValidator.class);
	
    private StringRedisTemplate redisTemplate;
    private TokenService tokenService;
    
	public TokenValidator(StringRedisTemplate redisTemplate, TokenService tokenService) {
		this.redisTemplate = redisTemplate;
		this.tokenService = tokenService;
	}

	// Verifica que el token sea técnicamente válido, esté en la whitelist y no en la blacklist.
	// Devuelve las Claims si todo es correcto; de lo contrario, Optional.empty().
	//
	// Verifies that the token is technically valid, whitelisted, and not blacklisted.
	// Returns the Claims if everything is valid; otherwise, Optional.empty().
	public Optional<Claims> isTokenFullyValid(String token) {
	    logger.debug("Validating token...");

	    Optional<Claims> optionalClaims = getValidClaims(token);
	    if (optionalClaims.isEmpty()) {
	        return Optional.empty();
	    }

	    Claims claims = optionalClaims.get();
	    String jti = claims.getId();

	    if (isTokenWhitelisted(jti) && !isTokenBlacklisted(jti)) {
	        return optionalClaims;
	    }

	    return Optional.empty();
	}

	// Parsea y valida las claims del token (firma, expiración, etc.).
	// Devuelve Optional.empty() si el token no es técnicamente válido.
	//
	// Parses and validates the token claims (signature, expiration, etc.).
	// Returns Optional.empty() if the token is not technically valid.
	public Optional<Claims> getValidClaims(String token) {
	    try {
	        Claims claims = tokenService.parseClaims(token);
	        return Optional.of(claims);
	    } catch (JwtException ex) {
	        logger.warn("Token technical validation failed: {}", ex.getMessage());
	        return Optional.empty();
	    }
	}

    // Verifica si el token está en la whitelist (emitido por este servicio).
    //
    // Checks if the token is in the whitelist (issued by this service).
    public boolean isTokenWhitelisted(String jti) {
        boolean result = redisTemplate.hasKey("whitelist:" + jti);
        logger.debug("Whitelist check for jti {}: {}", jti, result);
        return result;
    }
    
    // Verifica si el token ha sido revocado (está en la blacklist).
    //
    // Checks if the token has been revoked (is in the blacklist).
    public boolean isTokenBlacklisted(String jti) {
        boolean result = redisTemplate.hasKey("blacklist:" + jti);
        logger.debug("Blacklist check for jti {}: {}", jti, result);
        return result;
    }

}
