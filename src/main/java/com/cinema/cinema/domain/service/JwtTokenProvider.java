package com.cinema.cinema.domain.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class JwtTokenProvider {

    private static final String DEFAULT_SECRET = "cinema-super-secret-jwt-token-signing-key-2026-32-bytes-secure";
    private static final long DEFAULT_EXPIRATION_SECONDS = 86400L; // 24 hours
    private static final String HMAC_SHA256 = "HmacSHA256";

    private final byte[] secretKeyBytes;
    private final long expirationSeconds;
    private final ObjectMapper objectMapper;

    public JwtTokenProvider() {
        this(DEFAULT_SECRET, DEFAULT_EXPIRATION_SECONDS);
    }

    public JwtTokenProvider(String secret, long expirationSeconds) {
        this.secretKeyBytes = (secret != null ? secret : DEFAULT_SECRET).getBytes(StandardCharsets.UTF_8);
        this.expirationSeconds = expirationSeconds > 0 ? expirationSeconds : DEFAULT_EXPIRATION_SECONDS;
        this.objectMapper = new ObjectMapper();
    }

    public String generateToken(Long userId, String email, String fullName) {
        return generateToken(userId, email, fullName, java.util.Collections.emptySet());
    }

    public String generateToken(Long userId, String email, String fullName, java.util.Set<String> roles) {
        try {
            long now = Instant.now().getEpochSecond();
            long exp = now + expirationSeconds;

            Map<String, Object> headerMap = new HashMap<>();
            headerMap.put("alg", "HS256");
            headerMap.put("typ", "JWT");

            Map<String, Object> payloadMap = new HashMap<>();
            payloadMap.put("sub", String.valueOf(userId));
            payloadMap.put("userId", userId);
            payloadMap.put("email", email);
            payloadMap.put("fullName", fullName);
            payloadMap.put("roles", roles != null ? roles : java.util.Collections.emptySet());
            payloadMap.put("iat", now);
            payloadMap.put("exp", exp);

            String headerEncoded = Base64.getUrlEncoder().withoutPadding().encodeToString(objectMapper.writeValueAsBytes(headerMap));
            String payloadEncoded = Base64.getUrlEncoder().withoutPadding().encodeToString(objectMapper.writeValueAsBytes(payloadMap));
            String dataToSign = headerEncoded + "." + payloadEncoded;

            String signatureEncoded = sign(dataToSign);

            return dataToSign + "." + signatureEncoded;
        } catch (Exception e) {
            throw new RuntimeException("Error generating JWT token", e);
        }
    }

    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return false;
            }
            String dataToSign = parts[0] + "." + parts[1];
            String expectedSignature = sign(dataToSign);
            if (!expectedSignature.equals(parts[2])) {
                return false;
            }

            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            JsonNode payloadNode = objectMapper.readTree(payloadBytes);
            if (payloadNode.has("exp")) {
                long exp = payloadNode.get("exp").asLong();
                long now = Instant.now().getEpochSecond();
                return now <= exp;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Long getUserIdFromToken(String token) {
        JsonNode payload = extractPayload(token);
        if (payload != null) {
            if (payload.has("userId")) {
                return payload.get("userId").asLong();
            } else if (payload.has("sub")) {
                return Long.parseLong(payload.get("sub").asText());
            }
        }
        return null;
    }

    public String getEmailFromToken(String token) {
        JsonNode payload = extractPayload(token);
        if (payload != null && payload.has("email")) {
            return payload.get("email").asText();
        }
        return null;
    }

    public String getFullNameFromToken(String token) {
        JsonNode payload = extractPayload(token);
        if (payload != null && payload.has("fullName")) {
            return payload.get("fullName").asText();
        }
        return null;
    }

    public java.util.Set<String> getRolesFromToken(String token) {
        JsonNode payload = extractPayload(token);
        if (payload != null && payload.has("roles")) {
            JsonNode rolesNode = payload.get("roles");
            if (rolesNode.isArray()) {
                java.util.Set<String> roles = new java.util.HashSet<>();
                for (JsonNode r : rolesNode) {
                    roles.add(r.asText());
                }
                return roles;
            }
        }
        return java.util.Collections.emptySet();
    }

    private JsonNode extractPayload(String token) {
        if (!validateToken(token)) {
            return null;
        }
        try {
            String[] parts = token.split("\\.");
            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            return objectMapper.readTree(payloadBytes);
        } catch (Exception e) {
            return null;
        }
    }

    private String sign(String data) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance(HMAC_SHA256);
        mac.init(new SecretKeySpec(secretKeyBytes, HMAC_SHA256));
        byte[] signatureBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes);
    }
}
