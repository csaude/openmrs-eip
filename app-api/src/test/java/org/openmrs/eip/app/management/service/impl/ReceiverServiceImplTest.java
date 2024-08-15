package org.openmrs.eip.app.management.service.impl;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openmrs.eip.app.management.repository.SyncedMessageRepository;
import org.openmrs.eip.app.management.service.ReceiverService;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class ReceiverServiceImplTest {
	
	@Mock
	private SyncedMessageRepository mockSyncedMsgRepo;
	
	private ReceiverService service;
	
	@Before
	public void setup() {
		service = new ReceiverServiceImpl(null, mockSyncedMsgRepo, null, null, null, null, null, null, null, null);
	}
	
	@Test
	public void markAsEvictedFromCache_shouldCallTheRepoToUpdateTheDb() {
		final Long maxId = 3L;
		
		service.markAsEvictedFromCache(maxId);
		
		Mockito.verify(mockSyncedMsgRepo).markAsEvictedFromCache(maxId);
	}
	
	@Test
	public void markAsReIndexed_shouldCallTheRepoToUpdateTheDb() {
		final Long maxId = 3L;
		
		service.markAsReIndexed(maxId);
		
		Mockito.verify(mockSyncedMsgRepo).markAsReIndexed(maxId);
	}
	
}
