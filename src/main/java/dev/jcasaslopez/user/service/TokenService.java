package dev.jcasaslopez.user.service;

import java.util.Optional;

import dev.jcasaslopez.user.enums.TokenType;
import io.jsonwebtoken.Claims;

public interface TokenService {
	
	// Creación de tokens.
	//
	// Token creation.
	String createAuthToken(TokenType tokenType);
	String createVerificationToken(String username);
	
	// Extracción y Validación de tokens.
	//
	// Claims & token parsing.
	Claims parseClaims(String token);
	Optional<Claims> getValidClaims(String token);
	String getJtiFromToken(String token);

	// Revocado de tokens y Logout.
	//
	// Token blacklisting & Logout 
	void logOut(String token);
	void blacklistToken(String redisKey, long expirationInSeconds);
	boolean isTokenBlacklisted(String token);
	
}