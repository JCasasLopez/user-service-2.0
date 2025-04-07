package dev.jcasaslopez.user.service;

import dev.jcasaslopez.user.enums.TokenType;
import io.jsonwebtoken.Claims;

public interface TokenService {
	
	String createAuthToken(TokenType tokenType);
	String createVerificationToken();
	void logOut(String token);
	String getJtiFromToken(String token);
	Claims parseClaims(String token);
	void blacklistToken(String jti, long expirationInSeconds);
	
}
