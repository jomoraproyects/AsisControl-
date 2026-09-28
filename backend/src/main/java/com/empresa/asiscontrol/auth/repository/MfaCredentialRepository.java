package com.empresa.asiscontrol.auth.repository;

import com.empresa.asiscontrol.auth.entity.MfaCredential;
import com.empresa.asiscontrol.auth.entity.MfaCredentialStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MfaCredentialRepository extends JpaRepository<MfaCredential, Long> {
    Optional<MfaCredential> findFirstByUsuarioIdAndEstadoOrderByIdDesc(Long usuarioId, MfaCredentialStatus status);
    List<MfaCredential> findByUsuarioIdAndEstadoIn(Long usuarioId, List<MfaCredentialStatus> states);
}

