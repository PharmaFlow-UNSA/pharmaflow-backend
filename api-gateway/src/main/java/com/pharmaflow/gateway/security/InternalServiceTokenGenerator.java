package com.pharmaflow.gateway.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Service for generating and validating internal service tokens.
 * These tokens are used for inter-service communication to ensure
 * requests come from the API Gateway and not directly from external sources.
 *
 * Uses HMAC-SHA256 for secure token generation following NIST standards.
 */
@Component
public class InternalServiceTokenGenerator {

    private static final Logger log = LoggerFactory.getLogger(InternalServiceTokenGenerator.class);
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    @Value("${internal.service.secret:pharmaflow-internal-secret-2024-very-secure}")
    private String internalSecret;

    /**
     * Generate internal service token using HMAC-SHA256.
     * Token format: Base64(HMAC-SHA256(serviceName:timestamp:secret))
     *
     * @param serviceName Name of the service generating the token
     * @return Base64-encoded HMAC token
     */
    public String generateInternalToken(String serviceName) {
        try {
            long timestamp = System.currentTimeMillis();
            String data = serviceName + ":" + timestamp + ":" + internalSecret;

            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                internalSecret.getBytes(StandardCharsets.UTF_8),
                HMAC_ALGORITHM
            );
            mac.init(secretKeySpec);

            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            String token = Base64.getEncoder().encodeToString(hash);

            log.debug("Generated internal service token for: {}", serviceName);
            return token;

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Failed to generate internal token", e);
            throw new RuntimeException("Failed to generate internal token", e);
        }
    }
}
