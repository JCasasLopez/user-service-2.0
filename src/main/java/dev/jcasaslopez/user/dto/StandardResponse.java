package dev.jcasaslopez.user.dto;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = """
	Standard response object used for both successful operations and error messages. 
	It includes a timestamp, a human-readable message, optional details, and the HTTP status.
	""")
public class StandardResponse {
	
//	   StandardResponse representa una manera unificada de representar respuestas HTTP
//	   en este microservicio, ya sean respuestas satisfactorias como errores.
//	   Esta clase incluye como atributos un timestamp, un mensaje, detalles y el estatus HTTP.

//	   StandardResponse is a common structure for HTTP responses (both successful responses 
//	   and errors) in the application, including a timestamp, a message, additional details, 
//	   and the HTTP status, ensuring a unified response format throughout the API.

//	   Example usage:
//	   return new ResponseEntity<>(new StandardResponse(
//	       LocalDateTime.now(),
//	       "Resource created successfully",
//	       "User ID: 123",
//	       HttpStatus.CREATED), 
//         HttpStatus.CREATED);

	 @Schema(
		        description = "The date and time the response was generated",
		        example = "2025-06-13T14:25:47.091"
		    )
	private LocalDateTime timestamp;
	 
	@Schema(
		        description = "A brief summary of the response",
		        example = "User created successfully"
		    )
	private String message;
	
	@Schema(
	        description = "Additional data or error details, if applicable",
	        example = "User ID: 123"
	    )
	private Object details;
	
	@Schema(
		        description = "HTTP status associated with the response",
		        example = "CREATED"
		    )
	private HttpStatus status;
	
	public StandardResponse(LocalDateTime timestamp, String message, Object details, HttpStatus status) {
		this.timestamp = timestamp;
		this.message = message;
		this.details = details;
		this.status = status;
	}

	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(LocalDateTime timestamp) {
		this.timestamp = timestamp;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Object getDetails() {
		return details;
	}

	public void setDetails(Object details) {
		this.details = details;
	}

	public HttpStatus getStatus() {
		return status;
	}

	public void setStatus(HttpStatus status) {
		this.status = status;
	}
	
}
