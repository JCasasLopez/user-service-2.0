package dev.jcasaslopez.user.service;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import dev.jcasaslopez.user.entity.User;
import dev.jcasaslopez.user.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailServiceImpl implements EmailService {
	
    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

	private JavaMailSender mailSender;
	private UserRepository userRepository;

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
            
            logger.info("Email successfully sent to: {}", recipient);
            
        } catch (MessagingException ex) {
            logger.error("Failed to send email to {}: {}", recipient, ex.getMessage(), ex);
        }
    }
    
    public void processMessageDetails(Map<String, String> messageAsMap) {
        int idUser = Integer.valueOf(messageAsMap.get("Recipient"));
        logger.debug("Processing message for user ID: {}", idUser);

        Optional<User> optionalUser = userRepository.findById(idUser);
        if (optionalUser.isEmpty()) {
            logger.warn("User with ID {} not found in the database", idUser);
            throw new UsernameNotFoundException("User not found in the database");
        }

        User user = optionalUser.get();
        String email = user.getEmail();
        String subject = messageAsMap.get("Subject");
        String messageBody = messageAsMap.get("Message");

        logger.info("Sending email to {} with subject '{}'", email, subject);
        sendEmail(email, subject, messageBody);
    }

}
