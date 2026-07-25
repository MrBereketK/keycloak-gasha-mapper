package com.gasha.mapper.modifier;

import com.gasha.mapper.model.RiskModels.EvaluationResponse;
import org.keycloak.representations.AccessToken;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TokenModifier {

    public void applyRiskModifications(AccessToken token, EvaluationResponse riskResponse, String claimsToRemoveStr, String riskThreshold) {
        
        String assessedLevel = riskResponse.riskLevel;
        
        // 1. Always inject the risk context claims into the token
        token.getOtherClaims().put("risk_level", assessedLevel);
        
        boolean isRestricted = isThresholdMet(assessedLevel, riskThreshold);
        token.getOtherClaims().put("restricted", isRestricted);

        // 2. If the risk is high enough, strip the configured privileged roles/scopes
        if (isRestricted && claimsToRemoveStr != null && !claimsToRemoveStr.trim().isEmpty()) {
            
            List<String> privilegedRoles = Arrays.asList(claimsToRemoveStr.split("\\s*,\\s*"));
            
            // Strip from Realm Roles
            AccessToken.Access realmAccess = token.getRealmAccess();
            if (realmAccess != null && realmAccess.getRoles() != null) {
                Set<String> roles = realmAccess.getRoles();
                roles.removeAll(privilegedRoles);
            }

            // Strip from Resource/Client Roles
            Map<String, AccessToken.Access> resourceAccess = token.getResourceAccess();
            if (resourceAccess != null) {
                for (AccessToken.Access access : resourceAccess.values()) {
                    if (access.getRoles() != null) {
                        access.getRoles().removeAll(privilegedRoles);
                    }
                }
            }
        }
    }

    private boolean isThresholdMet(String assessedLevel, String riskThreshold) {
        if ("HIGH".equalsIgnoreCase(assessedLevel)) {
            return true;
        }
        if ("MEDIUM".equalsIgnoreCase(assessedLevel) && "MEDIUM".equalsIgnoreCase(riskThreshold)) {
            return true;
        }
        return false;
    }
}