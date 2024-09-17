package org.openmrs.eip.app.receiver.processor;

import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

import org.openmrs.eip.app.AppUtils;
import org.openmrs.eip.app.management.entity.receiver.ConflictQueueItem;
import org.openmrs.eip.app.management.service.ReceiverService;
import org.openmrs.eip.app.receiver.SyncHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Synchronizes the payload of a ConflictQueueItem to the receiver database.
 */
public class ConflictMessageProcessor extends BaseSyncProcessor<ConflictQueueItem> {
	
	private static final Logger LOG = LoggerFactory.getLogger(ConflictMessageProcessor.class);
	
	private ReceiverService service;
	
	private Set<String> propertiesToSync;
	
	public ConflictMessageProcessor(ThreadPoolExecutor executor, ReceiverService service, SyncHelper syncHelper,
	    Set<String> propertiesToSync) {
		super(executor, syncHelper);
		this.service = service;
		this.propertiesToSync = propertiesToSync;
	}
	
	@Override
	public String getProcessorName() {
		return "conflict";
	}
	
	@Override
	public String getQueueName() {
		return "conflict";
	}
	
	@Override
	public String getUniqueId(ConflictQueueItem item) {
		return item.getIdentifier();
	}
	
	@Override
	public String getThreadName(ConflictQueueItem item) {
		return item.getSite().getIdentifier() + "-" + AppUtils.getSimpleName(item.getModelClassName()) + "-"
		        + item.getIdentifier() + "-" + item.getMessageUuid();
	}
	
	@Override
	public String getLogicalType(ConflictQueueItem item) {
		return item.getModelClassName();
	}
	
	@Override
	protected void beforeSync(ConflictQueueItem item) {
		LOG.info("Syncing conflict");
	}
	
	@Override
	protected String getSyncPayload(ConflictQueueItem item) {
		return item.getEntityPayload();
	}
	
	@Override
	protected void afterSync(ConflictQueueItem item) {
		//TODO
		LOG.info("Done syncing conflict");
	}
	
	@Override
	protected void onConflict(ConflictQueueItem item) {
		//TODO
	}
	
	@Override
	protected void onError(ConflictQueueItem item, String exceptionClass, String errorItem) {
		//TODO
	}
	
}
