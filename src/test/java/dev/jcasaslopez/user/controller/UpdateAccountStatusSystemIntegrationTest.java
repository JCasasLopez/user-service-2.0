package dev.jcasaslopez.user.controller;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.util.UriComponentsBuilder;

import dev.jcasaslopez.user.dto.StandardResponse;
import dev.jcasaslopez.user.entity.User;
import dev.jcasaslopez.user.enums.AccountStatus;
import dev.jcasaslopez.user.enums.RoleName;
import dev.jcasaslopez.user.repository.UserRepository;
import dev.jcasaslopez.user.testhelper.AuthenticationTestHelper;
import dev.jcasaslopez.user.testhelper.TestHelper;
import dev.jcasaslopez.user.testhelper.UserTestBuilder;
import dev.jcasaslopez.user.utilities.Constants;

// NOTE ON AUTHENTICATION:
// We use manual E2E login (obtaining real JWT) instead of using @WithMockUser.
// With @WithMockUser, the AuthenticationFilter does not even check SecurityContext, because its logic checks
// for a valid header and token first, causing 401 UNAUTHORIZED instead of the expected 
// 403 FORBIDDEN for authorization failures.

@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UpdateAccountStatusSystemIntegrationTest {
	
	@Autowired private UserRepository userRepository;
	@Autowired private TestHelper testHelper; 
	@Autowired private AuthenticationTestHelper authTestHelper;
	@Autowired private TestRestTemplate testRestTemplate;
	
	private User user;
	private String authToken;
	
	private static final String ADMIN_USER_USERNAME = "userAdmin";
	private static final String ADMIN_USER_PASSWORD = "Password456!";
	
	@BeforeEach
	void persistAndLogInUser() throws Exception {
		// This test defines 2 different users:
		// 1) Target user: The account subject to modification. 
		UserTestBuilder builderUser = new UserTestBuilder("Yorch22", "Password123!");
		user = testHelper.createAndPersistUser(builderUser);
		
		// 2) Admin user: Performs successful modification (expected to succeed - 200 OK).
		UserTestBuilder builderAdminUser = new UserTestBuilder(ADMIN_USER_USERNAME, ADMIN_USER_PASSWORD).withRole(RoleName.ROLE_ADMIN);
		testHelper.createAndPersistUser(builderAdminUser);	
		
		authToken = authTestHelper.logInWithTestRestTemplate(ADMIN_USER_USERNAME, ADMIN_USER_PASSWORD).getAccessToken();
	}
	
	@AfterEach
	void cleanup() {
	    testHelper.cleanDataBaseAndRedis();
	}
	
	@Test
	@DisplayName("Admin updates account status successfully")
	public void updateAccountStatus_WhenUserAdmin_ShouldReturn200Ok() throws Exception{
		// Arrange		
		AccountStatus newAccountStatus = AccountStatus.TEMPORARILY_BLOCKED;

		// Act
		ResponseEntity<StandardResponse> response = updateAccountStatus(authToken, user, newAccountStatus);
	    user = reloadUser();

		// Assert
		assertAll(
				() -> assertEquals(HttpStatus.OK, response.getBody().getStatus(), "HTTP status should be 200 OK"),
				() -> assertNotNull(response.getBody(), "Response body should not be null"),
				() -> assertTrue(response.getBody().getMessage().contains("status successfully updated"), "Unexpected response message"),
				() -> assertTrue(user.getAccountStatus() == newAccountStatus, "Account has unexpected status after the test")
			);
	}
	
	@Test
	@DisplayName("Throws exception when trying to update to the same status")
	public void updateAccountStatus_WhenNewStatusIsSame_ShouldThrowException() throws Exception{
		// Arrange		
		// The default account status is ACTIVE, so trying to update it to the same status, should throw an exception.
		AccountStatus newAccountStatus = AccountStatus.ACTIVE;

		// Act
		ResponseEntity<StandardResponse> response = updateAccountStatus(authToken, user, newAccountStatus);
	    user = reloadUser();

		// Assert
		assertAll(
				() -> assertEquals(HttpStatus.CONFLICT, response.getBody().getStatus(), "HTTP status should be 409 CONFLICT"),
				() -> assertNotNull(response.getBody(), "Response body should not be null"),
				() -> assertEquals("The account already has the specified status", response.getBody().getMessage(), "Unexpected response message")
				);
	}
	
	@Test
	@DisplayName("Throws exception when trying to update a permanently suspended account")
	public void updateAccountStatus_WhenAccountIsPermanentlySuspended_ShouldThrowException() throws Exception{
		// Arrange
		AccountStatus newAccountStatus = AccountStatus.PERMANENTLY_SUSPENDED;
		updateAccountStatus(authToken, user, newAccountStatus);

		// Act
		// Update the account to 'ACTIVE'. The result of this call is the test real subject.
		AccountStatus accountStatusSecondCall = AccountStatus.ACTIVE;
		ResponseEntity<StandardResponse> response = updateAccountStatus(authToken, user, accountStatusSecondCall);
	    user = reloadUser();

		// Assert
		assertAll(
				() -> assertEquals(HttpStatus.CONFLICT, response.getBody().getStatus(), "HTTP status should be 409 CONFLICT"),
				() -> assertNotNull(response.getBody(), "Response body should not be null"),
				() -> assertEquals("Cannot change status: the account is permanently suspended", response.getBody().getMessage(), "Unexpected response message")
				);
	}
	
	// ************** HELPER METHODS **************
	
	private ResponseEntity<StandardResponse> updateAccountStatus(String authToken, User targetUser, AccountStatus newAccountStatus) throws Exception {
		HttpHeaders headers = new HttpHeaders();
	    headers.setContentType(MediaType.APPLICATION_JSON);
	    headers.setBearerAuth(authToken);
	    headers.setAccept(List.of(MediaType.APPLICATION_JSON));
	    
	    String url = UriComponentsBuilder.fromPath(Constants.UPDATE_ACCOUNT_STATUS_PATH)
	        .queryParam("newAccountStatus", newAccountStatus.name())
	        .toUriString();
	    
	    String userEmailInBody = targetUser.getEmail(); 
	    HttpEntity<String> request = new HttpEntity<>(userEmailInBody, headers);
	    return testRestTemplate.exchange(url, HttpMethod.PUT, request, StandardResponse.class);
	}

	private User reloadUser() {
	    return userRepository.findById(user.getIdUser())
	        .orElseThrow(() -> new UsernameNotFoundException(user.getUsername() + " was not found in the database"));
	}
}