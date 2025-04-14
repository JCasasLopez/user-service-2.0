package dev.jcasaslopez.user.service;

import dev.jcasaslopez.user.entity.User;
import dev.jcasaslopez.user.enums.LoginFailureReason;

public interface LoginAttemptService {

	void recordAttempt(boolean successful, String ipAddress, LoginFailureReason reason,
			User user);

}