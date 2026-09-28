package com.empresa.asiscontrol.auth.service;

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base32;
import org.springframework.stereotype.Service;

@Service
public class TotpService {

    private static final Duration STEP = Duration.ofSeconds(30);
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();
    private final TimeBasedOneTimePasswordGenerator generator;
    private final Base32 base32 = new Base32();

    public TotpService(Clock clock) {
        this.clock = clock;
        try {
            this.generator = new TimeBasedOneTimePasswordGenerator(STEP, 6,
                    TimeBasedOneTimePasswordGenerator.TOTP_ALGORITHM_HMAC_SHA1);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("TOTP no está disponible", exception);
        }
    }

    public byte[] generateSecret() {
        byte[] secret = new byte[20];
        secureRandom.nextBytes(secret);
        return secret;
    }

    public String toBase32(byte[] secret) {
        return base32.encodeToString(secret).replace("=", "");
    }

    public boolean validate(byte[] secret, String suppliedCode) {
        if (suppliedCode == null || !suppliedCode.matches("\\d{6}")) {
            return false;
        }
        SecretKey key = new SecretKeySpec(secret, generator.getAlgorithm());
        Instant now = clock.instant();
        for (int offset = -1; offset <= 1; offset++) {
            try {
                String expected = generator.generateOneTimePasswordString(key, now.plus(STEP.multipliedBy(offset)));
                if (MessageDigest.isEqual(expected.getBytes(), suppliedCode.getBytes())) {
                    return true;
                }
            } catch (GeneralSecurityException exception) {
                throw new IllegalStateException("No fue posible validar TOTP", exception);
            }
        }
        return false;
    }
}

