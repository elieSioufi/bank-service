package com.examples.bankservice.model;

import java.io.Serializable;

public class CreditResponse implements Serializable {

    private String correlationId;
    private String username;
    private boolean approved;
    private String message;

    public CreditResponse() {}

    public CreditResponse(String correlationId, String username, boolean approved, String message) {
        this.correlationId = correlationId;
        this.username = username;
        this.approved = approved;
        this.message = message;
    }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public boolean isApproved() { return approved; }
    public void setApproved(boolean approved) { this.approved = approved; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
