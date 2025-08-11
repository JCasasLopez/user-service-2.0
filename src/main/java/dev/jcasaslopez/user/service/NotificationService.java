package dev.jcasaslopez.user.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;

import dev.jcasaslopez.user.enums.AccountStatus;
import dev.jcasaslopez.user.enums.NotificationType;
import dev.jcasaslopez.user.event.NotifyingEvent;

@Service
public class NotificationService {
	
	private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    @Value("${frontend.url.angular}") 
    private String urlAngular;
	private EmailService emailService;
	private final String messageGreeting = "<p>Hi %s,</p>";
	private final String messageFarewell =  """
											<p>Best regards,<br>
											The Team</p>
											""";
    
	public NotificationService(EmailService emailService) {
		this.emailService = emailService;
	}
	
	// Constructs and sends notification emails based on event data and NotificationType.
	// Includes user info, optional token or account status, and builds message accordingly.
	// Different message constructions are applied depending on notification type and event content.

	@EventListener
	public void buildNotificationMessage(NotificationType notificationType, 
			NotifyingEvent event) throws JsonProcessingException {
		String username = event.getUser().getUsername();
		String email = event.getUser().getEmail();
		String subject = notificationType.getSubject();
		String logText = notificationType.getLogText();
				
		// Example: "Starting email verification flow for user: Yorch123"
	    logger.info(logText + ": {}", username);
	    
	    String htmlMessage = buildEmailText(notificationType, event, username);
		
	    emailService.sendEmail(email, subject, htmlMessage);
	}
	
	private String buildEmailText(NotificationType notificationType, NotifyingEvent event,
				String username) {
		// Message text minus the greeting and the farewell, defined above as constants.
		String messageCore = notificationType.getMessageText();

		if(notificationType == NotificationType.FORGOT_PASSWORD || 
				notificationType == NotificationType.VERIFY_EMAIL) {
			String token = event.getToken();
			return (messageGreeting + messageCore + messageFarewell).formatted(username, urlAngular, token);

		} else if(notificationType == NotificationType.UPDATE_ACCOUNT_STATUS) {
			AccountStatus newAccountStatus = event.getAccountStatus();
			if (newAccountStatus == AccountStatus.TEMPORARILY_BLOCKED) {
				messageCore = "Your account has been temporarily blocked. It will become active again automatically within 24 hours.";
			} else if (newAccountStatus == AccountStatus.ACTIVE) {
				messageCore = "Your account is active again.";
			} else if (newAccountStatus == AccountStatus.PERMANENTLY_SUSPENDED) {
				messageCore = "Your account has been permanently suspended.";
			}
			return (messageGreeting + messageCore + messageFarewell).formatted(username, messageCore);

		} else {
			return (messageGreeting + messageCore + messageFarewell).formatted(username);
		}
	}
}