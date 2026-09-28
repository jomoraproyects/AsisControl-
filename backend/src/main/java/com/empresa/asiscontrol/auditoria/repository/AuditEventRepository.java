package com.empresa.asiscontrol.auditoria.repository;

import com.empresa.asiscontrol.auditoria.entity.AuditAction;
import com.empresa.asiscontrol.auditoria.entity.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    long countByAccion(AuditAction accion);
    Page<AuditEvent> findAllByOrderByFechaHoraDesc(Pageable pageable);
}

