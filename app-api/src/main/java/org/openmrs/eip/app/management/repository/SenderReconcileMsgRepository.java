package org.openmrs.eip.app.management.repository;

import org.openmrs.eip.app.management.entity.sender.SenderReconcileMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SenderReconcileMsgRepository extends JpaRepository<SenderReconcileMessage, Long> {}
