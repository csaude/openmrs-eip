package org.openmrs.eip.app.sender;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.openmrs.eip.app.management.entity.receiver.JmsMessage.MessageType.SYNC;
import static org.openmrs.eip.app.sender.SenderConstants.PROP_ACTIVEMQ_ENDPOINT;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openmrs.eip.app.AppUtils;
import org.openmrs.eip.app.SyncConstants;
import org.openmrs.eip.app.management.entity.sender.SenderSyncMessage;
import org.openmrs.eip.component.SyncContext;
import org.openmrs.eip.component.exception.EIPException;
import org.openmrs.eip.component.model.SyncModel;
import org.openmrs.eip.component.repository.PersonRepository;
import org.openmrs.eip.component.utils.JsonUtils;
import org.openmrs.eip.component.utils.Utils;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.core.env.Environment;

import jakarta.jms.BytesMessage;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.MessageProducer;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import jakarta.jms.StreamMessage;

@RunWith(PowerMockRunner.class)
@PrepareForTest(SyncContext.class)
public class SenderUtilsTest {
	
	private static final String QUEUE_NAME = "test";
	
	private static final String SITE_ID = "testSite";
	
	@Mock
	private Environment mockEnv;
	
	@Mock
	private ConnectionFactory mockConnFactory;
	
	@Mock
	private Connection mockConnection;
	
	@Mock
	private Session mockSession;
	
	@Mock
	private Queue mockQueue;
	
	@Mock
	private MessageProducer mockMsgProducer;
	
	@Mock
	private BytesMessage mockBytesMsg;
	
	@Before
	public void setup() {
		PowerMockito.mockStatic(SyncContext.class);
		when(SyncContext.getBean(Environment.class)).thenReturn(mockEnv);
		when(mockEnv.getProperty(PROP_ACTIVEMQ_ENDPOINT)).thenReturn("activemq:" + QUEUE_NAME);
	}
	
	@Test
	public void mask_shouldFailForAValueOfATypeThatIsNotSupported() {
		SyncModel model = new SyncModel();
		Exception thrown = Assert.assertThrows(EIPException.class, () -> SenderUtils.mask(model));
		Assert.assertEquals("Don't know how mask a value of type: " + model.getClass(), thrown.getMessage());
	}
	
	@Test
	public void mask_shouldReturnNullForANullValue() {
		Assert.assertNull(SenderUtils.mask(null));
	}
	
	@Test
	public void mask_shouldReturnTheCorrectMaskValueForAString() {
		Assert.assertEquals(SenderConstants.MASK, SenderUtils.mask("test"));
	}
	
	@Test
	public void getQueueName_shouldReturnTheNameOfTheJmsQueue() {
		assertEquals(QUEUE_NAME, SenderUtils.getQueueName());
	}
	
	@Test
	public void getUuidFromParentTable_shouldLookUpThePatientUuid() {
		final String expectedUuid = "test-uuid";
		final String table = "patient";
		final Long patientId = 2L;
		PersonRepository mockRepo = Mockito.mock(PersonRepository.class);
		when(SyncContext.getBean(PersonRepository.class)).thenReturn(mockRepo);
		when(mockRepo.getUuid(patientId)).thenReturn(expectedUuid);
		assertEquals(expectedUuid, SenderUtils.getUuidFromParentTable(table, patientId));
	}
	
	@Test
	public void sendBatch_shouldSendTheItemsInTheBatchBuffer() throws Exception {
		when(mockConnFactory.createConnection()).thenReturn(mockConnection);
		when(mockConnection.createSession()).thenReturn(mockSession);
		when(mockSession.createQueue(QUEUE_NAME)).thenReturn(mockQueue);
		when(mockSession.createProducer(mockQueue)).thenReturn(mockMsgProducer);
		when(mockSession.createBytesMessage()).thenReturn(mockBytesMsg);
		final Long id = 3L;
		SenderSyncMessage msg = new SenderSyncMessage();
		msg.setId(id);
		byte[] expectedSentBytes = JsonUtils.marshalToBytes(List.of(msg));
		
		SenderUtils.sendBatch(mockConnFactory, SITE_ID, List.of(msg), SyncConstants.DEFAULT_LARGE_MSG_SIZE);
		
		verify(mockBytesMsg).setIntProperty(SyncConstants.JMS_HEADER_BATCH_SIZE, 1);
		verify(mockBytesMsg).setStringProperty(eq(SyncConstants.JMS_HEADER_MSG_ID), anyString());
		verify(mockBytesMsg).setStringProperty(SyncConstants.JMS_HEADER_VERSION, AppUtils.getVersion());
		verify(mockBytesMsg).setStringProperty(SyncConstants.JMS_HEADER_SITE, SITE_ID);
		verify(mockBytesMsg).setStringProperty(SyncConstants.JMS_HEADER_TYPE, SYNC.name());
		ArgumentCaptor<byte[]> bytesCaptor = ArgumentCaptor.forClass(byte[].class);
		verify(mockBytesMsg).writeBytes(bytesCaptor.capture());
		assertTrue(Arrays.equals(expectedSentBytes, bytesCaptor.getValue()));
		verify(mockMsgProducer).send(mockBytesMsg);
		verify(mockMsgProducer).send(mockBytesMsg);
	}
	
