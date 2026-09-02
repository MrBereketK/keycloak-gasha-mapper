package com.gasha.mapper.client;

import com.gasha.mapper.model.RiskModels.EvaluationResponse;
import org.jboss.logging.Logger;

public class FallbackHandler {
    
    private static final Logger logger = Logger.getLogger(FallbackHandler.class);

    /**
     * Fallback strategy: If the AI Risk Engine is offline, times out, or returns a 500 error,
     * we default to HIGH risk (Fail-Closed) to ensure security is strictly maintained.
     */
    public static EvaluationResponse getFailClosedResponse(String reason) {
        logger.warnf("Applying Fail-Closed fallback risk assessment. Reason: %s", reason);
        
        EvaluationResponse fallback = new EvaluationResponse();
        fallback.riskLevel = "HIGH";
        fallback.riskScore = 1.0;
        fallback.evaluatorVersion = "fallback-fail-closed-v1";
        
        return fallback;
    }
}