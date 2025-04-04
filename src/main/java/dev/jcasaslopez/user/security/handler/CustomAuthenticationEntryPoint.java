package dev.jcasaslopez.user.security.handler;

import java.io.IOException;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import dev.jcasaslopez.user.handler.StandardResponseHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    StandardResponseHandler standardResponseHandler;

    public CustomAuthenticationEntryPoint(StandardResponseHandler standardResponseHandler) {
        this.standardResponseHandler = standardResponseHandler;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
    		AuthenticationException authException) throws IOException {
    	standardResponseHandler.handleResponse(response, 401, 
    			"Access denied: invalid or missing token", null);
    }
}