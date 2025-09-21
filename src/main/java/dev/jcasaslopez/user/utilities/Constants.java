package dev.jcasaslopez.user.utilities;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Constants {
	public static final String LOGIN_ATTEMPTS_REDIS_KEY = "login_attempts:";
	public static final String REFRESH_TOKEN_REDIS_KEY = "refresh_token:";
	public static final String CREATE_ACCOUNT_REDIS_KEY = "create_account:";
	
	public static final String LOGIN_PATH = "/login";
	public static final String LOGOUT_PATH = "/logout";
	public static final String REFRESH_TOKEN_PATH = "/refreshToken";
	public static final String INITIATE_REGISTRATION_PATH = "/initiateUserRegistration";
	public static final String REGISTRATION_PATH = "/userRegistration";
	public static final String FORGOT_PASSWORD_PATH = "/forgotPassword";
	public static final String RESET_PASSWORD_PATH = "/resetPassword";
	public static final String DELETE_ACCOUNT_PATH = "/deleteAccount";
	public static final String UPGRADE_USER_PATH = "/upgradeUser";
	public static final String CHANGE_PASSWORD_PATH = "/changePassword";
	public static final String UPDATE_ACCOUNT_STATUS_PATH = "/updateAccountStatus";
	public static final String SEND_NOTIFICATION_PATH = "/sendNotification";
	
	// Public endpoints: no authentication or token required
    public static final Set<String> PUBLIC_ENDPOINTS = Set.of(
        LOGIN_PATH,
        INITIATE_REGISTRATION_PATH,
        FORGOT_PASSWORD_PATH
    );
    
    // Action token endpoints - REFRESH: require a valid refresh token, but not an authenticated user (SecurityContext not populated)
    public static final Set<String> ACTION_TOKEN_REFRESH_ENDPOINTS = Set.of(
        LOGOUT_PATH,
        REFRESH_TOKEN_PATH
    );
    
    // Action token endpoints - VERIFICATION: require a valid verification token, but not an authenticated user (SecurityContext not populated)
    public static final Set<String> ACTION_TOKEN_VERIFICATION_ENDPOINTS = Set.of(
        REGISTRATION_PATH,
        RESET_PASSWORD_PATH
    );
    
    // Protected endpoints: require both a valid access token and authentication (SecurityContext must be populated with user details)
    public static final Set<String> PROTECTED_ENDPOINTS = Set.of(
        DELETE_ACCOUNT_PATH,
        UPGRADE_USER_PATH, 
        CHANGE_PASSWORD_PATH,
        UPDATE_ACCOUNT_STATUS_PATH,
        SEND_NOTIFICATION_PATH
    );
    
    
    // Converts protected endpoints Set to String array for Spring Security configuration.
    // Spring Security requestMatchers() requires String[] varargs.
    public static String[] getProtectedEndpoints() {
        return PROTECTED_ENDPOINTS.toArray(new String[0]);
    }

    public static String[] getNonAuthenticatedEndpoints() {
        List<String> allEndpoints = new ArrayList<>();
        allEndpoints.addAll(PUBLIC_ENDPOINTS);
        allEndpoints.addAll(ACTION_TOKEN_REFRESH_ENDPOINTS);
        allEndpoints.addAll(ACTION_TOKEN_VERIFICATION_ENDPOINTS);
        return allEndpoints.toArray(new String[0]);
    }
}
