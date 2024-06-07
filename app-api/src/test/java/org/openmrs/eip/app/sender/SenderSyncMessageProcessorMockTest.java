package org.openmrs.eip.app.sender;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openmrs.eip.app.AppUtils;
import org.openmrs.eip.app.BaseQueueProcessor;
import org.openmrs.eip.app.management.entity.sender.SenderSyncMessage;
import org.openmrs.eip.component.model.SyncMetadata;
import org.openmrs.eip.component.model.SyncModel;
import org.openmrs.eip.component.utils.JsonUtils;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.springframework.jms.core.JmsTemplate;

@RunWith(PowerMockRunner.class)
@PrepareForTest(JsonUtils.class)
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
		PowerMockito.mockStatic(JsonUtils.class);
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
		final String content = "{}";
		SenderSyncMessage msg = new SenderSyncMessage();
		msg.setData(content);
		SyncModel model = Mockito.mock(SyncModel.class);
		SyncMetadata metadata = Mockito.mock(SyncMetadata.class);
		Mockito.when(model.getMetadata()).thenReturn(metadata);
		Mockito.when(JsonUtils.unmarshalSyncModel(content)).thenReturn(model);
		long timestamp = System.currentTimeMillis();
		
		processor.processItem(msg);
		
		verify(metadata).setSourceIdentifier(SITE_ID);
		verify(metadata).setSyncVersion(AppUtils.getVersion());
		ArgumentCaptor<LocalDateTime> argCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
		verify(metadata).setDateSent(argCaptor.capture());
		long dateMillis = argCaptor.getValue().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
		assertTrue(dateMillis == timestamp || dateMillis > timestamp);
		verify(mockBatchManager).add(msg);
		verifyNoInteractions(mockTemplate);
	}
	
}
