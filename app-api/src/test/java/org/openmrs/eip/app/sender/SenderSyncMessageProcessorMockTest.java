package org.openmrs.eip.app.sender;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openmrs.eip.app.BaseQueueProcessor;
import org.openmrs.eip.app.management.entity.sender.SenderSyncMessage;
import org.openmrs.eip.app.management.repository.SenderSyncMessageRepository;
import org.openmrs.eip.component.SyncOperation;
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
	private SenderSyncMessageRepository mockRepo;
	
	@Mock
	private JmsTemplate mockTemplate;
	
	private boolean originalBatchDisabled;
	
	private String originalSiteId;
	
	@Before
	public void setup() {
		Whitebox.setInternalState(BaseQueueProcessor.class, "initialized", true);
		processor = new SenderSyncMessageProcessor(null, mockTemplate, mockRepo, mockBatchManager);
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
	
	@Test
	public void processWork_shouldSquashEventsForTheSameRowAndSendOnlyTheMostRecent() throws Exception {
		final String uuid1 = "uuid-1";
		final String personTable = "person";
		SenderSyncMessage msg1 = new SenderSyncMessage();
		msg1.setId(1L);
		msg1.setTableName(personTable);
		msg1.setIdentifier(uuid1);
		SenderSyncMessage msg2 = new SenderSyncMessage();
		msg2.setId(2L);
		msg2.setTableName(personTable);
		msg2.setIdentifier(uuid1);
		SenderSyncMessage msg2b = new SenderSyncMessage();
		msg2b.setId(20L);
		msg2b.setTableName("patient");
		msg2b.setIdentifier(uuid1);
		
		final String uuid2 = "uuid-2";
		SenderSyncMessage msg3 = new SenderSyncMessage();
		msg3.setId(3L);
		msg3.setTableName(personTable);
		msg3.setIdentifier(uuid2);
		SenderSyncMessage msg4 = new SenderSyncMessage();
		msg4.setId(4L);
		msg4.setTableName(personTable);
		msg4.setIdentifier(uuid2);
		SenderSyncMessage msg5 = new SenderSyncMessage();
		msg5.setId(5L);
		msg5.setTableName(personTable);
		msg5.setIdentifier(uuid2);
		SenderSyncMessage msg6 = new SenderSyncMessage();
		msg6.setId(6L);
		msg6.setTableName(personTable);
		msg6.setIdentifier(uuid2);
		msg6.setOperation(SyncOperation.d.name());
		
		processor = Mockito.spy(processor);
		List<SenderSyncMessage> events = List.of(msg1, msg2b, msg2, msg3, msg4, msg5, msg6);
		List<SenderSyncMessage> processedMsgs = new ArrayList();
		Mockito.doAnswer(invocation -> {
			List<SenderSyncMessage> eventList = invocation.getArgument(0);
			processedMsgs.addAll(eventList);
			return null;
		}).when(processor).doProcessWork(anyList());
		
		processor.processWork(events);
		
		Mockito.verify(processor).doProcessWork(processedMsgs);
		assertEquals(3, processedMsgs.size());
		assertEquals(20l, processedMsgs.get(0).getId().longValue());
		assertEquals(2l, processedMsgs.get(1).getId().longValue());
		assertEquals(5l, processedMsgs.get(2).getId().longValue());
		Mockito.verify(mockRepo).deleteAllInBatch(List.of(msg1, msg3, msg4));
	}
	
}
