package dev.jcasaslopez.user.service;

import dev.jcasaslopez.user.enums.TokenType;

public interface TokenService {
	
	String createToken(TokenType tokenType);
	boolean isTokenValid(String token);
	String logOut(String token);
	String getJtiFromToken(String token);
	
}
