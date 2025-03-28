package dev.jcasaslopez.user.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailServiceImpl implements EmailService {
	
    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

	JavaMailSender mailSender;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
	public void sendEmail(String recipient, String subject, String message) {
    	
        try {
        	logger.info("Preparing email to: {}", recipient);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);

            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(message, true); 

            mailSender.send(mimeMessage);
            System.out.println("Email sent to: " + recipient);
            
            logger.info("Email successfully sent to: {}", recipient);
            
        } catch (MessagingException ex) {
            logger.error("Failed to send email to {}: {}", recipient, ex.getMessage(), ex);
        }
    }
}
