package org.openmrs.eip.app.receiver.task;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openmrs.eip.app.management.repository.SyncedMessageRepository;
import org.openmrs.eip.app.receiver.CustomHttpClient;
import org.openmrs.eip.app.receiver.HttpRequestProcessor;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class FullIndexerTest {
	
	@Mock
	private CustomHttpClient mockClient;
	
	@Mock
	private SyncedMessageRepository mockRepo;
	
	private FullIndexer indexer;
	
	@Before
	public void setup() {
		indexer = new FullIndexer(mockRepo, mockClient);
	}
	
	@Test
	public void start_shouldClearCacheAndRebuildSearchIndex() {
		final long maxId = 10;
		Mockito.when(mockRepo.getMaxId()).thenReturn(maxId);
		
		indexer.start();
		
		Mockito.verify(mockClient).sendRequest(HttpRequestProcessor.CACHE_RESOURCE, null);
		Mockito.verify(mockClient).sendRequest(HttpRequestProcessor.INDEX_RESOURCE, "{\"async\":true}");
		Mockito.verify(mockRepo).markAsEvictedFromCache(maxId);
		Mockito.verify(mockRepo).markAsReIndexed(maxId);
	}
	
	@Test
	public void start_shouldSkipExecutionIfTheSyncedQueueIsEmpty() {
		Mockito.when(mockRepo.getMaxId()).thenReturn(null);
		
		indexer.start();
		
		Mockito.verifyNoInteractions(mockClient);
		Mockito.verify(mockRepo, Mockito.never()).markAsEvictedFromCache(ArgumentMatchers.any());
		Mockito.verify(mockRepo, Mockito.never()).markAsReIndexed(ArgumentMatchers.any());
	}
	
}
