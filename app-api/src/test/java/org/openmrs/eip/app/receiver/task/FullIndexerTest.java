package org.openmrs.eip.app.receiver.task;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openmrs.eip.app.receiver.CustomHttpClient;
import org.openmrs.eip.app.receiver.HttpRequestProcessor;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class FullIndexerTest {
	
	@Mock
	private CustomHttpClient mockClient;
	
	@Test
	public void start_shouldClearCacheAndRebuildSearchIndex() {
		new FullIndexer(mockClient).start();
		Mockito.verify(mockClient).sendRequest(HttpRequestProcessor.CACHE_RESOURCE, null);
		Mockito.verify(mockClient).sendRequest(HttpRequestProcessor.INDEX_RESOURCE, "{\"async\":true}");
	}
	
}
