package org.openmrs.eip.app.management.service;

import java.util.List;

import org.openmrs.eip.app.management.entity.receiver.JmsMessage;
import org.openmrs.eip.app.management.entity.receiver.ReconciliationMessage;

/**
 * Contains reconciliation service methods.
 */
public interface ReconcileService extends Service {
	
	/**
	 * Processes a {@link JmsMessage}
	 *
	 * @param jmsMessage the message to process
	 */
	void processJmsMessage(JmsMessage jmsMessage);
	
	/**
	 * Updates the processed count, if the uuids were found, they are marked as found otherwise a sync
	 * request is created for the associated entity.
	 *
	 * @param message the message to update
	 * @param found specifies whether the uuids were found or not
	 * @param uuids the uuids
	 */
	void updateReconciliationMessage(ReconciliationMessage message, boolean found, List<String> uuids);
	
	/**
	 * Inserts or updates a table reconciliation based on the state of the specified message.
	 * Implementation of this method assumes no parallel invocations from multiple threads for
	 * reconciliation messages for the same site table.
	 *
	 * @param message the ReconciliationMessage instance
	 */
	void updateTableReconciliation(ReconciliationMessage message);
	
}