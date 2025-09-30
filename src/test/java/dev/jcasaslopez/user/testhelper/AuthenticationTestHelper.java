package dev.jcasaslopez.user.testhelper;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.jcasaslopez.user.dto.LoginResponse;
import dev.jcasaslopez.user.dto.StandardResponse;
import dev.jcasaslopez.user.utilities.Constants;

// Logs in a user through the real login endpoint (avoiding coupling with internal logic).
// Returns the access token needed for protected endpoints.

// The response body has the structure:
// StandardResponse { timestamp, message, details, httpStatus }
// where `details` contains a LoginResponse with user info, access token and refresh token.

// 2 versions, MockMvc and TestRestTemplate, since different tests use different approaches.

@Component
public class AuthenticationTestHelper {
	
	@Autowired private MockMvc mockMvc;
	@Autowired private TestRestTemplate testRestTemplate;
	@Autowired private ObjectMapper objectMapper;
		
	public LoginResponse logInWithMockMvc(String username, String password) throws Exception {
	        RequestBuilder requestBuilder = MockMvcRequestBuilders
	                .post(Constants.LOGIN_PATH)
	                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
	                .accept(MediaType.APPLICATION_JSON)
	                .param("username", username)
	                .param("password", password);

	        MvcResult mvcResult = mockMvc.perform(requestBuilder).andReturn();
	        String responseAsString = mvcResult.getResponse().getContentAsString();

	        // Serialize the StandardResponse object from the response.
	        StandardResponse response = objectMapper.readValue(responseAsString, StandardResponse.class);
	        Object detailsObject = response.getDetails();

	        // Serialize the LoginResponse object from the details field of StandardResponse.
	        LoginResponse loginResponse = objectMapper.convertValue(detailsObject, LoginResponse.class);
	        return loginResponse;
	}

	
	public LoginResponse logInWithTestRestTemplate(String username, String password) {
	    MultiValueMap<String, String> loginForm = new LinkedMultiValueMap<>();
	    loginForm.add("username", username);
	    loginForm.add("password", password);

	    HttpHeaders headers = new HttpHeaders();
	    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
	    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

	    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(loginForm, headers);
	    ResponseEntity<StandardResponse> response = testRestTemplate.postForEntity(Constants.LOGIN_PATH, request, StandardResponse.class);

	    // Gets the LoginResponse object from the details field of StandardResponse and serializes it.
	    Object detailsObject = response.getBody().getDetails();
	    LoginResponse loginResponse = objectMapper.convertValue(detailsObject, LoginResponse.class);

	    return loginResponse;
	}

}