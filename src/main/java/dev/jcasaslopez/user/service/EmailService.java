package dev.jcasaslopez.user.service;

import java.util.Map;

public interface EmailService {

	void sendEmail(String recipient, String subject, String message);
	void processMessageDetails(Map<String, String> messageAsMap);

}