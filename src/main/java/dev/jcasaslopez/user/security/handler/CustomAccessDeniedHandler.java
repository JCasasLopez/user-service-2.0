package dev.jcasaslopez.user.security.handler;

import java.io.IOException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import dev.jcasaslopez.user.handler.StandardResponseHandler;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {
	
	StandardResponseHandler standardResponseHandler;
	
	public CustomAccessDeniedHandler(StandardResponseHandler standardResponseHandler) {
		this.standardResponseHandler = standardResponseHandler;
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
			AccessDeniedException accessDeniedException) throws IOException, ServletException {
		standardResponseHandler.handleResponse(response, 403, 
				"Access denied: the user does not have the required role to access this resource"
				, null);
	}

}
