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

import dev.jcasaslopez.user.dto.StandardResponse;
import dev.jcasaslopez.user.entity.User;
import dev.jcasaslopez.user.enums.RoleName;
import dev.jcasaslopez.user.repository.RoleRepository;
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
public class UpgradeUserSystemIntegrationTest {
	
	@Autowired private RoleRepository roleRepository;
	@Autowired private UserRepository userRepository;
	@Autowired private TestHelper testHelper; 
	@Autowired private AuthenticationTestHelper authTestHelper;
	@Autowired private TestRestTemplate testRestTemplate;
	
	private User user;
	private String authToken;
	
	private static final String SUPER_ADMIN_USER_USERNAME = "userSuperAdmin";
	private static final String SUPER_ADMIN_USER_PASSWORD = "Password456!";
	
	@BeforeEach
	void persistAndLogInUser() throws Exception {
		// This test defines 2 different users:
		// 1) Target user: The account subject to modification. 
		UserTestBuilder builderUser = new UserTestBuilder("Yorch22", "Password123!");
		user = testHelper.createAndPersistUser(builderUser);
		
		// 2) Super admin user: Performs or tries to perform modification.
		UserTestBuilder builderAdminUser = new UserTestBuilder(SUPER_ADMIN_USER_USERNAME, SUPER_ADMIN_USER_PASSWORD).withRole(RoleName.ROLE_SUPERADMIN);
		testHelper.createAndPersistUser(builderAdminUser);	
		
		authToken = authTestHelper.logInWithTestRestTemplate(SUPER_ADMIN_USER_USERNAME, SUPER_ADMIN_USER_PASSWORD).getAccessToken();
	}
	
	@AfterEach
	void cleanup() {
	    testHelper.cleanDataBaseAndRedis();
	}
	
	@Test
	@DisplayName("SuperAdmin upgrades user successfully")
	public void upgradeUser_WhenSuperAdminUpgrades_ShouldReturn200OK() throws Exception {
		// Act
		ResponseEntity<StandardResponse> response = upgradeUser(authToken, user);
	    user = reloadUser();
		
		// Assert
		assertAll(
				() -> assertEquals(HttpStatus.OK, response.getBody().getStatus(), "HTTP status should be 200 OK"), 
			    () -> assertNotNull(response.getBody().getMessage(), "Response body should not be null"),
			    () -> assertEquals("User upgraded successfully to admin", response.getBody().getMessage(), "Unexpected response message"),
			    () -> assertTrue(user.getRoles().contains(roleRepository.findByRoleName(RoleName.ROLE_ADMIN).get()), 
			    		user.getUsername() + " should have the role admin")
			);
	}
	
	@Test
	@DisplayName("SuperAdmin cannot upgrade user already admin")
	public void upgradeUser_WhenUserAlreadyAdmin_ShouldThrowException() throws Exception {
		// Act
		// First, we upgrade the user to admin.
		upgradeUser(authToken, user);		
		// Then, we try to upgrade it again. The result of this call is the real test subject.
		ResponseEntity<StandardResponse> response = upgradeUser(authToken, user);
		user = reloadUser();

	    // Assert
	    assertAll(
	        () -> assertEquals(HttpStatus.BAD_REQUEST, response.getBody().getStatus(), "HTTP status should be 400 Bad Request"),
	        () -> assertNotNull(response.getBody(), "Response should contain error message"),
	        () -> assertEquals("User is already ADMIN", response.getBody().getMessage(), "Unexpected error message")
	    );
	}
	
	// ************** HELPER METHODS **************
		
	private ResponseEntity<StandardResponse> upgradeUser(String authToken, User targetUser) throws Exception {
		HttpHeaders headers = new HttpHeaders();
	    headers.setContentType(MediaType.APPLICATION_JSON);
	    headers.setBearerAuth(authToken);
	    headers.setAccept(List.of(MediaType.APPLICATION_JSON));
	    
	    String userEmailInBody = user.getEmail(); 
	    HttpEntity<String> request = new HttpEntity<>(userEmailInBody, headers);
	    return testRestTemplate.exchange(Constants.UPGRADE_USER_PATH, HttpMethod.PUT, request, StandardResponse.class);
	}
	
	private User reloadUser() {
	    return userRepository.findById(user.getIdUser())
	        .orElseThrow(() -> new UsernameNotFoundException(user.getUsername() + " was not found in the database"));
	}
}