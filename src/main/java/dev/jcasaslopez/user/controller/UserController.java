package dev.jcasaslopez.user.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
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
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Validated
@CrossOrigin("*")
@RestController
public class UserController {

	private AccountOrchestrationService accountOrchestrationService;

	public UserController(AccountOrchestrationService accountOrchestrationService) {
		this.accountOrchestrationService = accountOrchestrationService;
	}

	// Manda un email con el token de verificación para la creación de una cuenta.
	//
	// It sends a verification email to initiate the registration process.
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
	
	// Sin parámetros: borra al usuario que está autenticado en ese momento (puebla SecurityContext).
	//
	// No paramters: deletes the user that populates SecurityContextHolder.
	@DeleteMapping(value = "/deleteAccount")
	public ResponseEntity<StandardResponse> deleteAccount() {
		accountOrchestrationService.deleteAccount();
		StandardResponse response = new StandardResponse(LocalDateTime.now(),
				"Account deleted successfully", null, HttpStatus.OK);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
	
	// Manda un email con el token de verificación para resetear la contraseña.
	//
	// It sends a verification email to initiate the password reset process.
	@PostMapping(value = Constants.FORGOT_PASSWORD_PATH)
	public ResponseEntity<StandardResponse> forgotPassword(@RequestParam @NotBlank @Email 
			String email) {
		accountOrchestrationService.forgotPassword(email);
		StandardResponse response = new StandardResponse(LocalDateTime.now(),
				"Token created successfully and sent to the user to reset password", null, HttpStatus.OK);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
	
	// Ver comentario en PasswordServiceImpl.resetPassword() para entender mejor la diferencia 
	// entre las funcionalidades resetPassword y changePassword, y por qué son 2 lógicas diferenciadas.
	//
	// See the comment in PasswordServiceImpl.resetPassword() to better understand the difference 
	// between the resetPassword and changePassword functionalities, and why they are two separate flows.
	@PutMapping(value = Constants.RESET_PASSWORD_PATH)
	public ResponseEntity<StandardResponse> resetPassword(@RequestParam @NotBlank String newPassword, 
			HttpServletRequest request) {
		accountOrchestrationService.resetPassword(newPassword, request);
		StandardResponse response = new StandardResponse(LocalDateTime.now(),
				"Password reset successfully", null, HttpStatus.OK);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
		
	@PutMapping(value = "/changePassword")
	public ResponseEntity<StandardResponse> changePassword(@RequestParam @NotBlank String oldPassword, 
			@RequestParam @NotBlank String newPassword) {
		accountOrchestrationService.changePassword(oldPassword, newPassword);
		StandardResponse response = new StandardResponse(LocalDateTime.now(),
				"Password changed successfully", null, HttpStatus.OK);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
	
	// Promociona usuario de simple USER a ADMIN. Solo accesible para usuarios SUPER_ADMIN.
	//
	// Upgrades user from USER to ADMIN. Only accesible to SUPER_ADMIN users.
	@PutMapping(value = "/upgradeUser")
	public ResponseEntity<StandardResponse> upgradeUser(@RequestParam @NotBlank @Email String email) {
		accountOrchestrationService.upgradeUser(email);
		StandardResponse response = new StandardResponse(LocalDateTime.now(),
				"User upgraded successfully to admin", null, HttpStatus.OK);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
	
	// Permite cambiar el estado de una cuenta, ya sea para suspenderla temporalmente o 
	// desactivarla de forma definitiva. Este método se utiliza en situaciones como problemas 
	// administrativos, detección de actividad sospechosa, etc.	
	// Solo accesible para usuarios ADMIN.
	// Desbloqueo de cuenta automático tras el período de tiempo estipulado en application.properties.
	// Spring permite pasar una enumeración como parámetro en un @RequestParam,
	// pero solo si el valor en la URL coincide exactamente con el nombre del enumeración.
	//
	// Allows changing the status of an account, either by temporarily suspending it or 
	// permanently deactivating it. This method is typically used in cases such as administrative 
	// issues, suspicious activity, etc.
	// Only accessible to ADMIN users.
	// Automatic account unlock after the time period specified in application.properties.
	// Spring allows passing an enumeration as a parameter in a @RequestParam,
	// but only if the value in the URL exactly matches the enumeration name.
	//
	// Accepted values: ACTIVE, TEMPORARILY_BLOCKED, PERMANENTLY_SUSPENDED
	@PutMapping(value = "/updateAccountStatus")
	public ResponseEntity<StandardResponse> updateAccountStatus(@RequestParam @NotBlank String email, 
			@RequestParam @NotNull AccountStatus newAccountStatus) {
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
	public ResponseEntity<StandardResponse> sendNotification(@RequestParam @NotNull Map<String, String> messageAsMap) {
		accountOrchestrationService.sendNotification(messageAsMap);
		StandardResponse response = new StandardResponse(LocalDateTime.now(),
				"Notification sent successfully", null, HttpStatus.OK);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
	
	// Endpoint para la funcionalidad de refrescado de tokens.
	// El sistema deberá recibir un token de refresco que se intercepta en AuthenticationFilter:
	// si el token es válido, se revoca en el filtro. 
	// accountOrchestrationService.refreshToken() simplemente devuelve otra pareja de tokens refresco/acceso.
	// IMPORTANTE: En la lista, el primer token corresponde al de refresco y el segundo al de acceso.
	//
	// Endpoint for token refresh functionality.
	// The system should receive a refresh token, which is intercepted in AuthenticationFilter:
	// if the token is valid, it is revoked in the filter. 
	// accountOrchestrationService.refreshToken() simply returns another pair of refresh/access tokens.
	// IMPORTANT: In the list, the first token is the refresh token and the second one is the access token.
	@PostMapping(value  = Constants.REFRESH_TOKEN_PATH)
	public ResponseEntity<StandardResponse> refreshToken() {
		List<String> tokens = accountOrchestrationService.refreshToken();
		StandardResponse response = new StandardResponse(LocalDateTime.now(),
				"New refresh and access tokens sent successfully", tokens, HttpStatus.CREATED);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
}