package dev.jcasaslopez.user.service;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.jcasaslopez.user.enums.TokenType;
import dev.jcasaslopez.user.event.ChangePasswordEvent;
import dev.jcasaslopez.user.event.CreateAccountEvent;
import dev.jcasaslopez.user.event.ForgotPasswordEvent;
import dev.jcasaslopez.user.event.ResetPasswordEvent;
import dev.jcasaslopez.user.event.UpdateAccountStatusEvent;
import dev.jcasaslopez.user.event.VerifyEmailEvent;
import dev.jcasaslopez.user.model.TokensLifetimes;

@Service
public class NotificationServiceImpl implements NotificationService {
	
	private static final Logger logger = LoggerFactory.getLogger(NotificationServiceImpl.class);

	String urlAngular = "http://classrooms.com/user-service/";

	private EmailService emailService;
    private StringRedisTemplate redisTemplate;
    private TokenService tokenService;
    private TokensLifetimes tokensLifetimes;
    private ObjectMapper objectMapper;
    private PasswordEncoder passwordEncoder;
    
	public NotificationServiceImpl(EmailService emailService, StringRedisTemplate redisTemplate, TokenService tokenService,
			TokensLifetimes tokensLifetimes, ObjectMapper objectMapper, PasswordEncoder passwordEncoder) {
		this.emailService = emailService;
		this.redisTemplate = redisTemplate;
		this.tokenService = tokenService;
		this.tokensLifetimes = tokensLifetimes;
		this.objectMapper = objectMapper;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	@EventListener
    public void handleVerifyEmail(VerifyEmailEvent event) throws JsonProcessingException {
		logger.info("Starting email verification flow for user: {}", event.getUser().getUsername()); 

        String subject = "Email verification";
        String message = "Hi " + event.getUser().getUsername() + ",\n\n"
                + "Thank you for registering. To complete your account setup, "
                + "please verify your email address by copying and pasting the next link:\n\n"
                + urlAngular + "/verifyEmail" + "?token=" + event.getToken()
                + "Best regards,\n"
                + "The Team";
        
        String tokenJti = tokenService.getJtiFromToken(event.getToken());
        long expirationInSeconds = tokensLifetimes.getTokensLifetimes().get(TokenType.VERIFY_EMAIL) * 60;
        
        // UserDto (codificar la contraseña ANTES de subir a Redis).
        //
        // UserDto (encode password BEFORE saving in Redis).
        String encodedPassword = passwordEncoder.encode(event.getUser().getPassword());
        event.getUser().setPassword(encodedPassword);
        String userJson = objectMapper.writeValueAsString(event.getUser());

        logger.debug("Storing token JTI {} in Redis with TTL {} seconds", tokenJti, expirationInSeconds);
        redisTemplate.opsForValue().set(tokenJti, userJson, expirationInSeconds, TimeUnit.SECONDS);
        
        emailService.sendEmail(event.getUser().getEmail(), subject, message);
        logger.info("Verification email sent to {}", event.getUser().getEmail()); 
    }
	
	@Override
	@EventListener
    public void handleCreateAccount(CreateAccountEvent event) {
        String subject = "Welcome to the platform!";
        String message = "Hi " + event.getUser().getUsername() + ",\n\n"
                + "Your account has been created successfully"
                + "Best regards,\n"
                + "The Team";
        emailService.sendEmail(event.getUser().getEmail(), subject, message);
        logger.info("Account created successfully email sent to {}", event.getUser().getEmail()); 
	}
	
	@Override
	@EventListener
    public void handleForgotPassword(ForgotPasswordEvent event) throws JsonProcessingException {
		logger.info("Starting password reset flow for user: {}", event.getUser().getUsername()); 

		String subject = "Password reset verification email";
        String message = "Hi " + event.getUser().getUsername() + ",\n\n"
                + "Copy and past the next link to reset your password:\n\n"
                + urlAngular + "/forgotPassword" + "?token=" + event.getToken()
                + "Best regards,\n"
                + "The Team";
        String tokenJti = tokenService.getJtiFromToken(event.getToken());
        long expirationInSeconds = tokensLifetimes.getTokensLifetimes().get(TokenType.PASSWORD_RESET) * 60;
        
        String userJson = objectMapper.writeValueAsString(event.getUser());
        logger.debug("Storing token JTI {} in Redis with TTL {} seconds", tokenJti, expirationInSeconds);
        redisTemplate.opsForValue().set(tokenJti, userJson, expirationInSeconds, TimeUnit.SECONDS);
        
        emailService.sendEmail(event.getUser().getEmail(), subject, message);
        logger.info("Password reset email sent to {}", event.getUser().getEmail()); 
	}
	
	@Override
	@EventListener
    public void handleResetPassword(ResetPasswordEvent event) {
		String subject = "Password reset successfully";
		String message = "Hi " + event.getUser().getUsername() + ",\n\n"
                + "Your password has been reset successfully"
                + "Best regards,\n"
                + "The Team";
        emailService.sendEmail(event.getUser().getEmail(), subject, message);
        logger.info("Password reset successfully email sent to {}", event.getUser().getEmail()); 
	}
	
	@Override
	@EventListener
    public void handleChangePassword(ChangePasswordEvent event) {
		String subject = "Password changed successfully";
		String message = "Hi " + event.getUsername() + ",\n\n"
                + "Your password has been changed successfully"
                + "Best regards,\n"
                + "The Team";
        emailService.sendEmail(event.getEmail(), subject, message);
        logger.info("Password changed successfully email sent to {}", event.getEmail()); 
	}
	
	@Override
	@EventListener
    public void handleUpdateAccountStatus(UpdateAccountStatusEvent event) {
		String subject = "Account status updated successfully";
		String message = "Hi " + event.getUsername() + ",\n\n"
                + "Your account status has been updated successfully to"
				+ event.getNewAccountStatus()
                + "Best regards,\n"
                + "The Team";
        emailService.sendEmail(event.getEmail(), subject, message);
        logger.info("Account status updated successfully email sent to {}", event.getEmail()); 
	}
}
