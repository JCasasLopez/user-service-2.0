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
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.jcasaslopez.user.dto.StandardResponse;
import dev.jcasaslopez.user.entity.User;
import dev.jcasaslopez.user.enums.AccountStatus;
import dev.jcasaslopez.user.repository.UserRepository;
import dev.jcasaslopez.user.testhelper.TestHelper;
import dev.jcasaslopez.user.utilities.Constants;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UpdateAccountStatusIntegrationTest {
	
	@Autowired private UserRepository userRepository;
	@Autowired private TestHelper testHelper; 
	@Autowired private MockMvc mockMvc;
	@Autowired private ObjectMapper objectMapper;
	
	private static User user;
	private static String userEmail;
	private static final String username = "Yorch22";
	private static final String password = "Jorge22!";
	
	@BeforeAll
	void setup() {
		user = testHelper.createAndPersistUser(username, password);
		userEmail = user.getEmail();
	}
	
	@AfterAll
	void cleanup() {
	    testHelper.cleanDataBaseAndRedis();
	}
	
	@Test
	@Order(1)
	@DisplayName("Plain user cannot update account status")
	@WithMockUser(username = "plain_user", roles = {"USER"})
	public void updateAccountStatus_WhenUserNotAdmin_ShouldReturn403Forbidden() throws Exception {
		// Arrange
		AccountStatus newAccountStatus = AccountStatus.TEMPORARILY_BLOCKED;
		RequestBuilder requestBuilder = buildMockMvcRequest(newAccountStatus);

		// Act
		MvcResult mvcResult = callEndpointAndUReloadUser(requestBuilder);

		String responseAsString = mvcResult.getResponse().getContentAsString();
		StandardResponse response = objectMapper.readValue(responseAsString, StandardResponse.class);

		// Assert
		assertAll(
		    () -> assertEquals(403, mvcResult.getResponse().getStatus(),
		        "HTTP status should be 403 Forbidden"),
		    () -> assertNotNull(response.getMessage(), "Response body should not be null"),
		    () -> assertTrue(response.getMessage().contains("Access denied"),
		        "Unexpected response message"),
		    () -> assertFalse(user.getAccountStatus() == newAccountStatus,
		        "Account status is " + user.getAccountStatus() + " but should be " + AccountStatus.PERMANENTLY_SUSPENDED)
		);
	}
	
	@Test
	@Order(2)
	@DisplayName("Admin updates account status successfully")
	@WithMockUser(username = "admin", roles = {"ADMIN"})
	public void updateAccountStatus_WhenUserAdmin_ShouldReturn200Ok() throws Exception{
		// Arrange
		AccountStatus newAccountStatus = AccountStatus.TEMPORARILY_BLOCKED;
		RequestBuilder requestBuilder = buildMockMvcRequest(newAccountStatus);

		// Act
		MvcResult mvcResult = callEndpointAndUReloadUser(requestBuilder);

		String responseAsString = mvcResult.getResponse().getContentAsString();
		StandardResponse response = objectMapper.readValue(responseAsString, StandardResponse.class);

		// Assert
		assertAll(
				() -> assertEquals(200, mvcResult.getResponse().getStatus(), 
						"HTTP status should be 200 OK"),
				() -> assertNotNull(response.getMessage(), "Response body should not be null"),
				() -> assertTrue(response.getMessage().contains("status successfully updated"), 
						"Unexpected response message"),
				() -> assertTrue(user.getAccountStatus() == newAccountStatus,
				        "Account has unexpected status after the test")
			);
	}
	
	@Test
	@Order(3)
	@DisplayName("Throws exception when trying to update to the same status")
	@WithMockUser(username = "admin", roles = {"ADMIN"})
	public void updateAccountStatus_WhenNewStatusIsSame_ShouldThrowException() throws Exception{
		// Arrange
		AccountStatus newAccountStatus = AccountStatus.TEMPORARILY_BLOCKED;
		RequestBuilder requestBuilder = buildMockMvcRequest(newAccountStatus);

		// Act
		MvcResult mvcResult = callEndpointAndUReloadUser(requestBuilder);

		String responseAsString = mvcResult.getResponse().getContentAsString();
		StandardResponse response = objectMapper.readValue(responseAsString, StandardResponse.class);

		// Assert
		assertAll(
				() -> assertEquals(409, mvcResult.getResponse().getStatus(), "HTTP status should be 409 CONFLICT"),
				() -> assertNotNull(response.getMessage(), "Response body should not be null"),
				() -> assertEquals("The account already has the specified status",
								response.getMessage(), "Unexpected response message")
				);
	}
	
	@Test
	@Order(4)
	@DisplayName("Throws exception when trying to update a permanently suspended account")
	@WithMockUser(username = "admin", roles = {"ADMIN"})
	public void updateAccountStatus_WhenAccountIsPermanentlySuspended_ShouldThrowException() throws Exception{
		// Arrange
		// Set the account status to 'PERMANENTLY SUSPENDED' using the endpoint to decouple base code and tests.
		AccountStatus newAccountStatus = AccountStatus.PERMANENTLY_SUSPENDED;
		RequestBuilder requestBuilder = buildMockMvcRequest(newAccountStatus);
		callEndpointAndUReloadUser(requestBuilder);

		// Act
		// Update the account to 'ACTIVE'. The result of this call is the test real subject.
		AccountStatus accountStatusSecondCall = AccountStatus.ACTIVE;
		RequestBuilder requestBuilderSecondCall = buildMockMvcRequest(accountStatusSecondCall);
		MvcResult mvcResultSecondCall = callEndpointAndUReloadUser(requestBuilderSecondCall);

		String responseAsString = mvcResultSecondCall.getResponse().getContentAsString();
		StandardResponse response = objectMapper.readValue(responseAsString, StandardResponse.class);

		// Assert
		assertAll(
				() -> assertEquals(409, mvcResultSecondCall.getResponse().getStatus(), "HTTP status should be 409 CONFLICT"),
				() -> assertNotNull(response.getMessage(), "Response body should not be null"),
				() -> assertEquals("Cannot change status: the account is permanently suspended",
								response.getMessage(), "Unexpected response message")
				);
	}

	
	// ************** HELPER METHODS **************
	// These two helper methods are shared by UpdateAccountStatusIntegrationTest and UpgradeUserIntegrationTest,
	// the second one is identical in both cases, the first one requires only slight modifications. However,
	// attempts to take them to a helper class to avoid code repetition render the code way more complex and difficult
	// to follow, so the decision to keep the code repeated is a conscious trade-off.
		
	private RequestBuilder buildMockMvcRequest(AccountStatus newAccountStatus) throws JsonProcessingException {
		return MockMvcRequestBuilders
		        .put(Constants.UPDATE_ACCOUNT_STATUS_PATH)
		        .param("newAccountStatus", newAccountStatus.name())
		        .contentType(MediaType.APPLICATION_JSON)
		        .content(userEmail)
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