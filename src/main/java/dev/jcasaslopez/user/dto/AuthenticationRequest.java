package dev.jcasaslopez.user.dto;

import dev.jcasaslopez.user.enums.TokenType;

// DTO that encapsulates authentication request details.  
// It replaces the direct extraction of data from HttpServletRequest in AuthenticationFilter by grouping username, 
// token, purpose, path, and method into a single object. This makes authentication logic cleaner and easier to test.

public class AuthenticationRequest {
	
	private String Username;
	private String Token;
	private TokenType Purpose;
	private String Path;
	private String Method;
	
	public AuthenticationRequest() {
		
	}

	public AuthenticationRequest(String username, String token, TokenType purpose, String path, String method) {
		Username = username;
		Token = token;
		Purpose = purpose;
		Path = path;
		Method = method;
	}

	public String getUsername() {
		return Username;
	}

	public void setUsername(String username) {
		Username = username;
	}

	public String getToken() {
		return Token;
	}

	public void setToken(String token) {
		Token = token;
	}

	public TokenType getPurpose() {
		return Purpose;
	}

	public void setPurpose(TokenType purpose) {
		Purpose = purpose;
	}

	public String getPath() {
		return Path;
	}

	public void setPath(String path) {
		Path = path;
	}

	public String getMethod() {
		return Method;
	}

	public void setMethod(String method) {
		Method = method;
	}
	
}