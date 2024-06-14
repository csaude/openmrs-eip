package org.openmrs.eip.app.receiver;

import static java.util.Collections.synchronizedList;
import static org.openmrs.eip.app.SyncConstants.THREAD_THRESHOLD_MULTIPLIER;
import static org.openmrs.eip.app.receiver.ReceiverConstants.DEFAULT_TASK_BATCH_SIZE;
import static org.openmrs.eip.app.receiver.ReceiverConstants.EX_PROP_ERR_MSG;
import static org.openmrs.eip.app.receiver.ReceiverConstants.EX_PROP_ERR_TYPE;
import static org.openmrs.eip.app.receiver.ReceiverConstants.EX_PROP_FOUND_CONFLICT;
import static org.openmrs.eip.app.receiver.ReceiverConstants.EX_PROP_MSG_PROCESSED;
import static org.openmrs.eip.app.receiver.ReceiverConstants.PROP_SYNC_TASK_BATCH_SIZE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.ExchangeBuilder;
import org.openmrs.eip.app.AppUtils;
import org.openmrs.eip.app.management.entity.receiver.SiteInfo;
import org.openmrs.eip.app.management.entity.receiver.SyncMessage;
import org.openmrs.eip.app.management.entity.receiver.SyncedMessage.SyncOutcome;
import org.openmrs.eip.app.management.repository.SyncMessageRepository;
import org.openmrs.eip.app.management.service.ReceiverService;
import org.openmrs.eip.component.SyncContext;
import org.openmrs.eip.component.camel.utils.CamelUtils;
import org.openmrs.eip.component.exception.EIPException;
import org.openmrs.eip.component.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * An instance of this class consumes sync messages for a single site and forwards them to the
 * message processor route
 */
public class SiteMessageConsumer implements Runnable {
	
	protected static final Logger log = LoggerFactory.getLogger(SiteMessageConsumer.class);
	
	private static boolean initialized = false;
	
	private static int taskThreshold;
	
	//Used to temporarily store the entities being processed at any point in time across all sites
	private static Set<String> PROCESSING_MSG_QUEUE;
	
	private SiteInfo site;
	
	private boolean errorEncountered = false;
	
	private ProducerTemplate producerTemplate;
	
	private ThreadPoolExecutor executor;
	
	private String messageProcessorUri;
	
	private ReceiverService service;
	
	private SyncMessageRepository syncMsgRepo;
	
	private static Pageable page;
	
	private static boolean orderById;
	
	/**
	 * @param messageProcessorUri the camel endpoint URI to call to process a sync message
	 * @param site sync messages from this site will be consumed by this instance
	 * @param executor {@link ExecutorService} instance to messages in parallel
	 */
	public SiteMessageConsumer(String messageProcessorUri, SiteInfo site, ThreadPoolExecutor executor) {
		this.messageProcessorUri = messageProcessorUri;
		this.site = site;
		this.executor = executor;
		producerTemplate = SyncContext.getBean(ProducerTemplate.class);
		service = SyncContext.getBean(ReceiverService.class);
		syncMsgRepo = SyncContext.getBean(SyncMessageRepository.class);
		initIfNecessary();
	}
	
	protected void initIfNecessary() {
		synchronized (SiteMessageConsumer.class) {
			if (!initialized) {
				Environment e = SyncContext.getBean(Environment.class);
				int pageSize = e.getProperty(PROP_SYNC_TASK_BATCH_SIZE, Integer.class, DEFAULT_TASK_BATCH_SIZE);
				page = PageRequest.of(0, pageSize);
				orderById = e.getProperty(ReceiverConstants.PROP_SYNC_ORDER_BY_ID, Boolean.class, false);
				//This ensures there will only be a limited number of queued items for each thread
				taskThreshold = executor.getMaximumPoolSize() * THREAD_THRESHOLD_MULTIPLIER;
				PROCESSING_MSG_QUEUE = Collections.synchronizedSet(new HashSet<>(executor.getMaximumPoolSize()));
				initialized = true;
			}
		}
	}
	
	@Override
	public void run() {
		if (AppUtils.isShuttingDown()) {
			if (log.isDebugEnabled()) {
				log.debug("Sync message consumer skipping execution because the application is stopping");
			}
			
			return;
		}
		
		if (log.isTraceEnabled()) {
			log.trace("Starting message consumer thread for site -> " + site);
		}
		
		do {
			Thread.currentThread().setName(site.getIdentifier());
			
			if (log.isTraceEnabled()) {
				log.trace("Fetching next batch of messages to sync for site: " + site);
			}
			
			try {
				List<SyncMessage> syncMessages;
				if (orderById) {
					syncMessages = syncMsgRepo.getSyncMessageBySite(site, page);
				} else {
					syncMessages = syncMsgRepo.getSyncMessageBySiteOrderByDateCreated(site, page);
				}
				
				if (syncMessages.isEmpty()) {
					if (log.isTraceEnabled()) {
						log.trace("No sync messages found from site: " + site);
					}
					
					break;
				}
				
				processMessages(syncMessages);
				
			}
			catch (Throwable t) {
				if (!AppUtils.isShuttingDown()) {
					errorEncountered = true;
					log.error("Message consumer thread for site: " + site + " encountered an error", t);
					break;
				}
			}
			
		} while (!AppUtils.isShuttingDown() && !errorEncountered);
		
		if (!errorEncountered) {
			if (log.isTraceEnabled()) {
				log.trace("Sync message consumer for site: " + site + " has completed");
			}
		}
		
	}
	
