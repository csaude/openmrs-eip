package org.openmrs.eip.app.receiver.reconcile;

import org.openmrs.eip.app.management.service.ReceiverReconcileService;
import org.openmrs.eip.app.receiver.task.FullIndexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

public class ReceiverScheduler {
	
	private static final Logger LOG = LoggerFactory.getLogger(ReceiverScheduler.class);
	
	private ReceiverReconcileService service;
	
	private FullIndexer indexer;
	
	public ReceiverScheduler(ReceiverReconcileService service, FullIndexer indexer) {
		this.service = service;
		this.indexer = indexer;
	}
	
	@Scheduled(cron = "${reconcile.schedule.cron:-}")
	public void reconcile() {
		LOG.info("Adding new reconciliation");
		service.addNewReconciliation();
	}
	
	@Scheduled(cron = "${full.indexer.schedule.cron:-}")
	public void index() {
		indexer.start();
	}
	
}
