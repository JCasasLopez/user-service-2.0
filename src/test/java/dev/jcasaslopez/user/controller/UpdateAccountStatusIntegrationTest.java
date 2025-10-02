package dev.jcasaslopez.user.controller;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UpdateAccountStatusIntegrationTest {
	
	@Autowired private UserRepository userRepository;
	@Autowired private TestHelper testHelper; 
	@Autowired private AuthenticationTestHelper authTestHelper;
	@Autowired private MockMvc mockMvc;
	@Autowired private ObjectMapper objectMapper;
	
	// This test defines 3 different users:
	// 1) Target user: The account subject to modification. Shared across tests.
	private static User user;
	private static final String USERNAME = "Yorch22";
	private static final String ORIGINAL_PASSWORD = "Password123!";

	// 2) Plain user: Attempts modification (expected to fail - 403 Forbidden).
	private static final String PLAIN_USER_USERNAME = "plainUser";
	private static final String PLAIN_USER_PASSWORD = "Password456!";
	
	// 3) Admin user: Performs successful modification (expected to succeed - 200 OK).
	private static final String ADMIN_USER_USERNAME = "userAdmin";
	private static final String ADMIN_USER_PASSWORD = "Password789!";
	
	// Static variable to share state between the ordered test methods.
	private static String authToken;

	@BeforeAll
	void setup() {
		UserTestBuilder builderUser = new UserTestBuilder(USERNAME, ORIGINAL_PASSWORD);
		user = testHelper.createAndPersistUser(builderUser);
		
		UserTestBuilder builderPlainUser = new UserTestBuilder(PLAIN_USER_USERNAME, PLAIN_USER_PASSWORD);
		testHelper.createAndPersistUser(builderPlainUser);
		
		UserTestBuilder builderAdminUser = new UserTestBuilder(ADMIN_USER_USERNAME, ADMIN_USER_PASSWORD)
														.withRole(RoleName.ROLE_ADMIN);
		testHelper.createAndPersistUser(builderAdminUser);
	}
	
	@AfterAll
	void cleanup() {
	    testHelper.cleanDataBaseAndRedis();
	}
	
	@Test
	@Order(1)
	@DisplayName("Plain user cannot update account status")
	public void updateAccountStatus_WhenUserNotAdmin_ShouldReturn403Forbidden() throws Exception {
		// Arrange
		authToken = authTestHelper.logInWithMockMvc(PLAIN_USER_USERNAME, PLAIN_USER_PASSWORD).getAccessToken();
		AccountStatus newAccountStatus = AccountStatus.TEMPORARILY_BLOCKED;
		RequestBuilder requestBuilder = buildMockMvcRequest(newAccountStatus, authToken);

		// Act
		MvcResult mvcResult = callEndpointAndUReloadUser(requestBuilder);

		String responseAsString = mvcResult.getResponse().getContentAsString();
		StandardResponse response = objectMapper.readValue(responseAsString, StandardResponse.class);

		// Assert
		assertAll(
		    () -> assertEquals(403, mvcResult.getResponse().getStatus(), "HTTP status should be 403 Forbidden"),
		    () -> assertNotNull(response.getMessage(), "Response body should not be null"),
		    () -> assertTrue(response.getMessage().contains("Access denied"), "Unexpected response message"),
		    () -> assertFalse(user.getAccountStatus() == newAccountStatus, "Account status is " + user.getAccountStatus() + " but should be " + AccountStatus.PERMANENTLY_SUSPENDED)
		);
	}
	
	@Test
	@Order(2)
	@DisplayName("Admin updates account status successfully")
	public void updateAccountStatus_WhenUserAdmin_ShouldReturn200Ok() throws Exception{
		// Arrange
		authToken = authTestHelper.logInWithMockMvc(ADMIN_USER_USERNAME, ADMIN_USER_PASSWORD).getAccessToken();
		AccountStatus newAccountStatus = AccountStatus.TEMPORARILY_BLOCKED;
		RequestBuilder requestBuilder = buildMockMvcRequest(newAccountStatus, authToken);

		// Act
		MvcResult mvcResult = callEndpointAndUReloadUser(requestBuilder);

		String responseAsString = mvcResult.getResponse().getContentAsString();
		StandardResponse response = objectMapper.readValue(responseAsString, StandardResponse.class);

		// Assert
		assertAll(
				() -> assertEquals(200, mvcResult.getResponse().getStatus(), "HTTP status should be 200 OK"),
				() -> assertNotNull(response.getMessage(), "Response body should not be null"),
				() -> assertTrue(response.getMessage().contains("status successfully updated"), "Unexpected response message"),
				() -> assertTrue(user.getAccountStatus() == newAccountStatus, "Account has unexpected status after the test")
			);
	}
	
	@Test
	@Order(3)
	@DisplayName("Throws exception when trying to update to the same status")
	public void updateAccountStatus_WhenNewStatusIsSame_ShouldThrowException() throws Exception{
		// Arrange
		// Admin user already logged in. We re-use the token received then.
		AccountStatus newAccountStatus = AccountStatus.TEMPORARILY_BLOCKED;
		RequestBuilder requestBuilder = buildMockMvcRequest(newAccountStatus, authToken);

		// Act
		MvcResult mvcResult = callEndpointAndUReloadUser(requestBuilder);

		String responseAsString = mvcResult.getResponse().getContentAsString();
		StandardResponse response = objectMapper.readValue(responseAsString, StandardResponse.class);

		// Assert
		assertAll(
				() -> assertEquals(409, mvcResult.getResponse().getStatus(), "HTTP status should be 409 CONFLICT"),
				() -> assertNotNull(response.getMessage(), "Response body should not be null"),
				() -> assertEquals("The account already has the specified status", response.getMessage(), "Unexpected response message")
				);
	}
	
	@Test
	@Order(4)
	@DisplayName("Throws exception when trying to update a permanently suspended account")
	public void updateAccountStatus_WhenAccountIsPermanentlySuspended_ShouldThrowException() throws Exception{
		// Arrange
		// Admin user already logged in. We re-use the token received then.
		// Set the account status to 'PERMANENTLY SUSPENDED' using the endpoint to decouple base code and tests.
		AccountStatus newAccountStatus = AccountStatus.PERMANENTLY_SUSPENDED;
		RequestBuilder requestBuilder = buildMockMvcRequest(newAccountStatus, authToken);
		callEndpointAndUReloadUser(requestBuilder);

		// Act
		// Update the account to 'ACTIVE'. The result of this call is the test real subject.
		AccountStatus accountStatusSecondCall = AccountStatus.ACTIVE;
		RequestBuilder requestBuilderSecondCall = buildMockMvcRequest(accountStatusSecondCall, authToken);
		MvcResult mvcResultSecondCall = callEndpointAndUReloadUser(requestBuilderSecondCall);

		String responseAsString = mvcResultSecondCall.getResponse().getContentAsString();
		StandardResponse response = objectMapper.readValue(responseAsString, StandardResponse.class);

		// Assert
		assertAll(
				() -> assertEquals(409, mvcResultSecondCall.getResponse().getStatus(), "HTTP status should be 409 CONFLICT"),
				() -> assertNotNull(response.getMessage(), "Response body should not be null"),
				() -> assertEquals("Cannot change status: the account is permanently suspended", response.getMessage(), "Unexpected response message")
				);
	}
	
	// ************** HELPER METHODS **************
	// These three helper methods are shared by UpdateAccountStatusIntegrationTest and UpgradeUserIntegrationTest,
	// the second one is identical in both cases, the first one requires only slight modifications. However,
	// attempts to take them to a helper class to avoid code repetition render the code way more complex and difficult
	// to follow, so the decision to keep the code repeated is a conscious trade-off.
		
	private RequestBuilder buildMockMvcRequest(AccountStatus newAccountStatus, String authToken) throws JsonProcessingException {
				
		return MockMvcRequestBuilders
		        .put(Constants.UPDATE_ACCOUNT_STATUS_PATH)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken)
		        .param("newAccountStatus", newAccountStatus.name())
		        .contentType(MediaType.APPLICATION_JSON)
		        .content(user.getEmail())
		        .accept(MediaType.APPLICATION_JSON);
	}

	// Performs the request and refreshes user from the database, simulating a full controller-to-database roundtrip.
	private MvcResult callEndpointAndUReloadUser(RequestBuilder requestBuilder) throws Exception {
		MvcResult mvcResult = mockMvc.perform(requestBuilder).andReturn();

		// Reloads user from the database with the upgraded roles.
		user = userRepository.findById(user.getIdUser()).orElseThrow(
				() -> new UsernameNotFoundException(user.getUsername() + " was not found in the database"));
		return mvcResult;
	}
}