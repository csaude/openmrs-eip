package org.openmrs.eip.app.management.repository;

import java.util.List;

import org.openmrs.eip.app.management.entity.sender.SenderReconcileMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SenderReconcileMsgRepository extends JpaRepository<SenderReconcileMessage, Long> {
	
	/**
	 * Reads a batch of reconciliation messages.
	 * 
	 * @param page {@link Pageable} object
	 * @return list of messages
	 */
	@Query("SELECT m FROM SenderReconcileMessage m")
	List<SenderReconcileMessage> getBatch(Pageable page);
	
}
