package dev.jcasaslopez.user.controller;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.jcasaslopez.user.dto.StandardResponse;
import dev.jcasaslopez.user.entity.User;
import dev.jcasaslopez.user.enums.TokenType;
import dev.jcasaslopez.user.exception.MalformedMessageException;
import dev.jcasaslopez.user.repository.UserRepository;
import dev.jcasaslopez.user.service.EmailService;
import dev.jcasaslopez.user.testhelper.TestHelper;

// No se testea acceso sin autenticación aquí porque ya está verificado 
// en otros endpoints y este utilizar la misma configuración.
//
// Access without authentication is not tested here because it has already been verified 
// in other endpoints and this one uses the same configuration.
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(Lifecycle.PER_CLASS)
@ActiveProfiles("test")
public class SendNotificationIntegrationTest {
	
	@Autowired private UserRepository userRepository;
	@Autowired private ObjectMapper objectMapper;
	@Autowired private MockMvc mockMvc;
	@Autowired private TestHelper testHelper;
	@SpyBean private EmailService emailService;
	
	private User user;
	
	@BeforeAll
	void setup() {
	    user = new User();
	    user.setIdUser(0);
	    user.setEmail("test@example.com");
	    userRepository.save(user);
	}
	
	@Test
	@DisplayName("When message is correcly formatted, an email is sent")
	void sendNotification_WhenMessageCorrectlyFormatted_ShouldSendEmail() throws Exception {
		// Arrange
		String recipient = String.valueOf(user.getIdUser());
		String subject = "Test";
		String message = "This is a test email";
		
		// El token es irrelevante para nuestro test, pero necesitamos cumplir con la firma 
		// del método, así que le asignamos un tipo válido cualquiera.
		// 
		// The token type is not relevant for this test, but required by the method signature,
		// so we pass any valid value.
		testHelper.loginUser(user, TokenType.ACCESS);
		
		RequestBuilder requestBuilder = MockMvcRequestBuilders
		        .post("/sendNotification")
		        .param("Recipient", recipient)
		        .param("Subject", subject)
		        .param("Message", message)
		        .accept(MediaType.APPLICATION_JSON);

		// Act
		MvcResult mvcResult = mockMvc.perform(requestBuilder).andReturn();
		String responseAsString = mvcResult.getResponse().getContentAsString();
	    StandardResponse response = objectMapper.readValue(responseAsString, StandardResponse.class);
		
		// Assert
	    ArgumentCaptor<String> emailAddressCaptor = ArgumentCaptor.forClass(String.class);
	    ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
	    ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
		verify(emailService).sendEmail( emailAddressCaptor.capture(), 
										subjectCaptor.capture(), 
										messageCaptor.capture()
										);
		String emailAddress = emailAddressCaptor.getValue();
		String emailSubject = subjectCaptor.getValue();
		String emailMessage = messageCaptor.getValue();
		
		assertAll(
			    () -> assertEquals(subject, emailSubject, 
			            "Email subject does not match the expected value"),
			    () -> assertEquals(message, emailMessage, 
			            "Email body does not match the expected content"),
			    () -> assertEquals(HttpStatus.OK.value(), mvcResult.getResponse().getStatus(), 
			            "Expected HTTP status 200 OK but got a different status"),
			    () -> assertNotNull(response.getMessage(), 
			            "Response message should not be null"),
			    () -> assertEquals("Notification sent successfully", response.getMessage(), 
			            "Unexpected response message content"),
			    () -> assertEquals(user.getEmail(), emailAddress, 
			            "Recipient email does not match the user's email")
			);
	}
	
	@Test
	@DisplayName("When message is NOT correcly formatted returns 400 BAD REQUEST")
	void sendNotification_WhenMessageNotCorrectlyFormatted_ShouldReturn400BadRequest() throws Exception {
		// Arrange
		String recipient = String.valueOf(user.getIdUser());
		String subject = "Test";
		String message = "This is a test email";
		
		testHelper.loginUser(user, TokenType.ACCESS);
		
		RequestBuilder requestBuilder = MockMvcRequestBuilders
		        .post("/sendNotification")
		        // Typo
		        .param("Recipie", recipient)
		        .param("Subject", subject)
		        .param("Message", message)
		        .accept(MediaType.APPLICATION_JSON);
		
		// Act 
		MvcResult mvcResult = mockMvc.perform(requestBuilder).andReturn();
		String responseAsString = mvcResult.getResponse().getContentAsString();
	    StandardResponse response = objectMapper.readValue(responseAsString, StandardResponse.class);
	    
	    // Assert
	    assertAll(
	    		() -> assertEquals(HttpStatus.BAD_REQUEST, response.getStatus(),
	    				"Expected HTTP status 400 BAD REQUEST"),
	    		() -> assertNotNull(response.getMessage(),
	    				"Response body should not be null"),
	    		() -> assertTrue(response.getMessage().contains("Invalid message format"),
	    				"Unexpected response message")
	    		);	  
	}
}