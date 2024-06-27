package org.openmrs.eip.app.sender;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.powermock.reflect.Whitebox.setInternalState;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.openmrs.eip.app.BaseQueueProcessor;
import org.openmrs.eip.app.management.entity.sender.SenderSyncMessage;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.springframework.jms.core.JmsTemplate;

@RunWith(PowerMockRunner.class)
public class SenderSyncMessageProcessorMockTest {
	
	private static final String SITE_ID = "siteId";
	
	private SenderSyncMessageProcessor processor;
	
	@Mock
	private SenderSyncBatchManager mockBatchManager;
	
	@Mock
	private JmsTemplate mockTemplate;
	
	private boolean originalBatchDisabled;
	
	private String originalSiteId;
	
	@Before
	public void setup() {
		Whitebox.setInternalState(BaseQueueProcessor.class, "initialized", true);
		processor = new SenderSyncMessageProcessor(null, mockTemplate, null, mockBatchManager);
		originalSiteId = Whitebox.getInternalState(processor, "senderId");
		originalBatchDisabled = Whitebox.getInternalState(processor, "batchDisabled");
		Whitebox.setInternalState(processor, "senderId", SITE_ID);
	}
	
	@After
	public void tearDown() {
		setInternalState(BaseQueueProcessor.class, "initialized", false);
		Whitebox.setInternalState(processor, "batchDisabled", originalBatchDisabled);
		Whitebox.setInternalState(processor, "senderId", originalSiteId);
	}
	
	@Test
	public void getProcessorName_shouldReturnTheProcessorName() {
		assertEquals("sync msg", processor.getProcessorName());
	}
	
	@Test
	public void getThreadName_shouldReturnTheThreadNameContainingEventDetails() {
		final String table = "visit";
		final String msgUuid = "msg-uuid";
		final String uuid = "som-visit-uuid";
		SenderSyncMessage msg = new SenderSyncMessage();
		msg.setMessageUuid(msgUuid);
		msg.setTableName(table);
		msg.setIdentifier(uuid);
		assertEquals(table + "-" + uuid + "-" + msgUuid, processor.getThreadName(msg));
	}
	
	@Test
	public void getUniqueId_shouldReturnTheUuid() {
		final String visitUuid = "som-visit-uuid";
		SenderSyncMessage msg = new SenderSyncMessage();
		msg.setIdentifier(visitUuid);
		assertEquals(visitUuid, processor.getUniqueId(msg));
	}
	
	@Test
	public void getLogicalType_shouldReturnTheTableName() {
		final String table = "visit";
		SenderSyncMessage msg = new SenderSyncMessage();
		msg.setTableName(table);
		assertEquals(table, processor.getLogicalType(msg));
	}
	
	@Test
	public void getLogicalTypeHierarchy_shouldReturnTheTablesInTheSameHierarchy() {
		assertEquals(1, processor.getLogicalTypeHierarchy("visit").size());
		assertEquals(2, processor.getLogicalTypeHierarchy("person").size());
		assertEquals(3, processor.getLogicalTypeHierarchy("orders").size());
	}
	
	@Test
	public void getQueueName_shouldReturnTheQueueName() {
		assertEquals("sync-msg", processor.getQueueName());
	}
	
	@Test
	public void flush_shouldSubmitTheBatch() {
		processor.flush();
		verify(mockBatchManager).send(true);
	}
	
	@Test
	public void flush_shouldNotSubmitTheBatchIfBatchModeIsDisabled() {
		Whitebox.setInternalState(processor, "batchDisabled", true);
		
		processor.flush();
		
		verifyNoInteractions(mockBatchManager);
	}
	
	@Test
	public void processItem_shouldAddTheItemToTheBatch() {
		SenderSyncMessage msg = new SenderSyncMessage();
		
		processor.processItem(msg);
		
		verify(mockBatchManager).add(msg);
		verifyNoInteractions(mockTemplate);
	}
	
}
