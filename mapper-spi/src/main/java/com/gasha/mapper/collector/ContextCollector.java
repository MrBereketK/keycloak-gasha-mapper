package com.gasha.mapper.collector;

import com.gasha.mapper.model.RiskModels.*;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.RoleModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ContextCollector {

    public EvaluationRequest collectContext(KeycloakSession session, ClientSessionContext clientSessionCtx) {
        EvaluationRequest request = new EvaluationRequest();
        RealmModel realm = session.getContext().getRealm();
        UserModel user = clientSessionCtx.getClientSession().getUserSession().getUser();
        
        request.realm = realm.getName();
        request.clientId = clientSessionCtx.getClientSession().getClient().getClientId();
        
        ContextPayload payload = new ContextPayload();
        
        // Extract IP & User Agent from the HTTP Request Context
        if (session.getContext().getConnection() != null) {
            payload.clientIp = session.getContext().getConnection().getRemoteAddr();
        }
        
        if (session.getContext().getRequestHeaders() != null) {
            payload.userAgent = session.getContext().getRequestHeaders().getRequestHeaders().getFirst("User-Agent");
            
            // Collect arbitrary headers if needed (Optional for V1)
            Map<String, String> capturedHeaders = new HashMap<>();
            capturedHeaders.put("Host", session.getContext().getRequestHeaders().getRequestHeaders().getFirst("Host"));
            payload.headers = capturedHeaders;
        }
        
        payload.timestamp = System.currentTimeMillis();
        
        // Extract User Info
        UserAttributes userAttrs = new UserAttributes();
        userAttrs.userId = user.getId();
        userAttrs.username = user.getUsername();
        
        // Fetch User Roles (Realm Level)
        List<String> roles = user.getRoleMappingsStream()
                .map(RoleModel::getName)
                .collect(Collectors.toList());
        userAttrs.assignedRoles = roles;
        
        payload.user = userAttrs;
        request.context = payload;
        
        return request;
    }
}