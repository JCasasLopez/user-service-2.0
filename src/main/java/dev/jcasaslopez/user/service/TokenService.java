package dev.jcasaslopez.user.service;

import java.util.Optional;

import dev.jcasaslopez.user.enums.TokenType;
import io.jsonwebtoken.Claims;

public interface TokenService {

	String createAuthToken(TokenType tokenType);
	String createVerificationToken();
	Claims parseClaims(String token);
	void logOut(String token);
	String getJtiFromToken(String token);
	void blacklistToken(String redisKey, long expirationInSeconds);
	Optional<Claims> getValidClaims(String token);
	boolean isTokenBlacklisted(String token);

}