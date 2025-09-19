package dev.jcasaslopez.user.controller;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.jcasaslopez.user.dto.StandardResponse;
import dev.jcasaslopez.user.entity.User;
import dev.jcasaslopez.user.enums.TokenType;
import dev.jcasaslopez.user.service.EmailService;
import dev.jcasaslopez.user.testhelper.TestHelper;
import dev.jcasaslopez.user.utilities.Constants;

@SuppressWarnings("removal")
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(Lifecycle.PER_CLASS)
public class SendNotificationIntegrationTest {
	
	@Autowired private ObjectMapper objectMapper;
	@Autowired private MockMvc mockMvc;
	@Autowired private TestHelper testHelper;
	@SpyBean private EmailService emailService;
	
	private User user;
	private static final String username = "Yorch22";
	private static final String password = "Jorge22!";
	
	private static final String subject = "Test";
	private static final String message = "This is a test email";
	
	@BeforeAll
	void setup() {
	    user = testHelper.createAndPersistUser(username, password);
	}
	
	@AfterAll
	void cleanUp() {
		testHelper.cleanDataBaseAndRedis();
	}
	
	@Test
	@DisplayName("When message is correcly formatted, an email is sent")
	void sendNotification_WhenMessageCorrectlyFormatted_ShouldSendEmail() throws Exception {
		// Arrange	    
	    testHelper.loginUser(user, TokenType.ACCESS);
	    
	    String recipient = String.valueOf(user.getIdUser());
	    Map<String, String> payload = createNotificationPayload(recipient, subject, message);
	    RequestBuilder requestBuilder = buildRequest(payload);

	    // Act
	    StandardResponse response = executeRequestAndGetResponse(requestBuilder);
	    EmailArguments emailArgs = captureEmailArguments();

	    // Assert
	    assertAll(
	        () -> assertEquals(subject, emailArgs.subject(), "Email subject does not match"),
	        () -> assertEquals(message, emailArgs.message(), "Email body does not match"),
	        () -> assertEquals(HttpStatus.OK, response.getStatus(), "Expected HTTP status 200"),
	        () -> assertNotNull(response.getMessage(), "Response message should not be null"),
	        () -> assertEquals("Notification sent successfully", response.getMessage(), "Unexpected response message"),
	        () -> assertEquals(user.getEmail(), emailArgs.emailAddress(), "Recipient email does not match")
	    );
	}
	
	@Test
	@DisplayName("When message is NOT correcly formatted returns 400 BAD REQUEST")
	void sendNotification_WhenMessageNotCorrectlyFormatted_ShouldReturn400BadRequest() throws Exception {
		// Arrange
	    testHelper.loginUser(user, TokenType.ACCESS);
	    
	    String recipient = String.valueOf(user.getIdUser());
	    Map<String, String> payload = createInvalidNotificationPayload(recipient, subject, message);
	    RequestBuilder requestBuilder = buildRequest(payload);

	    // Act 
	    StandardResponse response = executeRequestAndGetResponse(requestBuilder);
	    
	    // Assert
	    assertAll(
	        () -> assertEquals(HttpStatus.BAD_REQUEST, response.getStatus(), "Expected HTTP status 400"),
	        () -> assertNotNull(response.getMessage(), "Response body should not be null"),
	        () -> assertTrue(response.getMessage().contains("Invalid message format"), "Unexpected response message")
	    );  
	}
	
	
	// ************** HELPER METHODS **************
	
	private Map<String, String> createNotificationPayload(String recipient, String subject, String message) {
	    Map<String, String> payload = new HashMap<>();
	    payload.put("Recipient", String.valueOf(user.getIdUser()));
	    payload.put("Subject", "Test");
	    payload.put("Message", "This is a test email");
	    return payload;
	}

	private Map<String, String> createInvalidNotificationPayload(String recipient, String subject, String message) {
		Map<String, String> payload = createNotificationPayload(recipient, subject, message);
		// Change the right key for one with an intentional typo, rendering the payload invalid.
		payload.remove("Recipient");
		payload.put("Recip", String.valueOf(user.getIdUser())); 
	    return payload;
	}

	private RequestBuilder buildRequest(Map<String, String> payload) throws Exception {
	    return MockMvcRequestBuilders
	        .post(Constants.SEND_NOTIFICATION_PATH)
	        .contentType(MediaType.APPLICATION_JSON)
	        .content(objectMapper.writeValueAsString(payload))
	        .accept(MediaType.APPLICATION_JSON);
	}

	private StandardResponse executeRequestAndGetResponse(RequestBuilder requestBuilder) throws Exception {
	    MvcResult mvcResult = mockMvc.perform(requestBuilder).andReturn();
	    String responseAsString = mvcResult.getResponse().getContentAsString();
	    return objectMapper.readValue(responseAsString, StandardResponse.class);
	}

	private EmailArguments captureEmailArguments() {
	    ArgumentCaptor<String> emailAddressCaptor = ArgumentCaptor.forClass(String.class);
	    ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
	    ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
	    
	    verify(emailService).sendEmail(
	        emailAddressCaptor.capture(), 
	        subjectCaptor.capture(), 
	        messageCaptor.capture()
	    );
	    
	    return new EmailArguments(
	        emailAddressCaptor.getValue(),
	        subjectCaptor.getValue(),
	        messageCaptor.getValue()
	    );
	}

	// To encapsulate email fields
	private record EmailArguments(String emailAddress, String subject, String message) {}
}