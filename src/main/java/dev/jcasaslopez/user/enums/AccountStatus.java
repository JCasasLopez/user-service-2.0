package dev.jcasaslopez.user.enums;

public enum AccountStatus {
	
	// Cuenta completamente operativa.
	//
	// Account is fully operational.
	ACTIVE("Active"),

	// Cuenta bloqueada temporalmente al haberse superado el número máximo de
	// intentos permitidos de login. Se desbloquea automáticamente después de período de
	// tiempo establecido en application.properties.
	//
	// Account is temporarily blocked due to too many failed login attempts. The
	// account will unblock automatically after a period of time specified in application.properties.
	TEMPORARILY_BLOCKED("Temporarily Blocked"),

	// Cuenta bloqueada por razones administrativas o de seguridad.
	// Puede ser reactivada posteriormente por un admin, pero no se desbloquea automáticamente.
	//
	// Account is blocked due to administrative or security reasons.
	// It may be reactivated later by an admin, but it does not unblock automatically.
	BLOCKED("Blocked"),

	// Cuenta suspendida permanentemente. No puede ser reactivada.
	// Esto usualmente implica la eliminación o desactivación de los datos asociados.
	//
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

