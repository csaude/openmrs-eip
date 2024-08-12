package org.openmrs.eip.app.receiver.task;

import org.openmrs.eip.app.receiver.CustomHttpClient;
import org.openmrs.eip.app.receiver.HttpRequestProcessor;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Clears the entire cache and starts an asynchronous rebuild of the search index in the receiver
 * OpenMRS instance.
 */
@Slf4j
@Component
public class FullIndexer {
	
	private CustomHttpClient client;
	
	public FullIndexer(CustomHttpClient client) {
		this.client = client;
	}
	
	public void start() {
		if (log.isDebugEnabled()) {
			log.debug("Clearing DB cache in OpenMRS instance");
		}
		
		client.sendRequest(HttpRequestProcessor.CACHE_RESOURCE, null);
		
		if (log.isDebugEnabled()) {
			log.debug("Starting search index rebuild in OpenMRS instance");
		}
		
		client.sendRequest(HttpRequestProcessor.INDEX_RESOURCE, "{\"async\":true}");
	}
	
}
