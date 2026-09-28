package com.empresa.asiscontrol.auditoria.service;

import com.empresa.asiscontrol.auditoria.dto.AuditEventResponse;
import com.empresa.asiscontrol.auditoria.repository.AuditEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditQueryService {

    private final AuditEventRepository repository;

    public AuditQueryService(AuditEventRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Page<AuditEventResponse> list(int page, int size) {
        int boundedSize = Math.min(Math.max(size, 1), 100);
        return repository.findAllByOrderByFechaHoraDesc(PageRequest.of(Math.max(page, 0), boundedSize))
                .map(AuditEventResponse::from);
    }
}
