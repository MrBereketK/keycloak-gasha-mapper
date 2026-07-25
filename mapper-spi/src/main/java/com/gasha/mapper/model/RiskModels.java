package com.gasha.mapper.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public class RiskModels {

    // --- REQUEST MODELS ---

    public static class EvaluationRequest {
        @JsonProperty("realm")
        public String realm;
        
        @JsonProperty("client_id")
        public String clientId;
        
        @JsonProperty("context")
        public ContextPayload context;
    }

    public static class ContextPayload {
        @JsonProperty("client_ip")
        public String clientIp;
        
        @JsonProperty("user_agent")
        public String userAgent;
        
        @JsonProperty("timestamp")
        public long timestamp;
        
        @JsonProperty("headers")
        public Map<String, String> headers;
        
        @JsonProperty("user")
        public UserAttributes user;
    }

    public static class UserAttributes {
        @JsonProperty("user_id")
        public String userId;
        
        @JsonProperty("username")
        public String username;
        
        @JsonProperty("assigned_roles")
        public List<String> assignedRoles;
    }

    // --- RESPONSE MODELS ---

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EvaluationResponse {
        @JsonProperty("risk_level")
        public String riskLevel; // "LOW", "MEDIUM", "HIGH"
        
        @JsonProperty("risk_score")
        public double riskScore;
        
        @JsonProperty("reasons")
        public List<String> reasons;
        
        @JsonProperty("evaluator_version")
        public String evaluatorVersion;
    }
}