package org.openmrs.eip.app.receiver.task;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openmrs.eip.app.management.repository.SyncedMessageRepository;
import org.openmrs.eip.app.management.service.ReceiverService;
import org.openmrs.eip.app.receiver.CustomHttpClient;
import org.openmrs.eip.app.receiver.HttpRequestProcessor;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class FullIndexerTest {
	
	@Mock
	private ReceiverService mockService;
	
	@Mock
	private SyncedMessageRepository mockRepo;
	
	@Mock
	private CustomHttpClient mockClient;
	
	private FullIndexer indexer;
	
	@Before
	public void setup() {
		indexer = new FullIndexer(mockService, mockRepo, mockClient);
	}
	
	@Test
	public void start_shouldClearCacheAndRebuildSearchIndex() {
		final long maxId = 10;
		Mockito.when(mockRepo.getMaxId()).thenReturn(maxId);
		
		indexer.start();
		
		Mockito.verify(mockClient).sendRequest(HttpRequestProcessor.CACHE_RESOURCE, "");
		Mockito.verify(mockClient).sendRequest(HttpRequestProcessor.INDEX_RESOURCE, "{\"async\":true}");
		Mockito.verify(mockService).markAsEvictedFromCache(maxId);
		Mockito.verify(mockService).markAsReIndexed(maxId);
	}
	
	@Test
	public void start_shouldSkipExecutionIfTheSyncedQueueIsEmpty() {
		Mockito.when(mockRepo.getMaxId()).thenReturn(null);
		
		indexer.start();
		
		Mockito.verifyNoInteractions(mockClient);
		Mockito.verify(mockService, Mockito.never()).markAsEvictedFromCache(ArgumentMatchers.any());
		Mockito.verify(mockService, Mockito.never()).markAsReIndexed(ArgumentMatchers.any());
	}
	
}
