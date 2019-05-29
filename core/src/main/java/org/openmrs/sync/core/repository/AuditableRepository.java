package org.openmrs.sync.core.repository;

import org.openmrs.sync.core.entity.BaseEntity;
import org.springframework.data.repository.NoRepositoryBean;

import java.time.LocalDateTime;
import java.util.List;

@NoRepositoryBean
public interface AuditableRepository<E extends BaseEntity> extends OpenMrsRepository<E> {

    /**
     * find all entities created or modified after the given date
     * @param lastSyncDate the last sync date
     * @return list of entities
     */
    List<E> findModelsChangedAfterDate(LocalDateTime lastSyncDate);
}
