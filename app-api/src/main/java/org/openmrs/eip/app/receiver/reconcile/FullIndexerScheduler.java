package org.openmrs.eip.app.receiver.reconcile;

import org.openmrs.eip.app.receiver.ReceiverConstants;
import org.openmrs.eip.app.receiver.task.FullIndexer;
import org.springframework.scheduling.annotation.Scheduled;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FullIndexerScheduler {
	
	private FullIndexer indexer;
	
	public FullIndexerScheduler(FullIndexer indexer) {
		this.indexer = indexer;
	}
	
	@Scheduled(cron = "${" + ReceiverConstants.PROP_FULL_INDEXER_CRON + ":-}")
	public void execute() {
		indexer.start();
	}
	
}
