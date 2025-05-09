package dev.jcasaslopez.user.utilities;

import java.util.Map;

import dev.jcasaslopez.user.exception.MalformedMessageException;

public class MessageNotificationValidation {

	public static void validateMessage(Map<String, String> messageAsMap) {
		
		if (!messageAsMap.containsKey("Recipient") || 
				!messageAsMap.containsKey("Subject") || 
				!messageAsMap.containsKey("Message")) {
			throw new MalformedMessageException("Invalid message format: one or more keys are invalid ('Recipient', 'Subject', 'Message')");
		}
		
		if (messageAsMap.get("Recipient") == null || messageAsMap.get("Recipient").isBlank() ||
			    messageAsMap.get("Subject") == null || messageAsMap.get("Subject").isBlank() ||
			    messageAsMap.get("Message") == null || messageAsMap.get("Message").isBlank()) {
			throw new MalformedMessageException("Invalid message format: none of the fields can be null or blank");
		}
		
		try {
            Integer.parseInt(messageAsMap.get("Recipient"));
        } catch (NumberFormatException e) {
            throw new MalformedMessageException("Invalid message format: recipient must be a valid numeric ID");
        }
	}
}