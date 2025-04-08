package dev.jcasaslopez.user.utilities;

public class Constants {
	public static final String LOGIN_ATTEMPTS_REDIS_KEY = "login_attempts:";
	public static final String REFRESH_TOKEN_REDIS_KEY = "refresh_token:";
	public static final String CREATE_ACCOUNT_REDIS_KEY = "create_account:";
	
	public static final String LOGOUT_PATH = "/logout";
	public static final String REFRESH_TOKEN_PATH = "/refreshToken";
	public static final String INITIATE_REGISTRATION_PATH = "/initiateUserRegistration";
	public static final String REGISTRATION_PATH = "/userRegistration";
	public static final String FORGOT_PASSWORD_PATH = "/forgotPassword";
	public static final String RESET_PASSWORD_PATH = "/resetPassword";
}
