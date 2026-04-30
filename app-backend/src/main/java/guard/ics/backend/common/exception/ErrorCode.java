package guard.ics.backend.common.exception;

public enum ErrorCode {
    // Auth
    AUTH_INVALID_CREDENTIALS(401, "Invalid username or password"),
    AUTH_ACCOUNT_DISABLED(403, "Account is disabled"),
    AUTH_UNAUTHORIZED(401, "Authentication required"),

    // General
    RESOURCE_NOT_FOUND(404, "Resource not found"),
    BAD_REQUEST(400, "Bad request"),
    VALIDATION_ERROR(400, "Validation failed"),
    FORBIDDEN(403, "Access denied"),
    CONFLICT(409, "Resource already exists"),
    INTERNAL_ERROR(500, "Internal server error"),

    // Rules
    RULE_INVALID_CONFIG(400, "Invalid rule configuration for the specified rule type"),
    RULE_INVALID_TYPE(400, "Unknown rule type"),

    // Alerts
    ALERT_INVALID_TRANSITION(400, "Invalid alert status transition"),
    ALERT_NOT_FOUND(404, "Alert not found"),

    // SDN
    SDN_CONTROLLER_UNAVAILABLE(503, "SDN controller is not available"),
    SDN_FLOW_OPERATION_FAILED(500, "Flow operation failed on SDN controller"),

    // Kafka
    KAFKA_PRODUCE_FAILED(500, "Failed to produce Kafka message");

    private final int httpStatus;
    private final String defaultMessage;

    ErrorCode(int httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public int httpStatus() { return httpStatus; }
    public String defaultMessage() { return defaultMessage; }
}
