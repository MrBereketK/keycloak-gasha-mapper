package com.gasha.mapper;

import com.gasha.mapper.client.RiskEngineClient;
import com.gasha.mapper.collector.ContextCollector;
import com.gasha.mapper.model.RiskModels.EvaluationRequest;
import com.gasha.mapper.model.RiskModels.EvaluationResponse;
import com.gasha.mapper.modifier.TokenModifier;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.mappers.AbstractOIDCProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAccessTokenMapper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.AccessToken;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AiRiskProtocolMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper {

    public static final String PROVIDER_ID = "gasha-ai-risk-mapper";

    // Configuration Keys for the Admin Console
    private static final String CONF_AI_ENDPOINT = "ai.endpoint";
    private static final String CONF_TIMEOUT = "ai.timeout.ms";
    private static final String CONF_RISK_THRESHOLD = "ai.risk.threshold";
    private static final String CONF_CLAIMS_TO_REMOVE = "ai.claims.remove";

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    // Build the UI for the Keycloak Admin Console
    static {
        ProviderConfigProperty endpoint = new ProviderConfigProperty();
        endpoint.setName(CONF_AI_ENDPOINT);
        endpoint.setLabel("AI Risk Engine Endpoint");
        endpoint.setType(ProviderConfigProperty.STRING_TYPE);
        endpoint.setHelpText("The REST API URL of the FastAPI Risk Engine.");
        endpoint.setDefaultValue("http://risk-engine:8000/api/v1/evaluate");
        configProperties.add(endpoint);

        ProviderConfigProperty timeout = new ProviderConfigProperty();
        timeout.setName(CONF_TIMEOUT);
        timeout.setLabel("Timeout (ms)");
        timeout.setType(ProviderConfigProperty.STRING_TYPE);
        timeout.setHelpText("Maximum time in milliseconds to wait for the AI engine response.");
        timeout.setDefaultValue("500");
        configProperties.add(timeout);

        ProviderConfigProperty threshold = new ProviderConfigProperty();
        threshold.setName(CONF_RISK_THRESHOLD);
        threshold.setLabel("Risk Threshold");
        threshold.setType(ProviderConfigProperty.LIST_TYPE);
        threshold.setOptions(Arrays.asList("MEDIUM", "HIGH"));
        threshold.setHelpText("The minimum risk level required to trigger restriction and role suppression.");
        threshold.setDefaultValue("HIGH");
        configProperties.add(threshold);

        ProviderConfigProperty claimsToRemove = new ProviderConfigProperty();
        claimsToRemove.setName(CONF_CLAIMS_TO_REMOVE);
        claimsToRemove.setLabel("Roles/Scopes to Suppress");
        claimsToRemove.setType(ProviderConfigProperty.STRING_TYPE);
        claimsToRemove.setHelpText("Comma-separated list of privileged roles or scopes to remove if risk threshold is met.");
        claimsToRemove.setDefaultValue("admin,realm-admin");
        configProperties.add(claimsToRemove);
    }

    // Initialize our modular components
    private final ContextCollector collector = new ContextCollector();
    private final RiskEngineClient client = new RiskEngineClient();
    private final TokenModifier modifier = new TokenModifier();

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getDisplayType() {
        return "Gasha AI Risk Token Mapper";
    }

    @Override
    public String getHelpText() {
        return "Evaluates contextual risk via an external AI engine and modifies JWT claims accordingly.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    /**
     * This is the core Keycloak hook. It runs right before the token is signed and issued.
     */
    @Override
    public AccessToken transformAccessToken(AccessToken token, ProtocolMapperModel mappingModel, KeycloakSession session,
                                            UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
        
        // 1. Read configurations configured by the Administrator in the UI
        String endpoint = mappingModel.getConfig().getOrDefault(CONF_AI_ENDPOINT, "http://risk-engine:8000/api/v1/evaluate");
        int timeoutMs = Integer.parseInt(mappingModel.getConfig().getOrDefault(CONF_TIMEOUT, "500"));
        String threshold = mappingModel.getConfig().getOrDefault(CONF_RISK_THRESHOLD, "HIGH");
        String claimsToRemove = mappingModel.getConfig().getOrDefault(CONF_CLAIMS_TO_REMOVE, "");

        // 2. Collect the runtime context (IP, User-Agent, user details)
        EvaluationRequest requestPayload = collector.collectContext(session, clientSessionCtx);

        // 3. Ask the Python FastAPI service for a risk assessment
        EvaluationResponse response = client.assessRisk(endpoint, timeoutMs, requestPayload);

        // 4. Modify the token in-memory based on the risk response
        modifier.applyRiskModifications(token, response, claimsToRemove, threshold);

        return token;
    }
}