package org.openmrs.eip.app.receiver.processor;

import java.util.Set;

import org.openmrs.eip.app.AppUtils;
import org.openmrs.eip.app.management.entity.receiver.ConflictQueueItem;
import org.openmrs.eip.app.management.service.ConflictService;
import org.openmrs.eip.app.receiver.SyncHelper;
import org.openmrs.eip.component.SyncContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Synchronizes the payload of a ConflictQueueItem to the receiver database.
 */
public class ConflictMessageProcessor extends BaseSyncProcessor<ConflictQueueItem> {
	
	private static final Logger LOG = LoggerFactory.getLogger(ConflictMessageProcessor.class);
	
	private Set<String> propertiesToSync;
	
	public ConflictMessageProcessor(SyncHelper syncHelper, Set<String> propertiesToSync) {
		super(null, syncHelper);
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
	protected void sync(ConflictQueueItem item) throws Throwable {
		SyncContext.getBean(ConflictService.class).resolveWithMerge(item, propertiesToSync);
	}
	
	@Override
	protected void afterSync(ConflictQueueItem item) {
		LOG.info("Done syncing conflict");
	}
	
	@Override
	protected void onConflict(ConflictQueueItem item) {
		//TODO What should we do in case a new conflict is encountered, is it even possible anyways?
		LOG.error("Encountered another conflict while resolving a conflict");
	}
	
	@Override
	protected void onError(ConflictQueueItem item, String exceptionClass, String errorItem) {
		LOG.error("Failed to sync resolved conflict item with uuid: {}", item.getMessageUuid());
	}
	
}
