package org.openmrs.eip.app.receiver.task;

import org.openmrs.eip.app.management.repository.SyncedMessageRepository;
import org.openmrs.eip.app.receiver.CustomHttpClient;
import org.openmrs.eip.app.receiver.HttpRequestProcessor;

import lombok.extern.slf4j.Slf4j;

/**
 * Clears the entire cache and starts an asynchronous rebuild of the search index in the receiver
 * OpenMRS instance.
 */
@Slf4j
public class FullIndexer {
	
	private CustomHttpClient client;
	
	private SyncedMessageRepository syncedMsgRepo;
	
	public FullIndexer(SyncedMessageRepository syncedMsgRepo, CustomHttpClient client) {
		this.syncedMsgRepo = syncedMsgRepo;
		this.client = client;
	}
	
	public void start() {
		if (log.isDebugEnabled()) {
			log.debug("Capturing max id in the synced queue");
		}
		final Long maxId = syncedMsgRepo.getMaxId();
		if (maxId == null) {
			log.info("Skipping full index since synced queue is empty");
			return;
		}
		
		log.info("Running full indexer");
		
		//TODO Skip task if table is empty i.e. if maxId is null;
		if (log.isDebugEnabled()) {
			log.debug("Clearing DB cache in OpenMRS instance");
		}
		client.sendRequest(HttpRequestProcessor.CACHE_RESOURCE, null);
		
		if (log.isDebugEnabled()) {
			log.debug("Starting search index rebuild in OpenMRS instance");
		}
		client.sendRequest(HttpRequestProcessor.INDEX_RESOURCE, "{\"async\":true}");
		
		if (log.isDebugEnabled()) {
			log.debug("Updating rows for entities evicted from the cache");
		}
		syncedMsgRepo.markAsEvictedFromCache(maxId);
		
		if (log.isDebugEnabled()) {
			log.debug("Updating rows for re-indexed entities");
		}
		syncedMsgRepo.markAsReIndexed(maxId);
		
		log.info("Full indexer completed successfully");
	}
	
}
