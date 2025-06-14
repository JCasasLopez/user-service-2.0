package dev.jcasaslopez.user.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import dev.jcasaslopez.user.dto.StandardResponse;
import dev.jcasaslopez.user.dto.UserDto;
import dev.jcasaslopez.user.utilities.Constants;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ValidationsInitiateRegistrationTest {
	
	@Autowired TestRestTemplate testRestTemplate;
	@Autowired ObjectMapper mapper;
	
	public static Stream<Arguments> invalidUserData(){
		return Stream.of(
				// Username blank
				Arguments.of(new UserDto("", "Jorge22!", "Jorge García", "jorgecasas78@hotmail.com",
						LocalDate.of(1978, 11, 26))),
				// Username too short
				Arguments.of(new UserDto("Yorch", "Jorge22!", "Jorge García", "jorgecasas78@hotmail.com",
						LocalDate.of(1978, 11, 26))),
				// Username too long
				Arguments.of(new UserDto("YorchAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAaaa", "Jorge22!", 
						"Jorge García", "jorgecasas78@hotmail.com", LocalDate.of(1978, 11, 26))),
				// Password blank
				Arguments.of(new UserDto("Yorch22", "", "Jorge García", "jorgecasas78@hotmail.com",
						LocalDate.of(1978, 11, 26))),
				// Password does not have 8 characters
				Arguments.of(new UserDto("Yorch22", "Jorge2!", "Jorge García", "jorgecasas78@hotmail.com",
						LocalDate.of(1978, 11, 26))),
				// Password does not have a capital letter
				Arguments.of(new UserDto("Yorch22", "jorge22!", "Jorge García", "jorgecasas78@hotmail.com",
						LocalDate.of(1978, 11, 26))),
				// Password does not have a lowercase letter
				Arguments.of(new UserDto("Yorch22", "JORGE22!", "Jorge García", "jorgecasas78@hotmail.com",
						LocalDate.of(1978, 11, 26))),
				// Password does not have a number
				Arguments.of(new UserDto("Yorch22", "JorgeGarcia!", "Jorge García", "jorgecasas78@hotmail.com",
						LocalDate.of(1978, 11, 26))),
				// Password does not have a symbol
				Arguments.of(new UserDto("Yorch22", "JorgeGarcia22", "Jorge García", "jorgecasas78@hotmail.com",
						LocalDate.of(1978, 11, 26))),
				// Full blank
				Arguments.of(new UserDto("Yorch22", "Jorge22!", "", "jorgecasas78@hotmail.com",
						LocalDate.of(1978, 11, 26))),
				// Full name too long
				Arguments.of(new UserDto("Yorch22", "Jorge22!", "Jorge García AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", 
						"jorgecasas78@hotmail.com", LocalDate.of(1978, 11, 26))),
				// Email blank
				Arguments.of(new UserDto("Yorch22", "Jorge22!", "Jorge García", "", 
						LocalDate.of(1978, 11, 26))),
				// Email has wrong format
				Arguments.of(new UserDto("Yorch22", "Jorge22!", "Jorge García", "jorgecasas78hotmailcom", 
						LocalDate.of(1978, 11, 26))),
				// DOB null 
				Arguments.of(new UserDto("Yorch22", "Jorge22!", "Jorge García", "jorgecasas78hotmailcom", 
						null))
				);	
	}
	
	@ParameterizedTest
	@DisplayName("When user violates validations response is 400 BAD REQUEST")
	@MethodSource("invalidUserData")
	public void initiateRegistration_whenDetailsNotValid_ShouldResponse400BadRequest
											(UserDto invalidUser) throws JsonProcessingException {
		// Arrange
	
		// Jackson cannot serialize or deserialize java.time.LocalDate by default.
		// You need to register the JavaTimeModule with the ObjectMapper.
		mapper.registerModule(new JavaTimeModule());
		String userJson = mapper.writeValueAsString(invalidUser);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setAccept(List.of(MediaType.APPLICATION_JSON));

		HttpEntity<String> request = new HttpEntity<>(userJson, headers);

		// Act
		ResponseEntity<StandardResponse> initiateRegistrationResponse = testRestTemplate
				.postForEntity(Constants.INITIATE_REGISTRATION_PATH, request, StandardResponse.class);

		// Assert
		assertEquals(HttpStatus.BAD_REQUEST, initiateRegistrationResponse.getStatusCode());
	}
}