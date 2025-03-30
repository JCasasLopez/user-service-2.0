package dev.jcasaslopez.user.handler;

import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import dev.jcasaslopez.user.dto.StandardResponse;
import jakarta.servlet.http.HttpServletResponse;

// StandardResponseHandler convierte una StandardResponse en una respuesta HTTP estándar.
// Es necesario en clases como AuthenticationFilter, donde StandardResponse no puede usarse 
// directamente, ya que las excepciones lanzadas en un filtro no son gestionadas por 
// GlobalExceptionHandler.
//
// StandardResponseHandler converts a StandardResponse into a standard HTTP response.
// It is necessary in classes like AuthenticationFilter, where StandardResponse cannot be used 
//directly, as exceptions thrown in a filter are not handled by GlobalExceptionHandler.*/
@Component
public class StandardResponseHandler {
	
	ObjectMapper objectMapper;

  public StandardResponseHandler() {
      this.objectMapper = new ObjectMapper();
      objectMapper.registerModule(new JavaTimeModule());
      objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  }
  
  public HttpServletResponse handleResponse(HttpServletResponse response, int status, 
  																String message, String details) throws IOException {
  	response.setContentType("application/json");
  	response.setCharacterEncoding("UTF-8");
  	response.setStatus(status);
  	StandardResponse respuesta = new StandardResponse (LocalDateTime.now(), message, details, 
  																HttpStatus.resolve(status));
  	String jsonResponse = objectMapper.writeValueAsString(respuesta);
      response.getWriter().write(jsonResponse);
      return response;
  }
  
}

