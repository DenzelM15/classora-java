package com.classora.security;

import com.classora.model.Role;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Issues and checks HS256 JSON Web Tokens.
 * The format matches what the Node version produced with jsonwebtoken
 * (claims: userId, email, role, iat, exp), so tokens signed with the same
 * secret keep working during a migration.
 */
public class JwtService {

    private static final String HEADER_JSON = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
    private static final String HMAC = "HmacSHA256";
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private final byte[] secret;
    private final Duration lifetime;
    private final Clock clock;
    private final ObjectMapper mapper = new ObjectMapper();

    public JwtService(String secret, Duration lifetime, Clock clock) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("JWT secret must not be empty");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.lifetime = lifetime;
        this.clock = clock;
    }

    public String issue(int userId, String email, Role role) {
        long issuedAt = clock.instant().getEpochSecond();

        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("userId", userId);
        claims.put("email", email);
        claims.put("role", role.value());
        claims.put("iat", issuedAt);
        claims.put("exp", issuedAt + lifetime.getSeconds());

        try {
            String header = encode(HEADER_JSON.getBytes(StandardCharsets.UTF_8));
            String payload = encode(mapper.writeValueAsBytes(claims));
            String signature = encode(sign(header + "." + payload));
            return header + "." + payload + "." + signature;
        } catch (Exception e) {
            throw new IllegalStateException("Could not create token", e);
        }
    }

    public AuthenticatedUser verify(String token) throws InvalidTokenException {
        if (token == null) {
            throw new InvalidTokenException("Missing token");
        }
        String[] parts = token.split("\\.", -1);
        if (parts.length != 3) {
            throw new InvalidTokenException("Malformed token");
        }

        try {
            Map<String, Object> header = readJson(parts[0]);
            if (!"HS256".equals(header.get("alg"))) {
                throw new InvalidTokenException("Unsupported algorithm");
            }

            byte[] expected = sign(parts[0] + "." + parts[1]);
            byte[] actual = DECODER.decode(parts[2]);
            if (!MessageDigest.isEqual(expected, actual)) {
                throw new InvalidTokenException("Bad signature");
            }

            Map<String, Object> claims = readJson(parts[1]);

            Object exp = claims.get("exp");
            if (!(exp instanceof Number) || ((Number) exp).longValue() <= clock.instant().getEpochSecond()) {
                throw new InvalidTokenException("Token expired");
            }

            Object userId = claims.get("userId");
            Object email = claims.get("email");
            Role role = Role.fromValue(String.valueOf(claims.get("role")))
                    .orElseThrow(() -> new InvalidTokenException("Unknown role"));

            if (!(userId instanceof Number) || !(email instanceof String)) {
                throw new InvalidTokenException("Missing claims");
            }
            return new AuthenticatedUser(((Number) userId).intValue(), (String) email, role);

        } catch (InvalidTokenException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidTokenException("Unreadable token");
        }
    }

    private Map<String, Object> readJson(String base64Part) throws Exception {
        return mapper.readValue(DECODER.decode(base64Part), new TypeReference<Map<String, Object>>() { });
    }

    private byte[] sign(String data) throws GeneralSecurityException {
        Mac mac = Mac.getInstance(HMAC);
        mac.init(new SecretKeySpec(secret, HMAC));
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    private static String encode(byte[] bytes) {
        return ENCODER.encodeToString(bytes);
    }
}
