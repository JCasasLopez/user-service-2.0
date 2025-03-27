package dev.jcasaslopez.user.service;

import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

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

@Service
public class TokenServiceImpl implements TokenService {
	
	@Value("${jwt.secretKey}") 
	private String secretKey;
	
	byte[] keyBytes = Base64.getDecoder().decode(secretKey);
    SecretKey key = Keys.hmacShaKeyFor(keyBytes);
    
    @Autowired
    private TokensLifetimes tokensLifetimes;
    
    @Autowired
    private TokenBlacklistService tokenBlacklistService;

	@Override
	public String createToken(TokenType tokenType) {
		// tokensLifetimes.getTokensLifetimes() devuelve un Map<TokenType, Integer>.
		//
		// tokensLifetimes.getTokensLifetimes() returns a Map<TokenType, Integer>.
		int expirationInMilliseconds = tokensLifetimes.getTokensLifetimes().get(tokenType) * 60 * 1000;		
		Authentication authenticated = SecurityContextHolder.getContext().getAuthentication();
		
		// ID único que identifica el token (más práctico que el token completo).
		//
		// Unique ID to identify the token (more practical than the token itself).
		String jti = UUID.randomUUID().toString();
		
		String token = Jwts.builder().header().type("JWT").and().subject(authenticated.getName())
				.id(jti)
				.claim("roles",
						authenticated.getAuthorities().stream().map(GrantedAuthority::getAuthority)
								.collect(Collectors.toList()))
				.issuedAt(new Date(System.currentTimeMillis()))
				.expiration(new Date(System.currentTimeMillis() + expirationInMilliseconds)).signWith(key, Jwts.SIG.HS256)
				.compact();
		
		return token;
	}

	private Claims parseClaims(String token) {
		try {
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
				throw new JwtException("Expired token");
			} else if (ex instanceof io.jsonwebtoken.MalformedJwtException) {
				throw new JwtException("Malformed token");
			} else if (ex instanceof io.jsonwebtoken.security.SecurityException) {
				throw new JwtException("Invalid signature");
			}
			throw new JwtException("Error verifying the token: " + ex.getMessage());
		}
	} 
	
	@Override
	public String logOut(String token) {
		if (tokenBlacklistService.isTokenBlacklisted(getJtiFromToken(token))) {
	        return "The user is already logged out";
	    }

	    if (!isTokenValid(token)) {
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
	 	tokenBlacklistService.blacklistToken(jti, expirationInSeconds);

	    SecurityContextHolder.getContext().setAuthentication(null);
	    return "The user has logged out";
	}

	@Override
	public boolean isTokenValid(String token) {
		try {
			// Si este método no lanza excepción, el token es válido.
			//
			// If this method does not throw an exception, then the token is valid.
			parseClaims(token); 
			return true;
		} catch (JwtException ex) {
			throw ex; 
		}
	}

	@Override
	public String getJtiFromToken(String token) {
		return parseClaims(token).getId();
	}
	
}
