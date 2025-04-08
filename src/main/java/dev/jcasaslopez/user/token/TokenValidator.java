package dev.jcasaslopez.user.token;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import dev.jcasaslopez.user.service.TokenService;
import dev.jcasaslopez.user.utilities.Constants;
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

    // Verifica si el token ha sido revocado (está en la blacklist).
    //
    // Checks if the token has been revoked (is in the blacklist).
    public boolean isTokenBlacklisted(String token) {
    	String tokenJti = tokenService.getJtiFromToken(token);
    	String redisKey = Constants.REFRESH_TOKEN_REDIS_KEY + tokenJti;
        String redisValue = redisTemplate.opsForValue().get(redisKey);
        boolean result;
        if(redisValue.equals("blacklisted")) {
        	result = true;
        }
        // Incluye el caso de que no se encuentre la entrada en Redis (redisValue == null).
        //
        // Programs flow would reach this point also if no Redis entry is found (redisValue == null).
        result = false;
        logger.debug("Blacklist check for jti {}: {}", tokenJti, result);
        return result;
    }

}
