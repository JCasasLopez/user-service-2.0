package dev.jcasaslopez.user.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import dev.jcasaslopez.user.dto.StandardResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/auth")
public class AuthDocumentationController {

	@Operation(
		    summary = "[Virtual] Authenticates user and returns tokens",
		    description = """
		        This is a virtual endpoint for documentation purposes only.
		        The authentication process is handled internally via the filter chain and is not exposed as a standard controller.
		        
		        Handles login by receiving username and password. 
		        If authentication is successful, returns access and refresh tokens, and the authenticated user.
		        """
		)

    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login successful", content = @Content(schema = @Schema(implementation = StandardResponse.class))),
        @ApiResponse(responseCode = "400", description = "Username or password missing", content = @Content(schema = @Schema(implementation = StandardResponse.class))),
        @ApiResponse(responseCode = "401", description = "Bad credentials", content = @Content(schema = @Schema(implementation = StandardResponse.class))),
        @ApiResponse(responseCode = "403", description = "Account is locked", content = @Content(schema = @Schema(implementation = StandardResponse.class)))
    })
	@io.swagger.v3.oas.annotations.parameters.RequestBody(
		    description = "User credentials in URL-encoded format (`application/x-www-form-urlencoded`). Must include both fields:",
		    required = true,
		    content = @Content(
		        mediaType = "application/x-www-form-urlencoded",
		        schema = @Schema(
		            type = "object",
		            example = "username=myuser&password=Password123!"
		        )
		    )
		)
    @PostMapping("/login")
    public ResponseEntity<StandardResponse> login(@RequestBody Object loginRequest) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build(); 
    }
	

	@Operation(
		    summary = "[Virtual] Logs out the authenticated user",
		    description = """
		        This is a virtual endpoint for documentation purposes only.
		        The logout process is handled internally via the filter chain and is not exposed as a standard controller.

		        The request invalidates the token or clears the authentication context.
		        """
		)
	
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Logout successful", content = @Content(schema = @Schema(implementation = StandardResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized – invalid or missing refresh token", content = @Content(schema = @Schema(implementation = StandardResponse.class)))
    })
    @RequestBody(
        description = "Refresh token to invalidate",
        required = true,
        content = @Content(schema = @Schema(
            type = "string",
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
        ))
    )
	@SecurityRequirement(name = "bearerAuth")
    @PostMapping("/logout")
    public ResponseEntity<StandardResponse> logout(@RequestBody String refreshToken) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build(); 
    }
}