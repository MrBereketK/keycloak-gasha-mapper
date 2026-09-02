package com.gasha.mapper.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gasha.mapper.model.RiskModels.EvaluationRequest;
import com.gasha.mapper.model.RiskModels.EvaluationResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jboss.logging.Logger;

import java.io.IOException;

public class RiskEngineClient {

    private static final Logger logger = Logger.getLogger(RiskEngineClient.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    public EvaluationResponse assessRisk(String endpoint, int timeoutMs, EvaluationRequest requestPayload) {
        
        // Enforce strict timeouts so Keycloak authentication never hangs
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(timeoutMs)
                .setConnectionRequestTimeout(timeoutMs)
                .setSocketTimeout(timeoutMs)
                .build();

        try (CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(config).build()) {
            HttpPost post = new HttpPost(endpoint);
            post.setHeader("Content-Type", "application/json");
            post.setHeader("Accept", "application/json");

            // Serialize our Java Context object into JSON
            String jsonPayload = mapper.writeValueAsString(requestPayload);
            post.setEntity(new StringEntity(jsonPayload));

            // Execute the network call
            try (CloseableHttpResponse response = httpClient.execute(post)) {
                int statusCode = response.getStatusLine().getStatusCode();
                
                if (statusCode == 200) {
                    // Success! Parse the JSON response back into our Java object
                    return mapper.readValue(response.getEntity().getContent(), EvaluationResponse.class);
                } else {
                    logger.warnf("Risk Engine returned non-200 status: %d", statusCode);
                    return FallbackHandler.getFailClosedResponse("HTTP Status " + statusCode);
                }
            }
        } catch (IOException e) {
            logger.errorf("Failed to communicate with Risk Engine (Timeout/Network Error): %s", e.getMessage());
            return FallbackHandler.getFailClosedResponse("Network Error / Timeout: " + e.getMessage());
        }
    }
}