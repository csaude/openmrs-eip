package org.openmrs.eip.app.receiver;

import static org.openmrs.eip.app.receiver.ReceiverConstants.PROP_SYNC_ORDER_BY_ID;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openmrs.eip.app.management.entity.receiver.SyncMessage;
import org.openmrs.eip.app.receiver.processor.SyncMessageProcessor;
import org.openmrs.eip.app.receiver.task.Synchronizer;
import org.openmrs.eip.component.SyncContext;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Pageable;

@RunWith(PowerMockRunner.class)
@PrepareForTest(SyncContext.class)
public class BaseQueueSiteTaskTest {
	
	@Mock
	private SyncMessageProcessor mockProcessor;
	
	@Mock
	private Pageable mockPage;
	
	@Mock
	private Environment mockEnv;
	
	private BaseQueueSiteTask task;
	
	@Before
	public void setup() {
		PowerMockito.mockStatic(SyncContext.class);
		Mockito.when(SyncContext.getBean(SyncMessageProcessor.class)).thenReturn(mockProcessor);
		setInternalState(BaseSiteRunnable.class, "initialized", true);
		setInternalState(BaseSiteRunnable.class, Pageable.class, mockPage);
		Mockito.when(SyncContext.getBean(Environment.class)).thenReturn(mockEnv);
		Mockito.when(mockEnv.getProperty(PROP_SYNC_ORDER_BY_ID, Boolean.class, false)).thenReturn(false);
		task = Mockito.spy(new Synchronizer(null));
	}
	
	@After
	public void tearDown() {
		setInternalState(BaseSiteRunnable.class, "initialized", false);
		setInternalState(BaseSiteRunnable.class, Pageable.class, mockPage);
	}
	
	@Test
	public void doRun_shouldLoadAndSubmitTheItemsToTheProcessor() throws Exception {
		List<SyncMessage> msgs = List.of(new SyncMessage());
		Mockito.doReturn(msgs).when(task).getNextBatch(mockPage);
		
		Assert.assertFalse(task.doRun());
		
		Mockito.verify(mockProcessor).processWork(msgs);
	}
	
	@Test
	public void doRun_shouldSkipIfNoMessagesAreFound() throws Exception {
		List<SyncMessage> msgs = new ArrayList<>();
		Mockito.doReturn(msgs).when(task).getNextBatch(mockPage);
		
		Assert.assertTrue(task.doRun());
		
		Mockito.verifyNoInteractions(mockProcessor);
	}
	
}
