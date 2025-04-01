package dev.jcasaslopez.user.enums;

public enum AccountStatus {
	
    // Cuenta completamente operativa
    // Account is fully operational
    ACTIVE("Active"),

    // Cuenta bloqueada temporalmente por razones administrativas o de seguridad.
    // Puede ser reactivada posteriormente.
    //
    // Account is temporarily blocked due to administrative or security reasons.
    // It may be reactivated later.
    TEMPORARILY_BLOCKED("Temporarily Blocked"),

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

