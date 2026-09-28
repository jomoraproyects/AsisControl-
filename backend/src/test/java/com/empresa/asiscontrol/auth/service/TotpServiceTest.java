package com.empresa.asiscontrol.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class TotpServiceTest {

    @Test
    void validatesCurrentCodeAndRejectsIncorrectCode() throws Exception {
        Instant now = Instant.parse("2026-09-28T12:00:00Z");
        byte[] secret = "12345678901234567890".getBytes();
        TimeBasedOneTimePasswordGenerator generator = new TimeBasedOneTimePasswordGenerator(
                Duration.ofSeconds(30), 6, TimeBasedOneTimePasswordGenerator.TOTP_ALGORITHM_HMAC_SHA1);
        SecretKey key = new SecretKeySpec(secret, generator.getAlgorithm());
        String valid = generator.generateOneTimePasswordString(key, now);
        TotpService service = new TotpService(Clock.fixed(now, ZoneOffset.UTC));

        assertThat(service.validate(secret, valid)).isTrue();
        assertThat(service.validate(secret, "000000")).isFalse();
        assertThat(service.validate(secret, "abc")).isFalse();
    }
}

