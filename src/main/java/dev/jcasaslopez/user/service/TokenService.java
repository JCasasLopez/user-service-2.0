package dev.jcasaslopez.user.service;

import java.util.Optional;

import dev.jcasaslopez.user.enums.TokenType;
import io.jsonwebtoken.Claims;

public interface TokenService {
	
	
	// Token creation.
	String createAuthToken(TokenType tokenType);
	String createVerificationToken(String username);
	
	// Claims & token parsing.
	Claims parseClaims(String token);
	Optional<Claims> getValidClaims(String token);
	String getJtiFromToken(String token);

	// Token blacklisting & Logout 
	void logOut(String token);
	void blacklistToken(String redisKey, long expirationInSeconds);
	boolean isTokenBlacklisted(String token);
	
}