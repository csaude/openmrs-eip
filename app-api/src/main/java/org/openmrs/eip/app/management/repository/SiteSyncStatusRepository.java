package org.openmrs.eip.app.management.repository;

import org.openmrs.eip.app.management.entity.receiver.ReceiverSyncStatus;
import org.openmrs.eip.app.management.entity.receiver.SiteInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SiteSyncStatusRepository extends JpaRepository<ReceiverSyncStatus, Long> {
	
	/**
	 * Gets the sync status for the specified site
	 * 
	 * @param siteInfo the site
	 * @return ReceiverSyncStatus
	 */
	ReceiverSyncStatus findBySiteInfo(SiteInfo siteInfo);
	
}
