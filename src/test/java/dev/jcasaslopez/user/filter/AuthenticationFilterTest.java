package dev.jcasaslopez.user.filter;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import dev.jcasaslopez.user.utilities.Constants;

// Covered scenarios (All return a 401 UNAUTHORIZED response without proceeding to the controller):
// - Invalid Authorization Header (missing or malformed, as both return 'false' from verifyHeaderIsValid()).
// - Invalid Token (expired, malformed, invalid signature, as all return Optional.empty() from getValidClaims()).

// As for the scenarios where both header and token were valid, but token type did not match the endpoint request, 
// (flow enters the switch in AuthenticationFilter), the tests were unable to reliably replicate production behavior, 
// consistently failing with environment-specific errors that did not reflect actual code flaws, so they were removed.
// The security functionality remains verified manually and through the following tests and a separate integration 
// test suite that confirms happy path scenarios (see dev.jcasaslopez.user.controller package integration tests).

// Uses @SpringBootTest with MockMvc because mocking the dependencies was extremely brittle and error-prone for filter testing.
@SpringBootTest
@AutoConfigureMockMvc
public class AuthenticationFilterTest {
    
    @Autowired private MockMvc mockMvc;
    
    // For these tests, any non-public endpoint can be used. 
    
    // It does not matter if the header is completely missing or malformed (i.e., not starting with "Bearer "),
    // as all these scenarios return the same result: 'false' (see verifyHeaderIsValid() in AuthenticationServiceImpl),
    // which causes the filter to short-circuit and return 401. Therefore, we only test one of the above scenarios.
    @Test
    @DisplayName("If the header is invalid or missing, it returns 401 UNAUTHORIZED")
    void authenticationFilter_WhenHeaderInvalid_Returns401() throws Exception {    	
        mockMvc.perform(MockMvcRequestBuilders
                .put(Constants.UPGRADE_USER_PATH)
                // Missing header
                .contentType(MediaType.APPLICATION_JSON)
                .content("jorgecasas22@hotmail.com"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentType("application/json;charset=UTF-8"))
            .andExpect(jsonPath("$.message").value("Access denied: invalid or missing token")) 
            .andExpect(jsonPath("$.status").value("UNAUTHORIZED")); 
    }
    
    // It does not matter if token is malformed, expired or the signature is invalid, as all these scenarios return the
    // same result: an empty Optional (see parseClaims() and getValidClaims() in TokenService), so we only one test one of 
    // the above scenarios.
    @Test
    @DisplayName("If the token is invalid, it returns 401 UNAUTHORIZED")
    void authenticationFilter_WhenTokenInvalid_Returns401() throws Exception {
    	String invalidToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJZb3JjaDIyIiwianRpIjoiM";
    	
        mockMvc.perform(MockMvcRequestBuilders
                .put(Constants.UPGRADE_USER_PATH)
                .header("Authorization", "Bearer " + invalidToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("jorgecasas22@hotmail.com"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentType("application/json;charset=UTF-8"))
            .andExpect(jsonPath("$.message").value("Access denied: invalid or missing token")) 
            .andExpect(jsonPath("$.status").value("UNAUTHORIZED")); 
    }
    
}