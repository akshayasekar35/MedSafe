package com.medsafe.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Component
public class TokenService {

    private static final String HMAC_ALGO = "HmacSHA256";
    private final String secretKey;

    public TokenService(@Value("${app.jwt.secret:MedSafeSecureApplicationSecretKeyForHealthcareInteractionSystem2026!}") String secretKey) {
        this.secretKey = secretKey;
    }

    public String generateToken(Long userId, String email) {
        long timestamp = System.currentTimeMillis();
        String payload = userId + ":" + email + ":" + timestamp;
        String payloadEncoded = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        String signature = sign(payloadEncoded);
        return payloadEncoded + "." + signature;
    }

    public TokenPayload validateAndExtract(String token) {
        if (token == null || !token.contains(".")) {
            return null;
        }
        String[] parts = token.split("\\.");
        if (parts.length != 2) {
            return null;
        }
        String payloadEncoded = parts[0];
        String signature = parts[1];

        String expectedSignature = sign(payloadEncoded);
        if (!java.security.MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8))) {
            return null;
        }

        try {
            String payload = new String(Base64.getUrlDecoder().decode(payloadEncoded), StandardCharsets.UTF_8);
            String[] fields = payload.split(":");
            if (fields.length < 3) {
                return null;
            }
            Long userId = Long.parseLong(fields[0]);
            String email = fields[1];
            long timestamp = Long.parseLong(fields[2]);

            // Token valid for 7 days
            long maxAgeMs = 7L * 24 * 60 * 60 * 1000;
            if (System.currentTimeMillis() - timestamp > maxAgeMs) {
                return null;
            }

            return new TokenPayload(userId, email);
        } catch (Exception e) {
            return null;
        }
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), HMAC_ALGO);
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hmacBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Failed to calculate HMAC signature", e);
        }
    }

    public static class TokenPayload {
        private final Long userId;
        private final String email;

        public TokenPayload(Long userId, String email) {
            this.userId = userId;
            this.email = email;
        }

        public Long getUserId() {
            return userId;
        }

        public String getEmail() {
            return email;
        }
    }
}
