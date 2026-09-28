package com.empresa.asiscontrol.auth.service;

import com.empresa.asiscontrol.auditoria.entity.AuditAction;
import com.empresa.asiscontrol.auditoria.entity.AuditOutcome;
import com.empresa.asiscontrol.auditoria.service.AuditService;
import com.empresa.asiscontrol.auth.dto.AuthenticationResponse;
import com.empresa.asiscontrol.auth.dto.MfaConfirmationResponse;
import com.empresa.asiscontrol.auth.dto.MfaEnrollmentResponse;
import com.empresa.asiscontrol.auth.dto.MfaVerificationRequest;
import com.empresa.asiscontrol.auth.entity.MfaCredential;
import com.empresa.asiscontrol.auth.entity.MfaCredentialStatus;
import com.empresa.asiscontrol.auth.entity.MfaRecoveryCode;
import com.empresa.asiscontrol.auth.repository.MfaCredentialRepository;
import com.empresa.asiscontrol.auth.repository.MfaRecoveryCodeRepository;
import com.empresa.asiscontrol.auth.security.AsisUserPrincipal;
import com.empresa.asiscontrol.shared.exception.AuthenticationFailedException;
import com.empresa.asiscontrol.shared.exception.ConflictException;
import com.empresa.asiscontrol.shared.exception.InvalidRequestException;
import com.empresa.asiscontrol.shared.web.RequestMetadata;
import com.empresa.asiscontrol.usuarios.entity.Usuario;
import com.empresa.asiscontrol.usuarios.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.codec.binary.Base32;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MfaService {

    private static final int RECOVERY_CODE_COUNT = 8;

    private final AuthenticationService authenticationService;
    private final MfaCredentialRepository credentialRepository;
    private final MfaRecoveryCodeRepository recoveryCodeRepository;
    private final UsuarioRepository usuarioRepository;
    private final MfaSecretCipher cipher;
    private final TotpService totpService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationRateLimiter rateLimiter;
    private final AuditService auditService;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Base32 base32 = new Base32();

    public MfaService(AuthenticationService authenticationService,
                      MfaCredentialRepository credentialRepository,
                      MfaRecoveryCodeRepository recoveryCodeRepository,
                      UsuarioRepository usuarioRepository,
                      MfaSecretCipher cipher,
                      TotpService totpService,
                      PasswordEncoder passwordEncoder,
                      AuthenticationRateLimiter rateLimiter,
                      AuditService auditService,
                      Clock clock) {
        this.authenticationService = authenticationService;
        this.credentialRepository = credentialRepository;
        this.recoveryCodeRepository = recoveryCodeRepository;
        this.usuarioRepository = usuarioRepository;
        this.cipher = cipher;
        this.totpService = totpService;
        this.passwordEncoder = passwordEncoder;
        this.rateLimiter = rateLimiter;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional
    public MfaEnrollmentResponse enroll(HttpServletRequest request) {
        PendingAuthentication pending = authenticationService.requirePending(request.getSession(false));
        if (credentialRepository.findFirstByUsuarioIdAndEstadoOrderByIdDesc(
                pending.userId(), MfaCredentialStatus.ACTIVA).isPresent()) {
            throw new ConflictException("MFA_ALREADY_ENROLLED", "El usuario ya tiene MFA activo");
        }
        credentialRepository.findByUsuarioIdAndEstadoIn(pending.userId(), List.of(MfaCredentialStatus.PENDIENTE))
                .forEach(credential -> credential.revoke(clock.instant()));
        credentialRepository.flush();
        Usuario user = usuarioRepository.findById(pending.userId()).orElseThrow();
        byte[] secret = totpService.generateSecret();
        credentialRepository.save(MfaCredential.pending(user, cipher.encrypt(secret), clock.instant()));
        String encoded = totpService.toBase32(secret);
        String issuer = url("AsisControl");
        String account = url("AsisControl:" + pending.username());
        String uri = "otpauth://totp/" + account + "?secret=" + encoded
                + "&issuer=" + issuer + "&algorithm=SHA1&digits=6&period=30";
        return new MfaEnrollmentResponse(encoded, uri);
    }

    @Transactional
    public MfaConfirmationResponse confirm(String code, HttpServletRequest request,
                                           HttpServletResponse response, RequestMetadata metadata) {
        PendingAuthentication pending = authenticationService.requirePending(request.getSession(false));
        String rateKey = rateKey(metadata, pending);
        rateLimiter.check(rateKey);
        MfaCredential credential = credentialRepository.findFirstByUsuarioIdAndEstadoOrderByIdDesc(
                        pending.userId(), MfaCredentialStatus.PENDIENTE)
                .orElseThrow(() -> new InvalidRequestException("MFA_NOT_ENROLLED", "No existe enrolamiento pendiente"));
        if (!totpService.validate(cipher.decrypt(credential.getSecretoCifrado()), code)) {
            fail(rateKey, pending, metadata, "INVALID_TOTP");
        }
        credential.confirm(clock.instant());
        List<String> plainCodes = generateRecoveryCodes(credential);
        rateLimiter.success(rateKey);
        AsisUserPrincipal principal = authenticationService.completePending(pending, request, response, metadata);
        return new MfaConfirmationResponse(AuthenticationResponse.authenticated(principal), plainCodes);
    }

    @Transactional
    public AuthenticationResponse verify(MfaVerificationRequest input, HttpServletRequest request,
                                         HttpServletResponse response, RequestMetadata metadata) {
        PendingAuthentication pending = authenticationService.requirePending(request.getSession(false));
        String rateKey = rateKey(metadata, pending);
        rateLimiter.check(rateKey);
        MfaCredential credential = credentialRepository.findFirstByUsuarioIdAndEstadoOrderByIdDesc(
                        pending.userId(), MfaCredentialStatus.ACTIVA)
                .orElseThrow(() -> new InvalidRequestException("MFA_NOT_ENROLLED", "MFA no está enrolado"));
        boolean hasTotp = input.totpCode() != null && !input.totpCode().isBlank();
        boolean hasRecovery = input.recoveryCode() != null && !input.recoveryCode().isBlank();
        if (hasTotp == hasRecovery) {
            throw new InvalidRequestException("MFA_METHOD_REQUIRED",
                    "Envíe exactamente un código TOTP o un código de recuperación");
        }
        boolean valid = hasTotp
                ? totpService.validate(cipher.decrypt(credential.getSecretoCifrado()), input.totpCode())
                : consumeRecoveryCode(credential, input.recoveryCode());
        if (!valid) {
            fail(rateKey, pending, metadata, hasTotp ? "INVALID_TOTP" : "INVALID_RECOVERY_CODE");
        }
        rateLimiter.success(rateKey);
        AsisUserPrincipal principal = authenticationService.completePending(pending, request, response, metadata);
        return AuthenticationResponse.authenticated(principal);
    }

    @Transactional
    public void revokeForUser(Long userId) {
        credentialRepository.findByUsuarioIdAndEstadoIn(userId,
                        List.of(MfaCredentialStatus.PENDIENTE, MfaCredentialStatus.ACTIVA))
                .forEach(credential -> credential.revoke(clock.instant()));
    }

    private List<String> generateRecoveryCodes(MfaCredential credential) {
        List<String> plain = new ArrayList<>();
        for (int i = 0; i < RECOVERY_CODE_COUNT; i++) {
            byte[] random = new byte[10];
            secureRandom.nextBytes(random);
            String normalized = base32.encodeToString(random).replace("=", "").toUpperCase(Locale.ROOT);
            String formatted = normalized.replaceAll("(.{4})(?!$)", "$1-");
            plain.add(formatted);
            recoveryCodeRepository.save(MfaRecoveryCode.create(
                    credential, passwordEncoder.encode(normalized), clock.instant()));
        }
        return List.copyOf(plain);
    }

    private boolean consumeRecoveryCode(MfaCredential credential, String supplied) {
        String normalized = supplied.replace("-", "").trim().toUpperCase(Locale.ROOT);
        for (MfaRecoveryCode code : recoveryCodeRepository.findByCredentialIdAndUsedAtIsNull(credential.getId())) {
            if (passwordEncoder.matches(normalized, code.getCodeHash())) {
                code.use(clock.instant());
                return true;
            }
        }
        return false;
    }

    private void fail(String rateKey, PendingAuthentication pending, RequestMetadata metadata, String reason) {
        rateLimiter.failure(rateKey);
        Usuario user = usuarioRepository.findById(pending.userId()).orElse(null);
        auditService.recordIndependent(user, pending.username(), AuditAction.LOGIN, AuditOutcome.FALLIDO,
                "USUARIO", user == null ? null : user.getPublicId().toString(),
                Map.of("reason", reason), metadata);
        throw new AuthenticationFailedException("Código de autenticación inválido");
    }

    private String rateKey(RequestMetadata metadata, PendingAuthentication pending) {
        return "mfa|" + metadata.ip() + "|" + pending.username().toLowerCase(Locale.ROOT);
    }

    private String url(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
