package com.classora.security;

import com.classora.model.Role;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private static final String SECRET = "a-long-test-secret-value-1234567890";
    private final Clock now = Clock.fixed(Instant.parse("2026-11-23T10:00:00Z"), ZoneOffset.UTC);
    private final JwtService jwt = new JwtService(SECRET, Duration.ofHours(1), now);

    @Test
    void roundTripKeepsTheClaims() throws Exception {
        String token = jwt.issue(7, "a@b.com", Role.TEACHER);
        AuthenticatedUser user = jwt.verify(token);
        assertEquals(7, user.userId());
        assertEquals("a@b.com", user.email());
        assertEquals(Role.TEACHER, user.role());
    }

    @Test
    void tokenHasThreeParts() {
        assertEquals(3, jwt.issue(1, "a@b.com", Role.STUDENT).split("\\.").length);
    }

    @Test
    void expiredTokensAreRejected() {
        String token = jwt.issue(1, "a@b.com", Role.STUDENT);
        Clock later = Clock.fixed(Instant.parse("2026-11-23T11:00:01Z"), ZoneOffset.UTC);
        JwtService laterService = new JwtService(SECRET, Duration.ofHours(1), later);
        assertThrows(InvalidTokenException.class, () -> laterService.verify(token));
    }

    @Test
    void aWrongSecretIsRejected() {
        String token = jwt.issue(1, "a@b.com", Role.ADMIN);
        JwtService other = new JwtService("a-different-secret-value-0987654321", Duration.ofHours(1), now);
        assertThrows(InvalidTokenException.class, () -> other.verify(token));
    }

    @Test
    void aTamperedPayloadIsRejected() {
        String[] parts = jwt.issue(1, "a@b.com", Role.STUDENT).split("\\.");
        String forgedPayload = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"userId\":1,\"email\":\"a@b.com\",\"role\":\"admin\",\"exp\":9999999999}".getBytes());
        String forged = parts[0] + "." + forgedPayload + "." + parts[2];
        assertThrows(InvalidTokenException.class, () -> jwt.verify(forged));
    }

    @Test
    void unsignedTokensWithAlgNoneAreRejected() {
        String header = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes());
        String payload = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"userId\":1,\"email\":\"a@b.com\",\"role\":\"admin\",\"exp\":9999999999}".getBytes());
        assertThrows(InvalidTokenException.class, () -> jwt.verify(header + "." + payload + "."));
    }

    @Test
    void garbageIsRejected() {
        assertThrows(InvalidTokenException.class, () -> jwt.verify("not-a-token"));
        assertThrows(InvalidTokenException.class, () -> jwt.verify(null));
        assertThrows(InvalidTokenException.class, () -> jwt.verify("a.b.c"));
    }
}
