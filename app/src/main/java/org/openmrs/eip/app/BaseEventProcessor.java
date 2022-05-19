package org.openmrs.eip.app;

import static org.openmrs.eip.app.SyncConstants.DEFAULT_MSG_PARALLEL_SIZE;
import static org.openmrs.eip.app.SyncConstants.PROP_MSG_PARALLEL_SIZE;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.spi.CamelEvent;
import org.apache.camel.support.EventNotifierSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

public abstract class BaseEventProcessor extends EventNotifierSupport implements Processor {
	
	private static final Logger log = LoggerFactory.getLogger(BaseEventProcessor.class);
	
	protected static final int WAIT_IN_SECONDS = 300;
	
	@Value("${" + PROP_MSG_PARALLEL_SIZE + ":" + DEFAULT_MSG_PARALLEL_SIZE + "}")
	protected int threadCount;
	
	protected ProducerTemplate producerTemplate;
	
	protected ExecutorService executor;
	
	/**
	 * Wait for all the Future instances in the specified list to terminate
	 *
	 * @param futures the list of Futures instance to wait for
	 * @throws Exception
	 */
	public void waitForFutures(List<CompletableFuture<Void>> futures) throws Exception {
		if (log.isDebugEnabled()) {
			log.debug("Waiting for " + futures.size() + " " + getProcessorName() + " processor thread(s) to terminate");
		}
		
		CompletableFuture<Void> allFuture = CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));
		
		allFuture.get(WAIT_IN_SECONDS - 30, TimeUnit.SECONDS);
		
		if (log.isDebugEnabled()) {
			log.debug(futures.size() + " " + getProcessorName() + " processor thread(s) have terminated");
		}
	}
	
	@Override
	public void notify(CamelEvent event) {
		if (event instanceof CamelEvent.CamelContextStoppingEvent) {
			if (executor != null) {
				log.info("Shutting down executor for " + getProcessorName() + " processor threads");
				
				executor.shutdown();
				
				try {
					log.info("Waiting for " + WAIT_IN_SECONDS + " seconds for " + getProcessorName()
					        + " processor threads to terminate");
					
					executor.awaitTermination(WAIT_IN_SECONDS, TimeUnit.SECONDS);
					
					log.info("Done shutting down executor for " + getProcessorName() + " processor threads");
				}
				catch (Exception e) {
					log.error(
					    "An error occurred while waiting for " + getProcessorName() + " processor threads to terminate");
				}
			}
		}
	}
	
	public abstract String getProcessorName();
	
}
