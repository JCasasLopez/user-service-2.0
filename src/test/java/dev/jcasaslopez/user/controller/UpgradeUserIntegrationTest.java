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

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.jcasaslopez.user.dto.StandardResponse;
import dev.jcasaslopez.user.entity.User;
import dev.jcasaslopez.user.enums.RoleName;
import dev.jcasaslopez.user.repository.RoleRepository;
import dev.jcasaslopez.user.repository.UserRepository;
import dev.jcasaslopez.user.testhelper.TestHelper;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UpgradeUserIntegrationTest {
	
	@Autowired private RoleRepository roleRepository;
	@Autowired private UserRepository userRepository;
	@Autowired private TestHelper testHelper; 
	@Autowired private MockMvc mockMvc;
	@Autowired private ObjectMapper objectMapper;
	
	private static User user;
	private static String userEmail;
	
	@BeforeAll
	void setup() {
		user = testHelper.createUser("Yorch22", "Jorge22!");
		userEmail = user.getEmail();	
	}
	
	@AfterAll
	void cleanup() {
	    testHelper.cleanDataBaseAndRedis();
	}
	
	@Test
	@Order(1)
	@DisplayName("Admin cannot upgrade user to admin")
	@WithMockUser(username = "admin", roles = {"ADMIN"})
	public void upgradeUser_WhenAdminTriesToUpgrade_ShouldReturn403Forbidden() throws Exception {
		// Arrange
		RequestBuilder requestBuilder = buildMockMvcRequest();
		
		// Act
		MvcResult mvcResult = callEndpointAndUReloadUser(requestBuilder);
				
		String responseAsString = mvcResult.getResponse().getContentAsString();
		StandardResponse response = objectMapper.readValue(responseAsString, StandardResponse.class);
		
		// Assert
		assertAll(
				() -> assertEquals(403, mvcResult.getResponse().getStatus(), 
						"HTTP status should be 403 Forbidden"),
				() -> assertNotNull(response.getMessage(), 
			    		"Response body should not be null"),
			    () -> assertEquals("Access denied: the user does not have the required role "
			    		+ "to access this resource", response.getMessage(), 
			    		"Unexpected response message"),
			    () -> assertFalse(user.getRoles()
			    		.contains(roleRepository.findByRoleName(RoleName.ROLE_ADMIN).get()), 
			    		user.getUsername() + " should not have the role admin")
			);
	}
	
	@Test
	@Order(2)
	@DisplayName("SuperAdmin upgrades user successfully")
	@WithMockUser(username = "superAdmin", roles = {"SUPERADMIN"})
	public void upgradeUser_WhenSuperAdminUpgrades_ShouldReturn200OK() throws Exception {
		// Arrange
		RequestBuilder requestBuilder = buildMockMvcRequest();
		
		// Act
		MvcResult mvcResult = callEndpointAndUReloadUser(requestBuilder);
		
		String responseAsString = mvcResult.getResponse().getContentAsString();
		StandardResponse response = objectMapper.readValue(responseAsString, StandardResponse.class);
		
		// Assert
		assertAll(
				() -> assertEquals(200, mvcResult.getResponse().getStatus(), 
						"HTTP status should be 200 OK"), 
			    () -> assertNotNull(response.getMessage(), 
			    		"Response body should not be null"),
			    () -> assertEquals("User upgraded successfully to admin", response.getMessage(), 
			    		"Unexpected response message"),
			    () -> assertTrue(user.getRoles()
			    		.contains(roleRepository.findByRoleName(RoleName.ROLE_ADMIN).get()), 
			    		user.getUsername() + " should have the role admin")
			);
	}
	
	@Test
	@Order(3)
	@DisplayName("SuperAdmin cannot upgrade user already admin")
	@WithMockUser(username = "superAdmin", roles = {"SUPERADMIN"})
	public void upgradeUser_WhenUserAlreadyAdmin_ShouldThrowException() throws Exception {
		// Arrange
		RequestBuilder requestBuilder = buildMockMvcRequest();
		
		// Act
	    MvcResult mvcResult = mockMvc.perform(requestBuilder).andReturn();
	    String responseAsString = mvcResult.getResponse().getContentAsString();
	    StandardResponse response = objectMapper.readValue(responseAsString, StandardResponse.class);

	    // Assert
	    assertAll(
	        () -> assertEquals(400, mvcResult.getResponse().getStatus(), 
	        		"HTTP status should be 400 Bad Request"),
	        () -> assertNotNull(response.getMessage(), "Response should contain error message"),
	        () -> assertEquals("User is already ADMIN", response.getMessage(), 
	        		"Unexpected error message")
	    );
	}
	
	
	// Helper methods to reduce boilerplate code.
	
	private RequestBuilder buildMockMvcRequest() {
		return MockMvcRequestBuilders
				.put("/upgradeUser")
				.contentType(MediaType.APPLICATION_JSON)
				.content(userEmail)
				.accept(MediaType.APPLICATION_JSON);
	}
	
	private MvcResult callEndpointAndUReloadUser(RequestBuilder requestBuilder) throws Exception {
		MvcResult mvcResult = mockMvc.perform(requestBuilder).andReturn();
		
		// Reloads user from the database with the upgraded roles.
		user = userRepository.findById(user.getIdUser()).orElseThrow(
				() -> new UsernameNotFoundException(user.getUsername() + " was not found in the database"));
		return mvcResult;
	}
}