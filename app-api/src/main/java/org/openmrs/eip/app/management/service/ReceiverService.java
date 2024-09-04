package org.openmrs.eip.app.management.service;

import static org.openmrs.eip.app.SyncConstants.MGT_TX_MGR;

import org.openmrs.eip.app.management.entity.receiver.JmsMessage;
import org.openmrs.eip.app.management.entity.receiver.ReceiverRetryQueueItem;
import org.openmrs.eip.app.management.entity.receiver.SyncMessage;
import org.openmrs.eip.app.management.entity.receiver.SyncedMessage.SyncOutcome;
import org.openmrs.eip.component.management.hash.entity.BaseHashEntity;
import org.springframework.transaction.annotation.Transactional;

/**
 * Contains methods for managing receiver items
 */
public interface ReceiverService extends Service {
	
	/**
	 * Moves the specified {@link SyncMessage} to the synced queue
	 *
	 * @param message the message to move
	 * @param outcome {@link SyncOutcome}
	 */
	void moveToSyncedQueue(SyncMessage message, SyncOutcome outcome);
	
	/**
	 * Moves the specified {@link ReceiverRetryQueueItem} to the archive queue
	 *
	 * @param retry the retry to archive
	 */
	void archiveRetry(ReceiverRetryQueueItem retry);
	
	/**
	 * Updates the entity hash to match the current state in the database
	 * 
	 * @param modelClassname the model classname of the entity
	 * @param identifier the entity unique identifier
	 */
	void updateHash(String modelClassname, String identifier);
	
	/**
	 * Checks if an entity has an item in the sync queue
	 *
	 * @param identifier the entity identifier
	 * @param modelClassname the entity model classname
	 * @return true if the entity has an item in the sync queue otherwise false
	 */
	boolean hasSyncItem(String identifier, String modelClassname);
	
	/**
	 * Checks if an entity has an item in the retry queue
	 * 
	 * @param identifier the entity identifier
	 * @param modelClassname the entity model classname
	 * @return true if the entity has an item in the retry queue otherwise false
	 */
	boolean hasRetryItem(String identifier, String modelClassname);
	
	/**
	 * Adds a sync item to the error and synced queues.
	 * 
	 * @param message the failed sync item
	 * @param exceptionType the exception type name
	 * @param errorMsg the error message
	 */
	void processFailedSyncItem(SyncMessage message, String exceptionType, String errorMsg);
	
	/**
	 * Adds a sync item to the conflict and synced queues.
	 *
	 * @param message the conflicted sync item
	 */
	void processConflictedSyncItem(SyncMessage message);
	
	/**
	 * Moves the specified retry item to the conflict queue
	 *
	 * @param retry the message to move
	 */
	void moveToConflictQueue(ReceiverRetryQueueItem retry);
	
	/**
	 * Saves a {@link JmsMessage} to the database
	 *
	 * @param jmsMessage the message to save
	 */
	void saveJmsMessage(JmsMessage jmsMessage);
	
	/**
	 * Processes a {@link JmsMessage}
	 *
	 * @param jmsMessage the message to process
	 */
	void processJmsMessage(JmsMessage jmsMessage);
	
	/**
	 * Saves a hash object.
	 *
	 * @param hash the hash object to save
	 * @return saved hash object
	 */
	<T extends BaseHashEntity> void saveHash(T hash);
	
	/**
	 * Gets the hash object for an entity.
	 *
	 * @param identifier the unique identifier of the entity
	 * @param hashClass entity hash class
	 * @return the hash object if it exists otherwise null
	 */
	<T extends BaseHashEntity> T getHash(String identifier, Class<T> hashClass);
	
	/**
	 * Marks all items in the synced queue for cached entities as cached, matches only those with an id
	 * that is less than or equal to the specified maximum id.
	 *
	 * @@param maxId maximum id to match
	 */
	@Transactional(transactionManager = MGT_TX_MGR)
	void markAsEvictedFromCache(Long maxId);
	
	/**
	 * Marks all items in the synced queue for indexed entities as re-indexed, matches only those with
	 * an id that is less than or equal to the specified maximum id.
	 *
	 * @param maxId maximum id to match
	 */
	@Transactional(transactionManager = MGT_TX_MGR)
	void markAsReIndexed(Long maxId);
	
}
