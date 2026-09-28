package com.empresa.asiscontrol.auth.service;

import com.empresa.asiscontrol.shared.config.SecurityProperties;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class MfaSecretCipher {

    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;
    private static final String PREFIX = "v1:";

    private final SecurityProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public MfaSecretCipher(SecurityProperties properties) {
        this.properties = properties;
    }

    public String encrypt(byte[] plain) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD("asiscontrol:mfa:v1".getBytes(StandardCharsets.UTF_8));
            byte[] encrypted = cipher.doFinal(plain);
            return PREFIX + Base64.getEncoder().encodeToString(
                    ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array());
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("No fue posible cifrar el secreto MFA", exception);
        }
    }

    public byte[] decrypt(String value) {
        if (value == null || !value.startsWith(PREFIX)) {
            throw new IllegalStateException("Versión de secreto MFA no soportada");
        }
        try {
            byte[] combined = Base64.getDecoder().decode(value.substring(PREFIX.length()));
            ByteBuffer buffer = ByteBuffer.wrap(combined);
            byte[] iv = new byte[IV_LENGTH];
            buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD("asiscontrol:mfa:v1".getBytes(StandardCharsets.UTF_8));
            return cipher.doFinal(encrypted);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("No fue posible descifrar el secreto MFA", exception);
        }
    }

    private SecretKeySpec key() {
        String configured = properties.mfaEncryptionKey();
        if (configured == null || configured.isBlank()) {
            throw new IllegalStateException("MFA_ENCRYPTION_KEY es obligatoria para utilizar MFA");
        }
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(configured);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("MFA_ENCRYPTION_KEY debe estar codificada en Base64", exception);
        }
        if (decoded.length != 32) {
            throw new IllegalStateException("MFA_ENCRYPTION_KEY debe contener exactamente 32 bytes");
        }
        return new SecretKeySpec(decoded, "AES");
    }
}

