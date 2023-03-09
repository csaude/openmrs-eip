package org.openmrs.eip.app.receiver;

import static java.util.Collections.emptyList;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openmrs.eip.app.management.entity.SiteInfo;
import org.openmrs.eip.app.management.entity.receiver.SyncedMessage;
import org.openmrs.eip.app.management.repository.SyncedMessageRepository;
import org.openmrs.eip.component.SyncContext;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.springframework.data.domain.Pageable;

@RunWith(PowerMockRunner.class)
@PrepareForTest(SyncContext.class)
public class SyncedMessageItemizerTest {
	
	@Mock
	private SiteInfo mockSite;
	
	@Mock
	private SyncedMessageRepository mockRepo;
	
	@Mock
	private SyncedMessageItemizingProcessor mockProcessor;
	
	@Mock
	private Pageable mockPage;
	
	private SyncedMessageItemizer itemizer;
	
	@Before
	public void setup() {
		PowerMockito.mockStatic(SyncContext.class);
		setInternalState(BaseSiteRunnable.class, "initialized", true);
		setInternalState(BaseSiteRunnable.class, "page", mockPage);
		itemizer = new SyncedMessageItemizer(mockSite);
		Whitebox.setInternalState(itemizer, SyncedMessageRepository.class, mockRepo);
		Whitebox.setInternalState(itemizer, SyncedMessageItemizingProcessor.class, mockProcessor);
	}
	
	@After
	public void tearDown() {
		setInternalState(BaseSiteRunnable.class, "initialized", false);
		setInternalState(BaseSiteRunnable.class, "page", (Object) null);
	}
	
	@Test
	public void doRun_shouldLoadUnItemizedMessagesAndInvokeTheProcessor() throws Exception {
		List<SyncedMessage> msgs = Collections.singletonList(new SyncedMessage());
		when(mockRepo.getBatchOfMessagesForItemizing(mockSite, mockPage)).thenReturn(msgs);
		
		Assert.assertFalse(itemizer.doRun());
		
		Mockito.verify(mockProcessor).processWork(msgs);
	}
	
	@Test
	public void doRun_shouldNotInvokeTheProcessorIfThereAreNoUnItemizedMessages() throws Exception {
		when(mockRepo.getBatchOfMessagesForItemizing(mockSite, mockPage)).thenReturn(emptyList());
		
		Assert.assertTrue(itemizer.doRun());
		
		Mockito.verify(mockProcessor, never()).processWork(anyList());
	}
	
}
