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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.jcasaslopez.user.dto.StandardResponse;
import dev.jcasaslopez.user.entity.Role;
import dev.jcasaslopez.user.entity.User;
import dev.jcasaslopez.user.enums.AccountStatus;
import dev.jcasaslopez.user.enums.RoleName;
import dev.jcasaslopez.user.repository.RoleRepository;
import dev.jcasaslopez.user.repository.UserRepository;
import dev.jcasaslopez.user.testhelper.TestHelper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UpdateAccountStatusIntegrationTest {
	
	@Autowired private RoleRepository roleRepository;
	@Autowired private UserRepository userRepository;
	@Autowired private TestHelper testHelper; 
	@Autowired private MockMvc mockMvc;
	@Autowired private ObjectMapper objectMapper;
	
	private static User user;
	
	@BeforeAll
	void setup() {
	    if (roleRepository.count() == 0) {
	        roleRepository.save(new Role(RoleName.ROLE_USER));
	        roleRepository.save(new Role(RoleName.ROLE_ADMIN));
	        roleRepository.save(new Role(RoleName.ROLE_SUPERADMIN));
	    }	    
		user = testHelper.createUser();
	}
	
	@AfterAll
	void cleanup() {
	    userRepository.deleteById(user.getIdUser());
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
				() -> assertEquals(403, mvcResult.getResponse().getStatus(), "HTTP status should be 403 Forbidden"),
				() -> assertNotNull(response.getMessage(), "Response body should not be null"),
				() -> assertTrue(response.getMessage().contains("Access denied"), "Unexpected response message"),
				() -> assertFalse(user.getAccountStatus() == newAccountStatus)
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
				() -> assertEquals(200, mvcResult.getResponse().getStatus(), "HTTP status should be 200 OK"),
				() -> assertNotNull(response.getMessage(), "Response body should not be null"),
				() -> assertTrue(response.getMessage().contains("status successfully updated"), 
						"Unexpected response message"),
				() -> assertTrue(user.getAccountStatus() == newAccountStatus));
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
		
		// Primero, establecemos 'account status' como suspendida permanentemente. 
		// En vez de usar el 'setter', usamos el endpoint siguiendo las buenas prácticas de desacoplamiento.
		//
		// First of all, we set the account status to 'permanetly suspended'.
		// Instad of using the 'setter', we use the endpoint following good practices to decouple 
		// base code and tests.
		AccountStatus newAccountStatus = AccountStatus.PERMANENTLY_SUSPENDED;
		RequestBuilder requestBuilder = buildMockMvcRequest(newAccountStatus);
		callEndpointAndUReloadUser(requestBuilder);

		// Act
		
		// Después, intentamos actualizar la cuenta a 'activa'. El resultado de esta llamada es 
		// el sujeto real del test.
		//
		// Secondly, we try to update the account to 'active'. The result of this call is the 
		// test's real subject.
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
	
	// Métodos auxiliares para reducir código repetido.
	//
	// Helper methods to reduce boilerplate code.
	private RequestBuilder buildMockMvcRequest(AccountStatus newAccountStatus) {
		return MockMvcRequestBuilders
				.put("/updateAccountStatus")
				.param("email", user.getEmail())
				.param("newAccountStatus", newAccountStatus.name())
				.accept(MediaType.APPLICATION_JSON);
	}

	// Ejecuta la petición y recarga la entidad 'user' desde la base de datos,
	// simulando un ciclo completo de solicitud-controlador-base de datos.
	// 
	// Performs the request and refreshes the 'user' entity from the database,
	// simulating a full controller-to-database roundtrip.
	private MvcResult callEndpointAndUReloadUser(RequestBuilder requestBuilder) throws Exception {
		MvcResult mvcResult = mockMvc.perform(requestBuilder).andReturn();

		// Recarga el usuario desde la base de datos con los roles ya actualizados.
		//
		// Reloads user from the database with the upgraded roles.
		user = userRepository.findById(user.getIdUser()).orElseThrow(
				() -> new UsernameNotFoundException(user.getUsername() + " was not found in the database"));
		return mvcResult;
	}
}