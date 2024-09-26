package org.openmrs.eip.app.sender.reconcile;

import static org.junit.Assert.assertEquals;
import static org.powermock.reflect.Whitebox.setInternalState;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openmrs.eip.app.BaseQueueProcessor;
import org.openmrs.eip.app.management.entity.sender.SenderReconcileMessage;
import org.openmrs.eip.app.management.repository.SenderReconcileMsgRepository;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.springframework.jms.core.JmsTemplate;

@RunWith(PowerMockRunner.class)
public class SenderReconcileMsgProcessorTest {
	
	private static final String SITE_ID = "test_site";
	
	private SenderReconcileMsgProcessor processor;
	
	@Mock
	private SenderReconcileMsgRepository mockRepo;
	
	@Mock
	private JmsTemplate mockTemplate;
	
	@Before
	public void setup() {
		Whitebox.setInternalState(BaseQueueProcessor.class, "initialized", true);
		processor = new SenderReconcileMsgProcessor(null, mockRepo, mockTemplate);
		Whitebox.setInternalState(processor, "siteId", SITE_ID);
	}
	
	@After
	public void tearDown() {
		setInternalState(BaseQueueProcessor.class, "initialized", false);
	}
	
	@Test
	public void getThreadName_shouldReturnTheThreadNameContainingTheMessageId() {
		final Long id = 2L;
		SenderReconcileMessage m = new SenderReconcileMessage();
		m.setId(id);
		assertEquals(id.toString(), processor.getThreadName(m));
	}
	
	@Test
	public void processItem_shouldShouldSendAndDeleteMessage() {
		final byte[] body = "test".getBytes();
		SenderReconcileMessage m = new SenderReconcileMessage();
		m.setBody(body);
		
		processor.processItem(m);
		
		ArgumentCaptor<ReconcileResponseCreator> argCaptor = ArgumentCaptor.forClass(ReconcileResponseCreator.class);
		Mockito.verify(mockTemplate).send(argCaptor.capture());
		Assert.assertArrayEquals(body, argCaptor.getValue().getBody());
		Assert.assertEquals(SITE_ID, argCaptor.getValue().getSiteId());
		Mockito.verify(mockRepo).delete(m);
	}
	
}
