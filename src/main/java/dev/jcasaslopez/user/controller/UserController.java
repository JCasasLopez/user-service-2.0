package dev.jcasaslopez.user.controller;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.provisioning.UserDetailsManager;
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
import dev.jcasaslopez.user.enums.TokenType;
import dev.jcasaslopez.user.event.ForgotPasswordEvent;
import dev.jcasaslopez.user.event.VerifyEmailEvent;
import dev.jcasaslopez.user.mapper.UserMapper;
import dev.jcasaslopez.user.service.EmailService;
import dev.jcasaslopez.user.service.PasswordService;
import dev.jcasaslopez.user.service.TokenService;
import dev.jcasaslopez.user.service.UserAccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@CrossOrigin("*")
@RestController
public class UserController {

	private UserDetailsManager userDetailsManager;
	private TokenService tokenService;
	private ApplicationEventPublisher eventPublisher;
	private UserAccountService userAccountService;
	private PasswordService passwordService;
	private UserMapper userMapper;
	private EmailService emailService;
	
	public UserController(UserDetailsManager userDetailsManager, TokenService tokenService,
			ApplicationEventPublisher eventPublisher, UserAccountService userAccountService,
			PasswordService passwordService, UserMapper userMapper, EmailService emailService) {
		this.userDetailsManager = userDetailsManager;
		this.tokenService = tokenService;
		this.eventPublisher = eventPublisher;
		this.userAccountService = userAccountService;
		this.passwordService = passwordService;
		this.userMapper = userMapper;
		this.emailService = emailService;
	}

	@PostMapping(value = "/initiateUserRegistration", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<StandardResponse> initiateRegistration(@Valid @RequestBody UserDto user) {
		String verifyEmailToken = tokenService.createTokenUserNotAuthenticated
				(TokenType.VERIFY_EMAIL, user.getUsername());
		eventPublisher.publishEvent(new VerifyEmailEvent(user, verifyEmailToken));
		StandardResponse response = new StandardResponse(LocalDateTime.now(),
				"Token created successfully and sent to the user to verify email", null, HttpStatus.OK);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
	
	@PostMapping(value = "/userRegistration")
	public ResponseEntity<StandardResponse> createAccount(HttpServletRequest request) throws JsonMappingException, JsonProcessingException{
		// Atributo establecido en AuthenticationFilter.
		//
		// Attribute set in AuthenticationFilter.
		String token = (String) request.getAttribute("verificationToken");
		// En RegistrationService es donde publicaremos el evento.
		//
		// We will publish the event in RegistrationService.
		userAccountService.createAccount(token);
		StandardResponse response = new StandardResponse(LocalDateTime.now(),
				"Account created successfully", null, HttpStatus.CREATED);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
	
	@DeleteMapping(value = "/deleteAccount")
	public ResponseEntity<StandardResponse> deleteAccount() {
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		userDetailsManager.deleteUser(username);
		StandardResponse response = new StandardResponse(LocalDateTime.now(),
				"Account deleted successfully", null, HttpStatus.OK);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
	
	@PostMapping(value = "/forgotPassword")
	public ResponseEntity<StandardResponse> forgotPassword(@RequestParam String email) {
		UserDto user = userMapper.userToUserDtoMapper(userAccountService.findUserByEmail(email));
		String resetPasswordToken = tokenService.createTokenUserNotAuthenticated
				(TokenType.PASSWORD_RESET, user.getUsername());
		eventPublisher.publishEvent(new ForgotPasswordEvent(user, resetPasswordToken));
		StandardResponse response = new StandardResponse(LocalDateTime.now(),
				"Token created successfully and sent to the user to reset password", null, HttpStatus.OK);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
	
	@PostMapping(value = "/resetPassword")
	public ResponseEntity<StandardResponse> resetPassword(@RequestParam String newPassword, 
			HttpServletRequest request) {
		// Atributo establecido en AuthenticationFilter.
		//
		// Attribute set in AuthenticationFilter.
		String token = (String) request.getAttribute("verificationToken");
		passwordService.resetPassword(newPassword, token);
		StandardResponse response = new StandardResponse(LocalDateTime.now(),
				"Password reset successfully", null, HttpStatus.OK);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
	
	@PutMapping(value = "/upgradeUser")
	public ResponseEntity<StandardResponse> upgradeUser(@RequestParam String email) {
		userAccountService.upgradeUser(email);
		StandardResponse response = new StandardResponse(LocalDateTime.now(),
				"User with email " + email + " upgraded successfully to admin", null, HttpStatus.OK);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
	
	@PostMapping(value = "/changePassword")
	public ResponseEntity<StandardResponse> changePassword(@RequestParam String newPassword, 
			@RequestParam String oldPassword) {
		passwordService.changePassword(oldPassword, newPassword);
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
	@PostMapping(value = "/updateAccountStatus")
	public ResponseEntity<StandardResponse> updateAccountStatus(@RequestParam String email, 
			@RequestParam AccountStatus newAccountStatus) {
		userAccountService.updateAccountStatus(email, newAccountStatus);
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
		emailService.processMessageDetails(messageAsMap);
		StandardResponse response = new StandardResponse(LocalDateTime.now(),
				"Notification sent successfully", null, HttpStatus.OK);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
}