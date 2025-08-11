package dev.jcasaslopez.user.enums;

public enum NotificationType {
	
	// Fields correspond to:
	// (1) Email subject
	// (2) Log text
	// (3) Message text or core, minus the greeting and the farewell
	
	VERIFY_EMAIL(
	        "Email verification",
	        "Starting email verification flow for user: %s",
	        """
	        <p>Thank you for registering. To complete your account setup, 
	        please verify your email address by clicking on the following link:</p>
	        <p><a href="%s/verifyEmail?token=%s">Verify my email</a></p>
	        """
	    ),
	
	CREATE_ACCOUNT(
	        "Welcome to the platform!",
	        "Starting account creation flow for user: %s",
	        """
	        <p>Your account has been created successfully.</p>
	        """
	    ),
	
	FORGOT_PASSWORD(
	        "Password reset verification email",
	        "Starting password reset flow for user: %s",
	        """
	        <p>Click on the following link to reset your password:</p>
	        <p><a href="%s/resetPassword?token=%s">Reset my password</a></p>
	        """
	    ),
	
	RESET_PASSWORD(
			"Password reset successfully",
	        "Starting reset password flow for user: %s",
	        """
	        <p>Your password has been reset successfully.</p>
	        """
		),
	
	CHANGE_PASSWORD(
			"Password changed successfully",
			"Starting change password flow for user: %s",
			"""
	        <p>Your password has been changed successfully.</p>
	        """
		),
	
	UPDATE_ACCOUNT_STATUS(
			"Change in account status",
			"Informing user %s of change in account status",
			// The message core is defined in NotificationService and its content depends on 
			// the status the user account has changed to
			"""
	        <p>%s</p>
	        """
		);
	
	private final String subject;
    private final String logText;
    private final String messageText;
    
	private NotificationType(String subject, String logText, String messageText) {
		this.subject = subject;
		this.logText = logText;
		this.messageText = messageText;
	}

	public String getSubject() {
		return subject;
	}

	public String getLogText() {
		return logText;
	}

	public String getMessageText() {
		return messageText;
	}
    
}