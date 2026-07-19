package my.hive.domain.auth.model;

public final class AuthReason {
    public static final String EMPLOYEE_NOT_FOUND = "EMPLOYEE_NOT_FOUND";
    public static final String TENANT_SELECTION_REQUIRED = "TENANT_SELECTION_REQUIRED";
    public static final String TENANT_SELECTION_INVALID_OR_EXPIRED = "TENANT_SELECTION_INVALID_OR_EXPIRED";
    public static final String ACCOUNT_DISABLED = "ACCOUNT_DISABLED";
    public static final String EMPLOYEE_RESIGNED = "EMPLOYEE_RESIGNED";
    public static final String TENANT_UNAVAILABLE = "TENANT_UNAVAILABLE";
    public static final String TENANT_LICENSE_UNAVAILABLE = "TENANT_LICENSE_UNAVAILABLE";
    public static final String ACCOUNT_ACTIVATION_REQUIRED = "ACCOUNT_ACTIVATION_REQUIRED";
    public static final String PHONE_ACCOUNT_AMBIGUOUS = "PHONE_ACCOUNT_AMBIGUOUS";
    public static final String INVITATION_INVALID_OR_EXPIRED = "INVITATION_INVALID_OR_EXPIRED";

    private AuthReason() {
    }
}
