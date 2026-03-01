package com.btcs.fix;


public class ValidationResult {

    private final boolean valid;
    private final String rejectReason;

    private ValidationResult(boolean valid, String rejectReason) {
        this.valid = valid;
        this.rejectReason = rejectReason;
    }

    public static ValidationResult ok() {
        return new ValidationResult(true, null);
    }

    public static ValidationResult reject(String reason) {
        return new ValidationResult(false, reason);
    }

    public boolean isValid() {
        return valid;
    }

    public String getRejectReason() {
        return rejectReason;
    }
}