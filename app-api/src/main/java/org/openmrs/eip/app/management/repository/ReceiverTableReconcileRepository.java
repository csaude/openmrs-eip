package org.openmrs.eip.app.management.repository;

import org.openmrs.eip.app.management.entity.receiver.ReceiverTableReconciliation;
import org.openmrs.eip.app.management.entity.receiver.SiteReconciliation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReceiverTableReconcileRepository extends JpaRepository<ReceiverTableReconciliation, Long> {
	
	/**
	 * Gets a ReceiverTableReconciliation by SiteReconciliation and table name
	 * 
	 * @param siteReconciliation the SiteReconciliation to match
	 * @param tableName the table name to match
	 * @return TableReconciliation
	 */
	ReceiverTableReconciliation getBySiteReconciliationAndTableName(SiteReconciliation siteReconciliation, String tableName);
	
}