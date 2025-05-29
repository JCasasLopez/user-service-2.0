package dev.jcasaslopez.user.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;

import dev.jcasaslopez.user.enums.AccountStatus;
import dev.jcasaslopez.user.event.ChangePasswordEvent;
import dev.jcasaslopez.user.event.CreateAccountEvent;
import dev.jcasaslopez.user.event.ForgotPasswordEvent;
import dev.jcasaslopez.user.event.ResetPasswordEvent;
import dev.jcasaslopez.user.event.UpdateAccountStatusEvent;
import dev.jcasaslopez.user.event.VerifyEmailEvent;

@Service
public class NotificationServiceImpl implements NotificationService {
	
	private static final Logger logger = LoggerFactory.getLogger(NotificationServiceImpl.class);

    @Value("${frontend.url.angular}") 
    private String urlAngular;
   
	private EmailService emailService;
    
	public NotificationServiceImpl(EmailService emailService) {
		this.emailService = emailService;
	}

	@Override
	@EventListener
	public void handleVerifyEmail(VerifyEmailEvent event) throws JsonProcessingException {
		String username = event.getUser().getUsername();
		String email = event.getUser().getEmail();
		String token = event.getToken();

	    logger.info("Starting email verification flow for user: {}", event.getUser().getUsername());

	    String subject = "Email verification";
	    String message = """
	        <p>Hi %s,</p>
	        <p>Thank you for registering. To complete your account setup, 
	        please verify your email address by clicking on the following link:</p>
	        <p><a href="%s/verifyEmail?token=%s">Verify my email</a></p>
	        <p>Best regards,<br>
	        The Team</p>
	        """.formatted(username, urlAngular, token);

	    emailService.sendEmail(email, subject, message);
	}
	
	@Override
	@EventListener
    public void handleCreateAccount(CreateAccountEvent event) {
		String username = event.getUser().getUsername();
		String email = event.getUser().getEmail();
		
		String subject = "Welcome to the platform!";
	    
	    String htmlMessage = """
	        <p>Hi %s,</p>
	        <p>Your account has been created successfully.</p>
	        <p>Best regards,<br>
	        The Team</p>
	        """.formatted(username);
	    
	    emailService.sendEmail(email, subject, htmlMessage);
	}
	
	@Override
	@EventListener
	public void handleForgotPassword(ForgotPasswordEvent event) throws JsonProcessingException {
		String username = event.getUser().getUsername();
		String email = event.getUser().getEmail();
		String token = event.getToken();
		
	    logger.info("Starting password reset flow for user: {}", username);

	    String subject = "Password reset verification email";
	    String message = """
	        <p>Hi %s,</p>
	        <p>Click on the following link to reset your password:</p>
	        <p><a href="%s/resetPassword?token=%s">Reset my password</a></p>
	        <p>Best regards,<br>
	        The Team</p>
	        """.formatted(username, urlAngular, token);

	    emailService.sendEmail(email, subject, message);
	}

	@Override
	@EventListener
	public void handleResetPassword(ResetPasswordEvent event) {
		String username = event.getUser().getUsername();
		String email = event.getUser().getEmail();
		
	    String subject = "Password reset successfully";
	    String message = """
	        <p>Hi %s,</p>
	        <p>Your password has been reset successfully.</p>
	        <p>Best regards,<br>
	        The Team</p>
	        """.formatted(username);
	    
	    emailService.sendEmail(email, subject, message);
	}

	@Override
	@EventListener
	public void handleChangePassword(ChangePasswordEvent event) {
		String username = event.getUser().getUsername();
		String email = event.getUser().getEmail();
		
	    String subject = "Password changed successfully";
	    String message = """
	        <p>Hi %s,</p>
	        <p>Your password has been changed successfully.</p>
	        <p>Best regards,<br>
	        The Team</p>
	        """.formatted(username);
	    
	    emailService.sendEmail(email, subject, message);
	}

	@Override
	@EventListener
	public void handleUpdateAccountStatus(UpdateAccountStatusEvent event) {
		String username = event.getUser().getUsername();
		String email = event.getUser().getEmail();
		AccountStatus newAccountStatus = event.getNewAccountStatus();
		
		String messageCore="";
		if (newAccountStatus == AccountStatus.TEMPORARILY_BLOCKED) {
		    messageCore = "Your account has been temporarily blocked. It will become active again automatically within 24 hours.";
		} else if (newAccountStatus == AccountStatus.ACTIVE) {
		    messageCore = "Your account is active again.";
		} else if (newAccountStatus == AccountStatus.PERMANENTLY_SUSPENDED) {
		    messageCore = "Your account has been permanently suspended.";
		}
		
	    String subject = "Change in account status";
	    String message = """
	        <p>Hi %s,</p>
	        <p>%s</p>
	        <p>Best regards,<br>
	        The Team</p>
	        """.formatted(username, messageCore);
	    
	    emailService.sendEmail(email, subject, message);
	}
}