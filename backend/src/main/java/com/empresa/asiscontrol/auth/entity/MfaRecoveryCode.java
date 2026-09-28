package com.empresa.asiscontrol.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "codigos_recuperacion_mfa")
public class MfaRecoveryCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "credencial_mfa_id", nullable = false)
    private MfaCredential credential;

    @Column(name = "codigo_hash", nullable = false, length = 255)
    private String codeHash;

    @Column(name = "creado_en", nullable = false)
    private Instant createdAt;

    @Column(name = "usado_en")
    private Instant usedAt;

    protected MfaRecoveryCode() {
    }

    public static MfaRecoveryCode create(MfaCredential credential, String hash, Instant now) {
        MfaRecoveryCode code = new MfaRecoveryCode();
        code.credential = credential;
        code.codeHash = hash;
        code.createdAt = now;
        return code;
    }

    public void use(Instant now) {
        this.usedAt = now;
    }

    public Long getId() { return id; }
    public String getCodeHash() { return codeHash; }
    public Instant getUsedAt() { return usedAt; }
}

