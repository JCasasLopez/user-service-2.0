package dev.jcasaslopez.user.service;

public interface EmailService {

	void sendEmail(String recipient, String subject, String message);

}