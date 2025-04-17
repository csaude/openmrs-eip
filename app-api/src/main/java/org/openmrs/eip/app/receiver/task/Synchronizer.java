package org.openmrs.eip.app.receiver.task;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.openmrs.eip.app.management.entity.receiver.SiteInfo;
import org.openmrs.eip.app.management.entity.receiver.SyncMessage;
import org.openmrs.eip.app.management.repository.SyncMessageRepository;
import org.openmrs.eip.app.receiver.BaseQueueSiteTask;
import org.openmrs.eip.app.receiver.ReceiverConstants;
import org.openmrs.eip.app.receiver.processor.SyncMessageProcessor;
import org.openmrs.eip.component.SyncContext;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Pageable;

/**
 * Reads a batch of messages in the sync queue and forwards them to the
 * {@link SyncMessageProcessor}.
 */
public class Synchronizer extends BaseQueueSiteTask<SyncMessage, SyncMessageProcessor> {
	
	private Boolean orderById;
	
	private SyncMessageRepository repo;
	
	public Synchronizer(SiteInfo site) {
		super(site, SyncContext.getBean(SyncMessageProcessor.class));
		repo = SyncContext.getBean(SyncMessageRepository.class);
		Environment e = SyncContext.getBean(Environment.class);
		orderById = e.getProperty(ReceiverConstants.PROP_SYNC_ORDER_BY_ID, Boolean.class, false);
	}
	
	@Override
	public String getTaskName() {
		return "sync task";
	}
	
	@Override
	public List<SyncMessage> getNextBatch(Pageable page) {
		if (orderById) {
			return tryToSquashItems(repo.getSyncMessageBySite(site, page));
		} else {
			return tryToSquashItems(repo.getSyncMessageBySiteOrderByDateReceived(site, page));
		}
	}
	
	private List<SyncMessage> tryToSquashItems(List<SyncMessage> items) {
		if (items == null || items.isEmpty()) {
			return null;
		} else {
			Map<String, SyncMessage> uniqueMap = new LinkedHashMap<>();
			
			for (SyncMessage item : items) {
				uniqueMap.put(item.getIdentifier(), item);
			}
			
			return new ArrayList<>(uniqueMap.values());
		}
	}
	
	@Override
	public boolean stopAfterEachBatch() {
		return false;
	}
	
}
