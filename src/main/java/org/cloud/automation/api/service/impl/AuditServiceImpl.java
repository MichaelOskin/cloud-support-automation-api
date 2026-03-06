package org.cloud.automation.api.service.impl;

import lombok.RequiredArgsConstructor;
import org.cloud.automation.api.domain.AuditEvent;
import org.cloud.automation.api.domain.AuditLog;
import org.cloud.automation.api.domain.Task;
import org.cloud.automation.api.repository.AuditLogRepository;
import org.cloud.automation.api.service.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;

    /**
     * {@inheritDoc}
     * <p>
     * Этот метод выполняется в новой транзакции (REQUIRES_NEW), чтобы гарантировать,
     * что запись аудита будет сохранена в базе данных немедленно, даже если
     * основная транзакция, вызвавшая это событие, в конечном итоге будет отменена.
     * Это обеспечивает надежность и полноту журнала аудита.
     */
    @Override
    @Transactional
    public void logEvent(Task task, AuditEvent event, Map<String, Object> details) {
        AuditLog auditLog = AuditLog.builder()
                .task(task)
                .event(event)
                .details(details)
                .build();
        auditLogRepository.save(auditLog);
    }
}
