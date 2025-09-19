package dev.jcasaslopez.user.controller;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
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
import dev.jcasaslopez.user.utilities.MessageNotificationValidation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Validated
@RestController
public class UserController {

	private AccountOrchestrationService accountOrchestrationService;

	public UserController(AccountOrchestrationService accountOrchestrationService) {
		this.accountOrchestrationService = accountOrchestrationService;
	}

	@Operation(
			summary = "Initiates the registration process by sending a verification email",
		    description = "Receives a user object and sends a verification token to the provided email address"
	)
	@ApiResponses({
	    @ApiResponse(
	        responseCode = "200",
	        description = "Verification token created and sent successfully",
	        content = @Content(schema = @Schema(implementation = StandardResponse.class))
	    ),
	    @ApiResponse(
	        responseCode = "400",
	        description = "Validation failed for one or more fields",
	        content = @Content(schema = @Schema(implementation = StandardResponse.class))
	    )
	})
	@PostMapping(value = Constants.INITIATE_REGISTRATION_PATH, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<StandardResponse> initiateRegistration(@Valid @RequestBody UserDto user) 
			throws JsonProcessingException{
		accountOrchestrationService.initiateRegistration(user);
		StandardResponse response = new StandardResponse(LocalDateTime.now(),
				"Token created successfully and sent to the user to verify email", null, HttpStatus.OK);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
	
	@Operation(
		    summary = "Finalizes user registration",
		    description = "Consumes a verification token (from header) and request body to create a new user account"
		)
		@ApiResponses({
		    @ApiResponse(
		        responseCode = "201",
		        description = "Account created successfully",
		        content = @Content(schema = @Schema(implementation = StandardResponse.class))
		    ),
		    @ApiResponse(
		        responseCode = "400",
		        description = "Validation failed for one or more fields or invalid input",
		        content = @Content(schema = @Schema(implementation = StandardResponse.class))
		    ),
		    @ApiResponse(
			    responseCode = "401",
			    description = "Unauthorized – token is missing, expired, or invalid",
			    content = @Content(schema = @Schema(implementation = StandardResponse.class))
			),
		    @ApiResponse(
		        responseCode = "409",
		        description = "User with that email or username already exists",
		        content = @Content(schema = @Schema(implementation = StandardResponse.class))
		    ),
		    @ApiResponse(
		        responseCode = "500",
		        description = "Error processing the JSON payload",
		        content = @Content(schema = @Schema(implementation = StandardResponse.class))
		    )
		})
	@SecurityRequirement(name = "bearerAuth")
	@PostMapping(value = Constants.REGISTRATION_PATH)
	public ResponseEntity<StandardResponse> createAccount(HttpServletRequest request) 
			throws JsonMappingException, JsonProcessingException{
		accountOrchestrationService.userRegistration(request);
		StandardResponse response = new StandardResponse(LocalDateTime.now(),
				"Account created successfully", null, HttpStatus.CREATED);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
	
	@Operation(
		    summary = "Deletes the authenticated user account",
		    description = "Removes the account associated with the authenticated user (requires a valid token)"
		)
		@ApiResponses({
		    @ApiResponse(
		        responseCode = "200",
		        description = "Account deleted successfully",
		        content = @Content(schema = @Schema(implementation = StandardResponse.class))
		    ),
		    @ApiResponse(
		        responseCode = "401",
		        description = "Unauthorized – token is missing, expired, or invalid",
		        content = @Content(schema = @Schema(implementation = StandardResponse.class))
		    )
		})
	@SecurityRequirement(name = "bearerAuth")
	@DeleteMapping(value = Constants.DELETE_ACCOUNT)
	public ResponseEntity<StandardResponse> deleteAccount() {
		accountOrchestrationService.deleteAccount();
		StandardResponse response = new StandardResponse(LocalDateTime.now(),
				"Account deleted successfully", null, HttpStatus.OK);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
	
	@Operation(
		    summary = "Initiates password reset process",
		    description = "Sends a verification token to the user email to allow password reset"
		)
		@ApiResponses({
		    @ApiResponse(
		        responseCode = "200",
		        description = "Token created successfully and sent to the user to reset password",
		        content = @Content(schema = @Schema(implementation = StandardResponse.class))
		    ),
		    @ApiResponse(
		        responseCode = "400",
		        description = "Invalid email format or empty field",
		        content = @Content(schema = @Schema(implementation = StandardResponse.class))
		    ),
		    @ApiResponse(
		        responseCode = "404",
		        description = "User not found in the database",
		        content = @Content(schema = @Schema(implementation = StandardResponse.class))
		    )
		})
	@io.swagger.v3.oas.annotations.parameters.RequestBody(
		    description = "User's email address",
		    required = true,
		    content = @Content(schema = @Schema(type = "string", example = "user@example.com"))
		)
	@PostMapping(value = Constants.FORGOT_PASSWORD_PATH)
	public ResponseEntity<StandardResponse> forgotPassword(@RequestBody @NotBlank @Email String email) {
		accountOrchestrationService.forgotPassword(email);
		StandardResponse response = new StandardResponse(LocalDateTime.now(),
				"Token created successfully and sent to the user to reset password", null, HttpStatus.OK);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
	
	
	// See the comment in PasswordServiceImpl.resetPassword() to better understand the difference 
	// between the resetPassword and changePassword functionalities, and why they are two separate flows.
	@Operation(
		    summary = "Resets the user password",
		    description = "Consumes a new password and the token from the request to reset the user password"
		)
		@ApiResponses({
		    @ApiResponse(
		        responseCode = "200",
		        description = "Password reset successfully",
		        content = @Content(schema = @Schema(implementation = StandardResponse.class))
		    ),
		    @ApiResponse(
		        responseCode = "400",
		        description = "Missing or invalid password format",
		        content = @Content(schema = @Schema(implementation = StandardResponse.class))
		    ),
		    @ApiResponse(
			    responseCode = "401",
			    description = "Unauthorized – token is missing, expired, or invalid",
			    content = @Content(schema = @Schema(implementation = StandardResponse.class))
			)
		})
	@io.swagger.v3.oas.annotations.parameters.RequestBody(
		    description = "New password to set for the user",
		    required = true,
		    content = @Content(
		        schema = @Schema(
		            type = "string",
		            example = "Password123!"
		        )
		    )
		)
	@SecurityRequirement(name = "bearerAuth")
	@PutMapping(value = Constants.RESET_PASSWORD_PATH)
	public ResponseEntity<StandardResponse> resetPassword(@RequestBody @NotBlank String newPassword, 
			HttpServletRequest request) {
		accountOrchestrationService.resetPassword(newPassword, request);
		StandardResponse response = new StandardResponse(LocalDateTime.now(),
				"Password reset successfully", null, HttpStatus.OK);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
		
	@Operation(
		    summary = "Changes the password of the currently authenticated user",
		    description = "Requires both the old and new passwords in the request body. The new password must be valid and different from the current one."
		)
		@ApiResponses({
		    @ApiResponse(
		        responseCode = "200",
		        description = "Password changed successfully",
		        content = @Content(schema = @Schema(implementation = StandardResponse.class))
		    ),
		    @ApiResponse(
		        responseCode = "400",
		        description = "Missing password fields or password does not meet the security criteria",
		        content = @Content(schema = @Schema(implementation = StandardResponse.class))
		    ),
		    @ApiResponse(
			    responseCode = "401",
			    description = "Unauthorized – token is missing, expired, or invalid",
			    content = @Content(schema = @Schema(implementation = StandardResponse.class))
			)
		})
	@io.swagger.v3.oas.annotations.parameters.RequestBody(
		    description = "Map containing old and new passwords. Both fields are required.",
		    required = true,
		    content = @Content(
		        schema = @Schema(
		            type = "object",
		            example = """
		                      {
		                        "oldPassword": "Password123!",
		                        "newPassword": "NewPassword456!"
		                      }
		                      """
		        )
		    )
		)
	@SecurityRequirement(name = "bearerAuth")
	@PutMapping(value = Constants.CHANGE_PASSWORD)
	public ResponseEntity<StandardResponse> changePassword(@RequestBody @NotNull Map<String, String> passwordsAsMap) {
		String oldPassword = passwordsAsMap.get("oldPassword");
	    String newPassword = passwordsAsMap.get("newPassword");

	    if (oldPassword == null || oldPassword.isBlank() || 
	    	    newPassword == null || newPassword.isBlank()) {
	    	    throw new IllegalArgumentException("Both oldPassword and newPassword are required");
	    	}
	    
		accountOrchestrationService.changePassword(oldPassword, newPassword);
		StandardResponse response = new StandardResponse(LocalDateTime.now(),
				"Password changed successfully", null, HttpStatus.OK);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
	
	@Operation(
		    summary = "Grants admin privileges to a user",
		    description = "Takes an email address and upgrades the corresponding user to admin. Only accessible to users with SUPERADMIN role."
		)
		@ApiResponses({
		    @ApiResponse(
		        responseCode = "200",
		        description = "User upgraded successfully to admin",
		        content = @Content(schema = @Schema(implementation = StandardResponse.class))
		    ),
		    @ApiResponse(
		        responseCode = "400",
		        description = "Invalid or missing email",
		        content = @Content(schema = @Schema(implementation = StandardResponse.class))
		    ),
		    @ApiResponse(
		        responseCode = "401",
		        description = "Unauthorized – token is missing, expired, or invalid",
		        content = @Content(schema = @Schema(implementation = StandardResponse.class))
		    ),
		    @ApiResponse(
		        responseCode = "403",
		        description = "User does not have SUPERADMIN privileges",
		        content = @Content(schema = @Schema(implementation = StandardResponse.class))
		    ),
		    @ApiResponse(
		        responseCode = "404",
		        description = "User not found in the database",
		        content = @Content(schema = @Schema(implementation = StandardResponse.class))
		    )
		})
	@io.swagger.v3.oas.annotations.parameters.RequestBody(
		    description = "Email address of the user whose account is going to be updated",
		    required = true,
		    content = @Content(
		        schema = @Schema(
		            type = "string",
		            format = "email",
		            example = "user@example.com"
		        )
		    )
		)
	@SecurityRequirement(name = "bearerAuth")
	@PutMapping(value = Constants.UPGRADE_USER)
	public ResponseEntity<StandardResponse> upgradeUser(@RequestBody @NotBlank @Email String email) {
		accountOrchestrationService.upgradeUser(email);
		StandardResponse response = new StandardResponse(LocalDateTime.now(),
				"User upgraded successfully to admin", null, HttpStatus.OK);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
	
	// Allows changing the status of an account, either by temporarily suspending it or 
	// permanently deactivating it. This method is typically used in cases such as administrative 
	// issues, suspicious activity, etc.
	// Only accessible to ADMIN users.
	// Automatic account unlock after the time period specified in application.properties.
	// Spring allows passing an enumeration as a parameter in a @RequestParam,
	// but only if the value in the URL exactly matches the enumeration name.
	// Accepted values: ACTIVE, BLOCKED, TEMPORARILY_BLOCKED, PERMANENTLY_SUSPENDED
	@Operation(
		    summary = "Updates the account status of a user",
		    description = "Takes an email and a new account status to update the user. Only accessible to ADMIN and SUPERADMIN roles."
		)
		@ApiResponses({
		    @ApiResponse(
		        responseCode = "200",
		        description = "Account status updated successfully",
		        content = @Content(schema = @Schema(implementation = StandardResponse.class))
		    ),
		    @ApiResponse(
		        responseCode = "400",
		        description = "Invalid or missing email or account status value",
		        content = @Content(schema = @Schema(implementation = StandardResponse.class))
		    ),
		    @ApiResponse(
		        responseCode = "401",
		        description = "Unauthorized – token is missing, expired, or invalid",
		        content = @Content(schema = @Schema(implementation = StandardResponse.class))
		    ),
		    @ApiResponse(
		        responseCode = "403",
		        description = "User does not have ADMIN or SUPERADMIN privileges",
		        content = @Content(schema = @Schema(implementation = StandardResponse.class))
		    ),
		    @ApiResponse(
		        responseCode = "404",
		        description = "User not found in the database",
		        content = @Content(schema = @Schema(implementation = StandardResponse.class))
		    )
		})
	@io.swagger.v3.oas.annotations.parameters.RequestBody(
		    description = "Email address of the user to be upgraded to admin",
		    required = true,
		    content = @Content(
		        schema = @Schema(
		            type = "string",
		            format = "email",
		            example = "user@example.com"
		        )
		    )
		)
	@Parameter(
		    name = "newAccountStatus",
		    description = """
		        New status to assign to the user account. Accepted values:
		        
		        - `ACTIVE`: The account is in good standing and fully usable.
		        - `BLOCKED`: The account is suspended for a period (e.g., due to suspicious activity).
		        - `TEMPORARILY_BLOCKED`: The account is blocked due to too many failed login attempts.
		    						It will reactivate automatically after a defined period of time (24h by default).
		        - `PERMANENTLY_SUSPENDED`: The account is permanently deactivated and cannot be used.
		        
		        Must match the enum name exactly.
		        """,
		    required = true
		)
	@SecurityRequirement(name = "bearerAuth")
	@PutMapping(value = Constants.UPDATE_ACCOUNT_STATUS)
	public ResponseEntity<StandardResponse> updateAccountStatus(@RequestBody @NotBlank String email, 
			@RequestParam @NotNull AccountStatus newAccountStatus) {
		accountOrchestrationService.updateAccountStatus(email, newAccountStatus);
		StandardResponse response = new StandardResponse(LocalDateTime.now(),
				"Account status successfully updated to " + newAccountStatus.getDisplayName(), null, HttpStatus.OK);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
	
	@Operation(
		    summary = "Sends a notification to a user",
		    description = "Receives a message as a map and sends a notification email to the user with the provided information. Requires authentication."
		)
		@ApiResponses({
		    @ApiResponse(
		        responseCode = "200",
		        description = "Notification sent successfully",
		        content = @Content(schema = @Schema(implementation = StandardResponse.class))
		    ),
		    @ApiResponse(
		        responseCode = "400",
		        description = "Missing or invalid message fields",
		        content = @Content(schema = @Schema(implementation = StandardResponse.class))
		    ),
		    @ApiResponse(
		        responseCode = "401",
		        description = "Unauthorized – token is missing, expired, or invalid",
		        content = @Content(schema = @Schema(implementation = StandardResponse.class))
		    ),
		    @ApiResponse(
		        responseCode = "404",
		        description = "User not found in the database",
		        content = @Content(schema = @Schema(implementation = StandardResponse.class))
		    )
		})
	@io.swagger.v3.oas.annotations.parameters.RequestBody(
		    description = """
		        Notification message map. All keys must begin with an uppercase letter and be spelled exactly as shown.
		        The value of 'Recipient' must be the user ID as a String (e.g., \"123\").
		        
		        Required keys:
		        - Recipient : String (user ID)
		        - Subject   : String
		        - Message   : String
		    """,
		    required = true,
		    content = @Content(schema = @Schema(
		        type = "object",
		        example = """
		        {
		          "Recipient": "123",
		          "Subject": "Account warning",
		          "Message": "Your account has been temporarily suspended due to policy violations."
		        }
		        """
		    ))
		)
	@SecurityRequirement(name = "bearerAuth")
	@PostMapping(value = Constants.SEND_NOTIFICATION)
	public ResponseEntity<StandardResponse> sendNotification(@RequestBody @NotNull Map<String, String> messageAsMap) {
		
		// We validate the message has a a valid format.
		MessageNotificationValidation.validateMessage(messageAsMap);
		accountOrchestrationService.sendNotification(messageAsMap);
		StandardResponse response = new StandardResponse(LocalDateTime.now(),
				"Notification sent successfully", null, HttpStatus.OK);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
	
}