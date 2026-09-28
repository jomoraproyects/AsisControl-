package com.empresa.asiscontrol.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.empresa.asiscontrol.shared.config.SecurityProperties;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;

class MfaSecretCipherTest {

    private final SecurityProperties properties = new SecurityProperties(
            Base64.getEncoder().encodeToString(new byte[32]), List.of("http://localhost"),
            Duration.ofMinutes(5), 8, Duration.ofMinutes(15));
    private final MfaSecretCipher cipher = new MfaSecretCipher(properties);

    @Test
    void encryptsWithRandomIvAndDecrypts() {
        byte[] secret = "top-secret".getBytes();
        String first = cipher.encrypt(secret);
        String second = cipher.encrypt(secret);

        assertThat(first).startsWith("v1:").isNotEqualTo(second);
        assertThat(cipher.decrypt(first)).isEqualTo(secret);
    }

    @Test
    void rejectsTamperedCiphertext() {
        String encrypted = cipher.encrypt("secret".getBytes());
        String tampered = encrypted.substring(0, encrypted.length() - 2) + "AA";
        assertThatThrownBy(() -> cipher.decrypt(tampered)).isInstanceOf(IllegalStateException.class);
    }
}

