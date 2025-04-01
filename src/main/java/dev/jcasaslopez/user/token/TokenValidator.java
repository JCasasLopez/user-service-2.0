package dev.jcasaslopez.user.token;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import dev.jcasaslopez.user.service.TokenService;
import io.jsonwebtoken.JwtException;

public class TokenValidator {
	
	private static final Logger logger = LoggerFactory.getLogger(TokenValidator.class);
	
    private StringRedisTemplate redisTemplate;
    private TokenService tokenService;
    
	public TokenValidator(StringRedisTemplate redisTemplate, TokenService tokenService) {
		this.redisTemplate = redisTemplate;
		this.tokenService = tokenService;
	}

	public boolean isTokenFullyValid(String token) {
	    logger.debug("Validating token...");

	    if (!isTokenTechnicallyValid(token)) {
	        return false;
	    }
	    String jti = tokenService.getJtiFromToken(token);
	    return isTokenWhitelisted(jti) && !isTokenBlacklisted(jti);
	}

    // Verifica la validez técnica del token (firma, expiración, etc.).
    // Throws JwtException si el token no es válido.
    //
    // Checks the token's technical validity (signature, expiration, etc.).
    // Throws JwtException if the token is invalid.
    public boolean isTokenTechnicallyValid(String token) {
        try {
        	tokenService.parseClaims(token);
            return true;
        } catch (JwtException ex) {
            logger.warn("Token technical validation failed: {}", ex.getMessage());
            return false;
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
