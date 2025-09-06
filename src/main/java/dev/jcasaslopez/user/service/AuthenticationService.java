package dev.jcasaslopez.user.service;

import java.io.IOException;
import java.util.Optional;

import dev.jcasaslopez.user.dto.AuthenticationRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthenticationService {
	
	boolean verifyHeaderIsValid(HttpServletRequest request);
	Optional<AuthenticationRequest> parseAuthenticationRequest(HttpServletRequest request, HttpServletResponse response)
			throws IOException;
	void authenticateUser(String token, String username);

}