package org.openmrs.eip.app.receiver;

import org.openmrs.eip.app.receiver.task.Synchronizer;

import lombok.Getter;

/**
 * An enumeration of the child site task types
 */
public enum SiteChildTaskType {
	
	SYNCHRONIZER(Synchronizer.class),
	
	CACHE_EVICTOR(CacheEvictor.class),
	
	SEARCH_INDEX_UPDATER(SearchIndexUpdater.class),
	
	RESPONSE_SENDER(SyncResponseSender.class),
	
	DELETER(SyncedMessageDeleter.class);
	
	@Getter
	private final Class<? extends Runnable> taskClass;
	
	SiteChildTaskType(Class<? extends Runnable> taskClass) {
		this.taskClass = taskClass;
	}
	
}
