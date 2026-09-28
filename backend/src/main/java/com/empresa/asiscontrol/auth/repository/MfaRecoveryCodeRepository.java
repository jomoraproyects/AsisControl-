package com.empresa.asiscontrol.auth.repository;

import com.empresa.asiscontrol.auth.entity.MfaRecoveryCode;
import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface MfaRecoveryCodeRepository extends JpaRepository<MfaRecoveryCode, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<MfaRecoveryCode> findByCredentialIdAndUsedAtIsNull(Long credentialId);
}

