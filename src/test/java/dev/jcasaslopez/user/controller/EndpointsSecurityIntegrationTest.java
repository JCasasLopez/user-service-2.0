package dev.jcasaslopez.user.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

import dev.jcasaslopez.user.entity.User;
import dev.jcasaslopez.user.repository.UserRepository;
import dev.jcasaslopez.user.testhelper.AuthenticationTestHelper;
import dev.jcasaslopez.user.testhelper.TestHelper;
import dev.jcasaslopez.user.testhelper.UserTestBuilder;
import dev.jcasaslopez.user.utilities.Constants;

// @AutoConfigureMockMvc is needed because AuthenticationTestHelper requires MockMvc bean,
// which is not available by default in @SpringBootTest with RANDOM_PORT configuration.

@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class EndpointsSecurityIntegrationTest {
	
	@Autowired private MockMvc mockMvc;
	@Autowired private AuthenticationTestHelper authTestHelper;
	@Autowired private TestHelper testHelper;
	@Autowired private UserRepository userRepository;

	@Test
	@DisplayName("Fails to delete account when user is not authenticated")
	public void deleteAccount_whenNoUserAuthenticated_ShouldReturn401() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders
				.delete(Constants.DELETE_ACCOUNT_PATH)
				// Missing header
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON))
		.andExpect(status().isUnauthorized())
        .andExpect(content().contentType("application/json;charset=UTF-8"))
		.andExpect(jsonPath("$.message").value("Access denied: invalid or missing token"))
		.andExpect(jsonPath("$.status").value("UNAUTHORIZED"));
	}
	
	@Test
	@DisplayName("Fails to change password when user is not authenticated")
	public void changePassword_whenNoUserAuthenticated_ShouldReturn401() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders
				.put(Constants.CHANGE_PASSWORD_PATH)
				// Missing header
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON))
		.andExpect(status().isUnauthorized())
        .andExpect(content().contentType("application/json;charset=UTF-8"))
		.andExpect(jsonPath("$.message").value("Access denied: invalid or missing token"))
		.andExpect(jsonPath("$.status").value("UNAUTHORIZED"));
	}
	
	@Test
	@DisplayName("User cannot log out when access token is provided")
	void logOut_WhenAccessOrInvalidTokenProvided_ShouldNotLogOutUser() throws Exception {
		// Arrange
		String username = "Yorch22";
		String password = "Password123!";
		UserTestBuilder builder = new UserTestBuilder(username, password);
		testHelper.createAndPersistUser(builder);
		
		String accessToken = authTestHelper.logInWithMockMvc(username, password).getAccessToken();
		
		// Act & Assert
		mockMvc.perform(MockMvcRequestBuilders
				.post(Constants.LOGOUT_PATH)
                .header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON))
		.andExpect(status().isUnauthorized())
        .andExpect(content().contentType("application/json;charset=UTF-8"))
		.andExpect(jsonPath("$.message").value("Access denied: invalid or missing token"))
		.andExpect(jsonPath("$.status").value("UNAUTHORIZED"));
		
		// Clean-up
		testHelper.cleanDataBaseAndRedis();

		}
	
	// This test covers both scenarios, 1) access and 2) blacklisted token, as they both follow the same logic 
	// (see handleRefreshFlow in AuthenticationFlowHandler).
	@Test
	@DisplayName("Endpoint does not return tokens when access or blacklisted token is provided")
	void refreshToken_WhenAccessTokenProvided_ShouldBeUnauthorized() throws Exception {
		// Arrange
		String username = "Yorch22";
		String password = "Password123!";
		UserTestBuilder builder = new UserTestBuilder(username, password);
		testHelper.createAndPersistUser(builder);

		String accessToken = authTestHelper.logInWithMockMvc(username, password).getAccessToken();

		// Act & Assert
		mockMvc.perform(MockMvcRequestBuilders
				.post(Constants.REFRESH_TOKEN_PATH)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON))
		.andExpect(status().isUnauthorized())
		.andExpect(content().contentType("application/json;charset=UTF-8"))
		.andExpect(jsonPath("$.message").value("Access denied: invalid or missing token"))
		.andExpect(jsonPath("$.status").value("UNAUTHORIZED"));

		// Clean-up
		testHelper.cleanDataBaseAndRedis();
	}
	
	@Test
	@DisplayName("Plain user cannot update account status")
	public void updateAccountStatus_WhenUserNotAdmin_ShouldReturn403Forbidden() throws Exception {
		// Arrange
		// 1) Target user: The account subject to modification. 
		UserTestBuilder builderUser = new UserTestBuilder("Yorch22", "Password123!");
		User user = testHelper.createAndPersistUser(builderUser);
		
		// 2) Plain user: Fails to perform modification, as endpoint is only accesible to admin or super admin users.
		String plainUserUsername = "PlainUser";
		String plainUserPassword = "Password456!";
		UserTestBuilder builderAdminUser = new UserTestBuilder(plainUserUsername, plainUserPassword);
		testHelper.createAndPersistUser(builderAdminUser);		
		String authToken = authTestHelper.logInWithMockMvc(plainUserUsername, plainUserPassword).getAccessToken();

		// Act & Assert
		String newAccountStatus = user.getAccountStatus().name();
		
		// The user's email we want to update the account status goes in the body, whereas the new account status 
		// goes as a normal query parameter.
		mockMvc.perform(MockMvcRequestBuilders
				.put(Constants.UPDATE_ACCOUNT_STATUS_PATH)
				.header("Authorization", "Bearer " + authToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(user.getEmail()) 
		        .queryParam("newAccountStatus", newAccountStatus) 
				.accept(MediaType.APPLICATION_JSON))
		.andExpect(status().isForbidden())
		.andExpect(content().contentType("application/json;charset=UTF-8"))
		.andExpect(jsonPath("$.message").value("Access denied: the user does not have the required role to access this resource"))
		.andExpect(jsonPath("$.status").value("FORBIDDEN"));
		
		User userAfterAttempt = userRepository.findByUsername(user.getUsername()).get();
	    assertEquals(user.getAccountStatus(), userAfterAttempt.getAccountStatus(), 
	        "The account status should remain " + user.getAccountStatus() + " after the failed attempt.");

		// Clean-up
		testHelper.cleanDataBaseAndRedis();
	}
	
	@Test
	@DisplayName("Admin user cannot upgrade user to admin")
	public void upgradeUser_WhenUserNotSuperAdmin_ShouldReturn403Forbidden() throws Exception {
		// Arrange
		// 1) Target user: The account subject to modification. 
		UserTestBuilder builderUser = new UserTestBuilder("Yorch22", "Password123!");
		User user = testHelper.createAndPersistUser(builderUser);
		
		// 2) Admin user: Fails to perform modification, as endpoint is only accesible to super admin users.
		String adminUserUsername = "PlainUser";
		String adminUserPassword = "Password456!";
		UserTestBuilder builderAdminUser = new UserTestBuilder(adminUserUsername, adminUserPassword);
		testHelper.createAndPersistUser(builderAdminUser);		
		String authToken = authTestHelper.logInWithMockMvc(adminUserUsername, adminUserPassword).getAccessToken();

		// Act & Assert
		mockMvc.perform(MockMvcRequestBuilders
				.put(Constants.UPGRADE_USER_PATH)
				.header("Authorization", "Bearer " + authToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(user.getEmail()) 
				.accept(MediaType.APPLICATION_JSON))
		.andExpect(status().isForbidden())
		.andExpect(content().contentType("application/json;charset=UTF-8"))
		.andExpect(jsonPath("$.message").value("Access denied: the user does not have the required role to access this resource"))
		.andExpect(jsonPath("$.status").value("FORBIDDEN"));
		
		User userAfterAttempt = userRepository.findByUsername(user.getUsername()).get();
	    assertEquals(user.getAccountStatus(), userAfterAttempt.getAccountStatus(), 
	        "The account status should remain " + user.getAccountStatus() + " after the failed attempt.");

		// Clean-up
		testHelper.cleanDataBaseAndRedis();
	}
}
