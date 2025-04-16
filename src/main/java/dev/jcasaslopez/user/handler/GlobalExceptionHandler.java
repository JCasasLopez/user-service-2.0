package dev.jcasaslopez.user.handler;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import dev.jcasaslopez.user.dto.StandardResponse;
import dev.jcasaslopez.user.exception.AccountStatusException;
import dev.jcasaslopez.user.exception.MissingCredentialException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.validation.ConstraintViolationException;

@ControllerAdvice
public class GlobalExceptionHandler {
	
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
	
	@ExceptionHandler(AccountStatusException.class)
	public ResponseEntity<StandardResponse> handleAccountStatusException(AccountStatusException ex){
        log.error("AccountStatusException: {}", ex.getMessage(), ex);
        StandardResponse response = new StandardResponse (LocalDateTime.now(), 
				ex.getMessage() , null, HttpStatus.CONFLICT);
		return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
	}
	
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<StandardResponse> handleIllegalArgumentException(IllegalArgumentException ex){
        log.error("IllegalArgumentException: {}", ex.getMessage(), ex);
		StandardResponse response = new StandardResponse (LocalDateTime.now(), 
				ex.getMessage() , null, HttpStatus.BAD_REQUEST);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}
	
	@ExceptionHandler(UnsupportedOperationException.class)
	public ResponseEntity<StandardResponse> handleUnsupportedOperationException(UnsupportedOperationException ex){
		log.error("UnsupportedOperationException: {}", ex.getMessage(), ex);
		StandardResponse response = new StandardResponse (LocalDateTime.now(), 
				ex.getMessage() , null, HttpStatus.NOT_IMPLEMENTED);
		return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(response);
	}
	
	@ExceptionHandler(UsernameNotFoundException.class)
	public ResponseEntity<StandardResponse> handleUsernameNotFoundException(UsernameNotFoundException ex){
		log.error("UsernameNotFoundException: {}", ex.getMessage(), ex);
		StandardResponse response = new StandardResponse (LocalDateTime.now(), 
				ex.getMessage() , null, HttpStatus.NOT_FOUND);
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
	}
	
	@ExceptionHandler(MissingCredentialException.class)
	public ResponseEntity<StandardResponse> handleMissingCredentialException(MissingCredentialException ex){
        log.error("AccountStatusException: {}", ex.getMessage(), ex);
        StandardResponse response = new StandardResponse (LocalDateTime.now(), 
				ex.getMessage() , null, HttpStatus.BAD_REQUEST);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}
	
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<StandardResponse> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
		log.error("DataIntegrityViolationException: {}", ex.getMessage(), ex);
		if(ex.getMessage().contains("user.username")) {
			StandardResponse response = new StandardResponse (LocalDateTime.now(), 
					"A user with that username already exists", null, HttpStatus.CONFLICT);
			return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
		}
		if(ex.getMessage().contains("user.email")) {
			StandardResponse response = new StandardResponse (LocalDateTime.now(), 
					"A user with that email already exists", null, HttpStatus.CONFLICT);
			return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
		}else {
			StandardResponse response = new StandardResponse (LocalDateTime.now(), 
					 "A data integrity violation occurred", null, HttpStatus.CONFLICT);
			return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
		}
	}
	
	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<StandardResponse> handleConstraintViolationException(ConstraintViolationException ex) {
		log.error("ConstraintViolationException: {}", ex.getMessage(), ex);
		List<String> errors = ex.getConstraintViolations()
	                            .stream()
	                            .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
	                            .collect(Collectors.toList());

	    StandardResponse response = new StandardResponse(
	        LocalDateTime.now(),
	        "Validation failed for one or more fields",
	        errors.toString(),
	        HttpStatus.BAD_REQUEST
	    );

	    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}
	
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<StandardResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
	    List<String> errors = ex.getBindingResult()
	                            .getFieldErrors()
	                            .stream()
	                            .map(error -> error.getField() + ": " + error.getDefaultMessage())
	                            .collect(Collectors.toList());

	    StandardResponse response = new StandardResponse(
	        LocalDateTime.now(),
	        "Validation failed for one or more fields",
	        errors.toString(), 
	        HttpStatus.BAD_REQUEST
	    );

	    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}


	@ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<StandardResponse> handleExpiredJwtException(ExpiredJwtException ex) {
		log.error("ExpiredJwtException: {}", ex.getMessage(), ex);
		StandardResponse response = new StandardResponse (LocalDateTime.now(), ex.getMessage(), null,
				HttpStatus.UNAUTHORIZED);
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(MalformedJwtException.class)
    public ResponseEntity<StandardResponse> handleMalformedJwtException(MalformedJwtException ex) {
		log.error("MalformedJwtException: {}", ex.getMessage(), ex);
		StandardResponse response = new StandardResponse (LocalDateTime.now(), ex.getMessage(), null,
				HttpStatus.UNAUTHORIZED);
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<StandardResponse> handleJwtException(JwtException ex) {
		log.error("JwtException: {}", ex.getMessage(), ex);
    	StandardResponse response = new StandardResponse (LocalDateTime.now(), ex.getMessage(), null,
				HttpStatus.UNAUTHORIZED);
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }
    
    @ExceptionHandler({JsonMappingException.class, JsonProcessingException.class})
    public ResponseEntity<StandardResponse> handleJsonExceptions(JsonProcessingException ex) {
		log.error("JsonProcessingException: {}", ex.getMessage(), ex);
    	StandardResponse response = new StandardResponse (LocalDateTime.now(), 
    			"Error serializing or deserializing JSON data", null, HttpStatus.INTERNAL_SERVER_ERROR);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    @ExceptionHandler(RedisConnectionFailureException .class)
	public ResponseEntity<StandardResponse> handleRedisConnectionFailureException(RedisConnectionFailureException ex){
        log.error("RedisConnectionFailureException: {}", ex.getMessage(), ex);
        StandardResponse response = new StandardResponse (LocalDateTime.now(), 
        		"Error connecting to Redis" , null, HttpStatus.INTERNAL_SERVER_ERROR);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
	}
    
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<StandardResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = "Invalid value for parameter: " + ex.getName();
        return ResponseEntity.badRequest().body(
            new StandardResponse(LocalDateTime.now(), message, null, HttpStatus.BAD_REQUEST));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<StandardResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        String message = "Missing required parameter: " + ex.getParameterName();
        return ResponseEntity.badRequest().body(
            new StandardResponse(LocalDateTime.now(), message, null, HttpStatus.BAD_REQUEST));
    }
}