	@Test
	public void sendBatch_shouldCompressAndSendALargeMessage() throws Exception {
		when(mockConnFactory.createConnection()).thenReturn(mockConnection);
		when(mockConnection.createSession()).thenReturn(mockSession);
		when(mockSession.createQueue(QUEUE_NAME)).thenReturn(mockQueue);
		when(mockSession.createProducer(mockQueue)).thenReturn(mockMsgProducer);
		when(mockSession.createBytesMessage()).thenReturn(mockBytesMsg);
		final Long id = 3L;
		SenderSyncMessage msg = new SenderSyncMessage();
		msg.setId(id);
		byte[] msgBytes = JsonUtils.marshalToBytes(List.of(msg));
		byte[] expectedSentBytes = Utils.compress(msgBytes);
		
		SenderUtils.sendBatch(mockConnFactory, SITE_ID, List.of(msg), msgBytes.length - 1);
		
		verify(mockBytesMsg).setIntProperty(SyncConstants.JMS_HEADER_BATCH_SIZE, 1);
		verify(mockBytesMsg).setStringProperty(eq(SyncConstants.JMS_HEADER_MSG_ID), anyString());
		verify(mockBytesMsg).setStringProperty(SyncConstants.JMS_HEADER_VERSION, AppUtils.getVersion());
		verify(mockBytesMsg).setStringProperty(SyncConstants.JMS_HEADER_SITE, SITE_ID);
		verify(mockBytesMsg).setStringProperty(SyncConstants.JMS_HEADER_TYPE, SYNC.name());
		ArgumentCaptor<byte[]> bytesCaptor = ArgumentCaptor.forClass(byte[].class);
		verify(mockBytesMsg).writeBytes(bytesCaptor.capture());
		assertTrue(Arrays.equals(expectedSentBytes, bytesCaptor.getValue()));
		verify(mockMsgProducer).send(mockBytesMsg);
		verify(mockMsgProducer).send(mockBytesMsg);
	}
	
	@Test
	public void sendBatch_shouldCompressAndSendALargeMessageAsAStream() throws Exception {
		StreamMessage mockStreamMsg = Mockito.mock(StreamMessage.class);
		when(mockConnFactory.createConnection()).thenReturn(mockConnection);
		when(mockConnection.createSession()).thenReturn(mockSession);
		when(mockSession.createQueue(QUEUE_NAME)).thenReturn(mockQueue);
		when(mockSession.createProducer(mockQueue)).thenReturn(mockMsgProducer);
		when(mockSession.createStreamMessage()).thenReturn(mockStreamMsg);
		final Long id = 3L;
		SenderSyncMessage msg = new SenderSyncMessage();
		msg.setId(id);
		byte[] expectedSentBytes = Utils.compress(JsonUtils.marshalToBytes(List.of(msg)));
		
		SenderUtils.sendBatch(mockConnFactory, SITE_ID, List.of(msg), expectedSentBytes.length - 1);
		
		verify(mockStreamMsg).setIntProperty(SyncConstants.JMS_HEADER_BATCH_SIZE, 1);
		verify(mockStreamMsg).setStringProperty(eq(SyncConstants.JMS_HEADER_MSG_ID), anyString());
		verify(mockStreamMsg).setStringProperty(SyncConstants.JMS_HEADER_VERSION, AppUtils.getVersion());
		verify(mockStreamMsg).setStringProperty(SyncConstants.JMS_HEADER_SITE, SITE_ID);
		verify(mockStreamMsg).setStringProperty(SyncConstants.JMS_HEADER_TYPE, SYNC.name());
		ArgumentCaptor<byte[]> bytesCaptor = ArgumentCaptor.forClass(byte[].class);
		verify(mockStreamMsg).writeBytes(bytesCaptor.capture());
		assertTrue(Arrays.equals(expectedSentBytes, bytesCaptor.getValue()));
		verify(mockMsgProducer).send(mockStreamMsg);
		verify(mockMsgProducer).send(mockStreamMsg);
	}
	
}
