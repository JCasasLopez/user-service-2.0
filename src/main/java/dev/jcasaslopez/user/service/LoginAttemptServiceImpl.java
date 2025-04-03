package dev.jcasaslopez.user.service;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import dev.jcasaslopez.user.entity.LoginAttempt;
import dev.jcasaslopez.user.enums.LoginFailureReason;
import dev.jcasaslopez.user.repository.LoginAttemptRepository;
import dev.jcasaslopez.user.security.handler.CustomAuthenticationSuccessHandler;

@Service
public class LoginAttemptServiceImpl implements LoginAttemptService {
	
    private static final Logger logger = LoggerFactory.getLogger(CustomAuthenticationSuccessHandler.class);

    private final LoginAttemptRepository loginAttemptRepository;

    public LoginAttemptServiceImpl(LoginAttemptRepository loginAttemptRepository) {
        this.loginAttemptRepository = loginAttemptRepository;
    }

    @Override
	public void recordAttempt(boolean successful, String ipAddress, LoginFailureReason reason) {
        try {
            LoginAttempt attempt = new LoginAttempt(LocalDateTime.now(), successful, ipAddress, reason);
            loginAttemptRepository.save(attempt);
        } catch (Exception e) {
            logger.warn("Failed to persist login attempt: {}", e.getMessage());
        }
    }
}

