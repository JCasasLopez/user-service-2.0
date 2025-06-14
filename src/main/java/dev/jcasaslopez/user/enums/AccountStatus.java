package dev.jcasaslopez.user.enums;

public enum AccountStatus {
	
	// Account is fully operational.
	ACTIVE("Active"),

	// Account is temporarily blocked due to too many failed login attempts. The
	// account will unblock automatically after a period of time specified in application.properties.
	TEMPORARILY_BLOCKED("Temporarily Blocked"),

	// Account is blocked due to administrative or security reasons.
	// It may be reactivated later by an admin, but it does not unblock automatically.
	BLOCKED("Blocked"),

	// Account is permanently suspended and cannot be reactivated.
	// This typically results in data removal or deactivation.
	PERMANENTLY_SUSPENDED("Permanently Suspended");

    private final String displayName;

    AccountStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

