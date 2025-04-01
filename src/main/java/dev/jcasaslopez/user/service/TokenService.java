package dev.jcasaslopez.user.service;

import dev.jcasaslopez.user.enums.TokenType;
import io.jsonwebtoken.Claims;

public interface TokenService {
	
	String createTokenUserAuthenticated(TokenType tokenType);
	String createTokenUserNotAuthenticated(TokenType tokenType, String username);
	String logOut(String token);
	String getJtiFromToken(String token);
	Claims parseClaims(String token);
	void blacklistToken(String jti, long expirationInSeconds);
	
}
