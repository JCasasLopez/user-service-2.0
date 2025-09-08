package dev.jcasaslopez.user.service;

import dev.jcasaslopez.user.entity.User;

public interface AccountLockingService {
	
	int getLoginAttemptsRedisEntry(String username);
	void deleteLoginAttemptsRedisEntry(String username);
	void setLoginAttemptsRedisEntry(String usermame, int loginAttempts, int accountLockDurationInSeconds);
	void blockAccount(User user);
	void unBlockAccount(User user);

}
