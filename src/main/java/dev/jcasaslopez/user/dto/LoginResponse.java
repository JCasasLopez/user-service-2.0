package dev.jcasaslopez.user.dto;

public class LoginResponse {
	
	private UserDto user;
    private String refreshToken;
    private String accessToken;

    public LoginResponse(UserDto user, String refreshToken, String accessToken) {
        this.user = user;
        this.refreshToken = refreshToken;
        this.accessToken = accessToken;
    }
    
	public LoginResponse() {
	}

	public UserDto getUser() {
		return user;
	}

	public void setUser(UserDto user) {
		this.user = user;
	}

	public String getRefreshToken() {
		return refreshToken;
	}

	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

}