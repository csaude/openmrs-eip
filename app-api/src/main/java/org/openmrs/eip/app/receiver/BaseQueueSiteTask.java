package org.openmrs.eip.app.receiver;

import static org.openmrs.eip.app.SyncConstants.PROP_TASK_CONTINUOUS;

import java.util.List;

import org.openmrs.eip.app.BaseQueueProcessor;
import org.openmrs.eip.app.management.entity.AbstractEntity;
import org.openmrs.eip.app.management.entity.receiver.SiteInfo;
import org.openmrs.eip.component.SyncContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Pageable;

/**
 * Base class for tasks that read items from a database queue table for a single site and forward
 * them to a processor for processing
 * 
 * @see BaseQueueProcessor
 * @param <T> the queue entity type
 * @param <P> the processor type
 */
public abstract class BaseQueueSiteTask<T extends AbstractEntity, P extends BaseQueueProcessor<T>> extends BaseSiteRunnable {
	
	protected static final Logger log = LoggerFactory.getLogger(BaseQueueSiteTask.class);
	
	private final P processor;
	
	private Boolean continuous;
	
	public BaseQueueSiteTask(SiteInfo site, P processor) {
		super(site);
		this.processor = processor;
	}
	
	@Override
	public boolean doRun() throws Exception {
		if (log.isTraceEnabled()) {
			log.trace("Fetching next batch of " + page.getPageSize() + " items to process for site: " + site);
		}
		
		List<T> items = getNextBatch(page);
		if (items == null || items.isEmpty()) {
			if (log.isTraceEnabled()) {
				log.trace("No items found");
			}
			
			return true;
		}
		
		if (log.isDebugEnabled()) {
			log.debug("Submitting to processor " + items.size() + " items(s)");
		}
		
		processor.processWork(items);
		
		if (continuous == null) {
			continuous = SyncContext.getBean(Environment.class).getProperty(PROP_TASK_CONTINUOUS, Boolean.class, false);
		}
		
		if (!continuous) {
			return true;
		}
		
		return stopAfterEachBatch();
	}
	
	public boolean stopAfterEachBatch() {
		return true;
	}
	
	/**
	 * Gets the next batch of items to process
	 * 
	 * @param page {@link Pageable} object
	 * @return List of items
	 */
	public abstract List<T> getNextBatch(Pageable page);
	
}
