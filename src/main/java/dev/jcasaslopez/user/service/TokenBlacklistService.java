package dev.jcasaslopez.user.service;

public interface TokenBlacklistService {

	void blacklistToken(String jti, long expirationInSeconds);
	boolean isTokenBlacklisted(String jti);

}