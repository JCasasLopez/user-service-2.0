package dev.jcasaslopez.user.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;

import dev.jcasaslopez.user.event.ChangePasswordEvent;
import dev.jcasaslopez.user.event.CreateAccountEvent;
import dev.jcasaslopez.user.event.ForgotPasswordEvent;
import dev.jcasaslopez.user.event.ResetPasswordEvent;
import dev.jcasaslopez.user.event.UpdateAccountStatusEvent;
import dev.jcasaslopez.user.event.VerifyEmailEvent;

@Service
public class NotificationServiceImpl implements NotificationService {
	
	private static final Logger logger = LoggerFactory.getLogger(NotificationServiceImpl.class);

	String urlAngular = "http://classrooms.com/user-service/";

	private EmailService emailService;
    
	public NotificationServiceImpl(EmailService emailService) {
		this.emailService = emailService;
	}

	@Override
	@EventListener
    public void handleVerifyEmail(VerifyEmailEvent event) throws JsonProcessingException {
		logger.info("Starting email verification flow for user: {}", event.getUser().getUsername()); 

        String subject = "Email verification";
        String message = "Hi " + event.getUser().getUsername() + ",\n\n"
                + "Thank you for registering. To complete your account setup, "
                + "please verify your email address by copying and pasting the next link:"
                + urlAngular + "/verifyEmail" + "?token=" + event.getToken()
                + "Best regards The Team";
          
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
		String message = "Hi " + event.getUser().getUsername() + ",\n\n"
                + "Your password has been changed successfully"
                + "Best regards,\n"
                + "The Team";
        emailService.sendEmail(event.getUser().getEmail(), subject, message);
        logger.info("Password changed successfully email sent to {}", event.getUser().getEmail()); 
	}
	
	@Override
	@EventListener
    public void handleUpdateAccountStatus(UpdateAccountStatusEvent event) {
		String subject = "Change in account status";
		String message = "Hi " + event.getUser().getUsername() + ",\n\n"
                + "Your account status has been changed to"
				+ event.getNewAccountStatus().getDisplayName()
                + "Best regards,\n"
                + "The Team";
        emailService.sendEmail(event.getUser().getEmail(), subject, message);
        logger.info("Account status change successfully email sent to {}", event.getUser().getEmail()); 
	}
}
