package org.openmrs.eip.app.management.repository;

import org.openmrs.eip.app.management.entity.SenderSyncMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SenderSyncMessageRepository extends JpaRepository<SenderSyncMessage, Long> {}
