package dev.jcasaslopez.user.dto;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;

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

	private LocalDateTime timestamp;
	private String message;
	private Object details;
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
