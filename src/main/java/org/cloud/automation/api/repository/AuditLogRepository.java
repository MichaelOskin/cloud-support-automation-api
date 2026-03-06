package org.cloud.automation.api.repository;

import org.cloud.automation.api.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Репозиторий для доступа к данным журнала аудита {@link AuditLog}.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
