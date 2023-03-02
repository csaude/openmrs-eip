package org.openmrs.eip.app.receiver;

import java.util.List;

import org.openmrs.eip.app.management.entity.SiteInfo;
import org.openmrs.eip.app.management.entity.receiver.SyncedMessage;
import org.openmrs.eip.app.management.repository.SyncedMessageRepository;
import org.openmrs.eip.component.SyncContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Reads and processes done messages in the synced message queue and moves them to the archives
 * queue
 */
public class SyncedMessageArchiver extends BaseSiteRunnable {
	
	protected static final Logger log = LoggerFactory.getLogger(SyncedMessageArchiver.class);
	
	private SyncedMessageRepository syncedMsgRepo;
	
	private SyncedMessageArchivingProcessor processor;
	
	private Pageable page;
	
	public SyncedMessageArchiver(SiteInfo site) {
		super(site);
		syncedMsgRepo = SyncContext.getBean(SyncedMessageRepository.class);
		processor = SyncContext.getBean(SyncedMessageArchivingProcessor.class);
		//TODO Configure batch size
		page = PageRequest.of(0, 1000);
	}
	
	@Override
	public String getTaskName() {
		return "msg archiver task";
	}
	
	@Override
	public boolean doRun() throws Exception {
		//TODO Check for existence of sync messages before running
		if (log.isTraceEnabled()) {
			log.trace("Fetching next batch of " + page.getPageSize() + " messages to archive for site: " + site);
		}
		
		List<SyncedMessage> messages = syncedMsgRepo.getBatchOfMessagesForArchiving(site, page);
		
		if (messages.isEmpty()) {
			if (log.isTraceEnabled()) {
				log.trace("No messages for archiving found for site: " + site);
			}
			
			return true;
		}
		
		if (log.isDebugEnabled()) {
			log.debug("Archiving " + messages.size() + " message(s) for site: " + site);
		}
		
		processor.processWork(messages);
		
		return false;
	}
	
}
