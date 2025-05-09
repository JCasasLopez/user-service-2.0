package dev.jcasaslopez.user.utilities;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import dev.jcasaslopez.user.exception.MalformedMessageException;

public class MessageNotificationValidationTest {
	
	private static Stream<Arguments> messageValidationData(){
		return Stream.of(
				// Typo in first key
				Arguments.of("Recipien", "0", "Subject", "Test", "Message", "This is a test email"),
				// First value is null
				Arguments.of("Recipient", null, "Subject", "Test", "Message", "This is a test email"),
				// First value is blank
				Arguments.of("Recipient", "", "Subject", "Test", "Message", "This is a test email"),
				// First value's format is not valid
				Arguments.of("Recipient", "a", "Subject", "Test", "Message", "This is a test email"),
				// Type in second key
				Arguments.of("Recipient", "0", "Subjec", "Test", "Message", "This is a test email"),
				// Second value is null
				Arguments.of("Recipient", "0", "Subject", null, "Message", "This is a test email"),
				// Second value is blank
				Arguments.of("Recipient", "0", "Subject", "", "Message", "This is a test email"),
				// Typo in third key
				Arguments.of("Recipient", "0", "Subject", "Test", "Messag", "This is a test email"),
				// Third value is null
				Arguments.of("Recipient", "0", "Subject", "Test", "Message", null),
				// Third value is blank
				Arguments.of("Recipient", "0", "Subject", "Test", "Message", "")
				);
	}
	
	@ParameterizedTest
	@MethodSource("messageValidationData")
	@DisplayName("Throws an exception when message format is not valid")
	public void validateMessage_WhenMessageFormatNotValid_ShouldThrowException(String firstKey,
			String firstValue, String secondKey, String secondValue, String thirdKey, String thirdValue) {
		// Arrange
		Map<String, String> messageAsMap = new HashMap<>();
		messageAsMap.put(firstKey, firstValue);
		messageAsMap.put(secondKey, secondValue);
		messageAsMap.put(thirdKey, thirdValue);
		
		// Act & Assert
		assertThrows(MalformedMessageException.class, 
				() -> MessageNotificationValidation.validateMessage(messageAsMap),
				"When the map format is invalid, should throw an exception");
	}
}