	protected void processMessages(List<SyncMessage> syncMessages) throws Exception {
		if (log.isTraceEnabled()) {
			log.trace("Processing " + syncMessages.size() + " message(s) from site: " + site);
		}
		
		List<String> typeAndIdentifier = synchronizedList(new ArrayList(taskThreshold));
		List<CompletableFuture<Void>> futures = synchronizedList(new ArrayList(taskThreshold));
		
		for (SyncMessage msg : syncMessages) {
			if (AppUtils.isShuttingDown()) {
				log.info("Sync message consumer for site: " + site + " has detected a stop signal");
				break;
			}
			
			//Only process events if they don't belong to the same entity to avoid false conflicts and unique key
			//constraint violations, this applies to subclasses
			if (typeAndIdentifier.contains(msg.getModelClassName() + "#" + msg.getIdentifier())) {
				final String originalThreadName = Thread.currentThread().getName();
				try {
					setThreadName(msg);
					//TODO Record as skipped and go to next
					if (log.isDebugEnabled()) {
						log.debug("Postponed sync of {} because of an earlier unprocessed sync message for the entity", msg);
					}
				}
				finally {
					Thread.currentThread().setName(originalThreadName);
				}
				
				continue;
			}
			
			for (String modelClass : Utils.getListOfModelClassHierarchy(msg.getModelClassName())) {
				typeAndIdentifier.add(modelClass + "#" + msg.getIdentifier());
			}
			
			futures.add(CompletableFuture.runAsync(() -> {
				final String originalThreadName = Thread.currentThread().getName();
				try {
					setThreadName(msg);
					processMessage(msg);
				}
				finally {
					//Maybe we should also remove the entity from typeAndIdentifier list, may be not because there can  
					//be 2 snapshot events for the same entity i.e. for tables with a hierarchy
					Thread.currentThread().setName(originalThreadName);
				}
			}, executor));
			
			if (futures.size() >= taskThreshold) {
				waitForFutures(futures);
				futures.clear();
			}
			
		}
		
		if (futures.size() > 0) {
			waitForFutures(futures);
		}
	}
	
	/**
	 * Processes the specified sync message
	 *
	 * @param msg the sync message to process
	 */
	public void processMessage(SyncMessage msg) {
		Exchange exchange = ExchangeBuilder.anExchange(producerTemplate.getCamelContext()).withBody(msg).build();
		//TODO Move this logic that ensures no threads process events for the same entity to message-processor route
		String modelClass = msg.getModelClassName();
		if (ReceiverUtils.isSubclass(modelClass)) {
			modelClass = ReceiverUtils.getParentModelClassName(modelClass);
		}
		
		final String uniqueId = modelClass + "#" + msg.getIdentifier();
		boolean removeId = false;
		try {
			//We could ignore inserts because we don't expect any events for the entity from other sites yet BUT in a 
			//very rare case, this could be a message we previously processed but was never removed from the queue 
			//and it is just getting re-processed so the entity could have been already been imported by other sites 
			//and then we actually have events for the same entity from other sites
			if (!PROCESSING_MSG_QUEUE.add(uniqueId)) {
				if (log.isDebugEnabled()) {
					log.debug("Postponed sync of {} because another site is processing an event for the same entity", msg);
				}
				
				return;
			}
			
			removeId = true;
			CamelUtils.send(messageProcessorUri, exchange);
		}
		finally {
			if (removeId) {
				PROCESSING_MSG_QUEUE.remove(uniqueId);
			}
		}
		
		boolean foundConflict = exchange.getProperty(EX_PROP_FOUND_CONFLICT, false, Boolean.class);
		String errorType = exchange.getProperty(EX_PROP_ERR_TYPE, String.class);
		String errorMsg = exchange.getProperty(EX_PROP_ERR_MSG, String.class);
		boolean msgProcessed = exchange.getProperty(EX_PROP_MSG_PROCESSED, false, Boolean.class);
		
		if (msgProcessed) {
			service.moveToSyncedQueue(msg, SyncOutcome.SUCCESS);
		} else if (foundConflict) {
			service.processConflictedSyncItem(msg);
		} else if (errorType != null) {
			service.processFailedSyncItem(msg, errorType, errorMsg);
		} else {
			throw new EIPException("Something went wrong while processing sync message -> " + msg);
		}
		
		log.info("Done processing message");
	}
	
	/**
	 * Wait for all the Future instances in the specified list to terminate
	 * 
	 * @param futures the list of Futures instance to wait for
	 * @throws Exception
	 */
	public void waitForFutures(List<CompletableFuture<Void>> futures) throws Exception {
		if (log.isTraceEnabled()) {
			log.trace("Waiting for " + futures.size() + " sync message processor thread(s) to terminate");
		}
		
		CompletableFuture<Void> allFuture = CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));
		
		allFuture.get();
		
		if (log.isTraceEnabled()) {
			log.trace(futures.size() + " sync message processor thread(s) have terminated");
		}
	}
	
	private void setThreadName(SyncMessage msg) {
		Thread.currentThread().setName(Thread.currentThread().getName() + ":" + getThreadName(msg));
	}
	
	protected String getThreadName(SyncMessage msg) {
		return msg.getSite().getIdentifier() + "-" + AppUtils.getSimpleName(msg.getModelClassName()) + "-"
		        + msg.getIdentifier() + "-" + msg.getMessageUuid();
	}
	
}
