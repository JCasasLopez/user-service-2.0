package dev.jcasaslopez.user.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import dev.jcasaslopez.user.dto.StandardResponse;
import dev.jcasaslopez.user.dto.UserDto;
import dev.jcasaslopez.user.enums.AccountStatus;
import dev.jcasaslopez.user.service.AccountOrchestrationService;
import dev.jcasaslopez.user.utilities.Constants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@CrossOrigin("*")
@RestController
public class UserController {

	private AccountOrchestrationService accountOrchestrationService;

	public UserController(AccountOrchestrationService accountOrchestrationService) {
		this.accountOrchestrationService = accountOrchestrationService;
	}

	@PostMapping(value = Constants.INITIATE_REGISTRATION_PATH, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<StandardResponse> initiateRegistration(@Valid @RequestBody UserDto user) 
			throws JsonProcessingException{
		accountOrchestrationService.initiateRegistration(user);
		StandardResponse response = new StandardResponse(LocalDateTime.now(),
				"Token created successfully and sent to the user to verify email", null, HttpStatus.OK);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
	
	@PostMapping(value = Constants.REGISTRATION_PATH)
	public ResponseEntity<StandardResponse> createAccount(HttpServletRequest request) 
			throws JsonMappingException, JsonProcessingException{
		accountOrchestrationService.userRegistration(request);
		StandardResponse response = new StandardResponse(LocalDateTime.now(),
				"Account created successfully", null, HttpStatus.CREATED);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
	
	@DeleteMapping(value = "/deleteAccount")
	public ResponseEntity<StandardResponse> deleteAccount() {
		accountOrchestrationService.deleteAccount();
		StandardResponse response = new StandardResponse(LocalDateTime.now(),
				"Account deleted successfully", null, HttpStatus.OK);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
	
	@PostMapping(value = Constants.FORGOT_PASSWORD_PATH)
	public ResponseEntity<StandardResponse> forgotPassword(@RequestParam String email) {
		accountOrchestrationService.forgotPassword(email);
		StandardResponse response = new StandardResponse(LocalDateTime.now(),
				"Token created successfully and sent to the user to reset password", null, HttpStatus.OK);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
	
	@PutMapping(value = Constants.RESET_PASSWORD_PATH)
	public ResponseEntity<StandardResponse> resetPassword(@RequestParam String newPassword, 
			HttpServletRequest request) {
		accountOrchestrationService.resetPassword(newPassword, request);
		StandardResponse response = new StandardResponse(LocalDateTime.now(),
				"Password reset successfully", null, HttpStatus.OK);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
	
	@PutMapping(value = "/upgradeUser")
	public ResponseEntity<StandardResponse> upgradeUser(@RequestParam String email) {
		accountOrchestrationService.upgradeUser(email);
		StandardResponse response = new StandardResponse(LocalDateTime.now(),
				"User upgraded successfully to admin", null, HttpStatus.OK);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
	
	@PutMapping(value = "/changePassword")
	public ResponseEntity<StandardResponse> changePassword(@RequestParam String oldPassword, 
			@RequestParam String newPassword) {
		accountOrchestrationService.changePassword(oldPassword, newPassword);
		StandardResponse response = new StandardResponse(LocalDateTime.now(),
				"Password changed successfully", null, HttpStatus.OK);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
	
	// Spring permite pasar una enumeración como parámetro en un @RequestParam,
	// pero solo si el valor en la URL coincide exactamente con el nombre del enum.
	//
	// Spring allows passing an enumeration as a parameter in a @RequestParam,
	// but only if the value in the URL exactly matches the enum name.
	//
	// Accepted values: ACTIVE, TEMPORARILY_BLOCKED, PERMANENTLY_SUSPENDED
	@PutMapping(value = "/updateAccountStatus")
	public ResponseEntity<StandardResponse> updateAccountStatus(@RequestParam String email, 
			@RequestParam AccountStatus newAccountStatus) {
		accountOrchestrationService.updateAccountStatus(email, newAccountStatus);
		StandardResponse response = new StandardResponse(LocalDateTime.now(),
				"Account status successfully update to " + newAccountStatus.getDisplayName(), null, HttpStatus.OK);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
	
	// Otros servicios pueden usar este endpoint enviando un mensaje modelado como HashMap:
	// 
	// Other services can use this endpoint by sending the message as a HashMap:
	//
	// Example:
	// "Recipient" : String(idUser)
	// "Subject"   : String
	// "Message"   : String
	@PostMapping(value = "/sendNotification")
	public ResponseEntity<StandardResponse> sendNotification(@RequestParam Map<String, String> messageAsMap) {
		accountOrchestrationService.sendNotification(messageAsMap);
		StandardResponse response = new StandardResponse(LocalDateTime.now(),
				"Notification sent successfully", null, HttpStatus.OK);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
	
	@PostMapping(value  = Constants.REFRESH_TOKEN_PATH)
	public ResponseEntity<StandardResponse> refreshToken() {
		List<String> tokens = accountOrchestrationService.refreshToken();
		StandardResponse response = new StandardResponse(LocalDateTime.now(),
				"New refresh and access tokens sent successfully", tokens, HttpStatus.OK);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
}