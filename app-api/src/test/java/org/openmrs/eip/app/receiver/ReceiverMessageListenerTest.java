package org.openmrs.eip.app.receiver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.openmrs.eip.app.SyncConstants.JMS_HEADER_BATCH_SIZE;
import static org.openmrs.eip.app.SyncConstants.JMS_HEADER_COMPRESSED;
import static org.openmrs.eip.app.SyncConstants.JMS_HEADER_MSG_ID;
import static org.openmrs.eip.app.SyncConstants.JMS_HEADER_MSG_LENGTH;
import static org.openmrs.eip.app.SyncConstants.JMS_HEADER_SITE;
import static org.openmrs.eip.app.SyncConstants.JMS_HEADER_TYPE;
import static org.openmrs.eip.app.SyncConstants.MGT_DATASOURCE_NAME;
import static org.openmrs.eip.app.SyncConstants.MGT_TX_MGR;
import static org.openmrs.eip.app.management.entity.receiver.JmsMessage.MessageType.SYNC;
import static org.openmrs.eip.component.model.SyncModel.builder;
import static org.openmrs.eip.component.utils.JsonUtils.marshall;

import java.util.Arrays;
import java.util.List;

import org.apache.activemq.command.ActiveMQBytesMessage;
import org.apache.activemq.command.ActiveMQStreamMessage;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.openmrs.eip.app.management.entity.receiver.JmsMessage;
import org.openmrs.eip.app.management.repository.JmsMessageRepository;
import org.openmrs.eip.component.exception.EIPException;
import org.openmrs.eip.component.model.PersonModel;
import org.openmrs.eip.component.model.SyncMetadata;
import org.openmrs.eip.component.model.SyncModel;
import org.openmrs.eip.component.utils.Utils;
import org.powermock.reflect.Whitebox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;

import jakarta.jms.BytesMessage;
import jakarta.jms.StreamMessage;
import jakarta.jms.TextMessage;

@Sql(scripts = {
        "classpath:mgt_site_info.sql" }, config = @SqlConfig(dataSource = MGT_DATASOURCE_NAME, transactionManager = MGT_TX_MGR))
public class ReceiverMessageListenerTest extends BaseReceiverTest {
	
	@Autowired
	private ReceiverMessageListener listener;
	
	@Autowired
	private JmsMessageRepository repo;
	
	@Autowired
	private SyncStatusProcessor statusProcessor;
	
	private SyncStatusProcessor mockStatusProcessor;
	
	private boolean skipDuplicatesOriginal;
	
	@Before
	public void setup() {
		mockStatusProcessor = Mockito.mock(SyncStatusProcessor.class);
		skipDuplicatesOriginal = Whitebox.getInternalState(listener, "skipDuplicates");
		Whitebox.setInternalState(listener, SyncStatusProcessor.class, mockStatusProcessor);
	}
	
	@After
	public void tearDown() {
		Whitebox.setInternalState(listener, "skipDuplicates", skipDuplicatesOriginal);
		Whitebox.setInternalState(listener, SyncStatusProcessor.class, statusProcessor);
	}
	
	@Test
	public void onMessage_shouldAddTheJmsMessageToTheDb() throws Exception {
		assertEquals(0, repo.count());
		SyncMetadata md = new SyncMetadata();
		final String messageUuid = "msg-uuid-1";
		md.setMessageUuid(messageUuid);
		SyncModel model = builder().tableToSyncModelClass(PersonModel.class).metadata(md).build();
		final String body = marshall(model);
		final String siteId = "remote1";
		BytesMessage bytesMsg = new ActiveMQBytesMessage();
		bytesMsg.writeBytes(body.getBytes());
		bytesMsg.setStringProperty(JMS_HEADER_MSG_ID, "jms-msg-uuid");
		bytesMsg.setStringProperty(JMS_HEADER_SITE, siteId);
		bytesMsg.setStringProperty(JMS_HEADER_TYPE, SYNC.name());
		
		listener.onMessage(bytesMsg);
		
		List<JmsMessage> msgs = repo.findAll();
		assertEquals(1, msgs.size());
		JmsMessage msg = msgs.get(0);
		assertTrue(Arrays.equals(body.getBytes(), msg.getBody()));
		assertEquals(messageUuid, msg.getMessageId());
		assertEquals(siteId, msg.getSiteId());
		assertEquals(SYNC, msg.getType());
		Mockito.verify(mockStatusProcessor).process(ArgumentMatchers.eq(siteId));
	}
	
	@Test
	@Sql(scripts = { "classpath:mgt_site_info.sql",
	        "classpath:mgt_jms_msg.sql" }, config = @SqlConfig(dataSource = MGT_DATASOURCE_NAME, transactionManager = MGT_TX_MGR))
	public void onMessage_shouldSkipADuplicateMessage() throws Exception {
		final String messageUuid = "1cef940e-32dc-491f-8038-a8f3afe3e37d";
		SyncMetadata md = new SyncMetadata();
		md.setMessageUuid(messageUuid);
		final String body = marshall(builder().tableToSyncModelClass(PersonModel.class).metadata(md).build());
		assertTrue(repo.existsByMessageId(messageUuid));
		final long originalCount = repo.count();
		final String siteId = "remote1";
		BytesMessage bytesMsg = new ActiveMQBytesMessage();
		bytesMsg.writeBytes(body.getBytes());
		bytesMsg.setStringProperty(JMS_HEADER_MSG_ID, "test-msg-uuid");
		bytesMsg.setStringProperty(JMS_HEADER_SITE, siteId);
		bytesMsg.setStringProperty(JMS_HEADER_TYPE, SYNC.name());
		
		listener.onMessage(bytesMsg);
		
		assertEquals(originalCount, repo.count());
		Mockito.verifyNoInteractions(mockStatusProcessor);
	}
	
	@Test
	@Sql(scripts = { "classpath:mgt_site_info.sql",
	        "classpath:mgt_jms_msg.sql" }, config = @SqlConfig(dataSource = MGT_DATASOURCE_NAME, transactionManager = MGT_TX_MGR))
	public void onMessage_shouldNotSkipADuplicateMessage() throws Exception {
		Whitebox.setInternalState(listener, "skipDuplicates", false);
		final String msgId = "1cef940e-32dc-491f-8038-a8f3afe3e37d";
		SyncMetadata md = new SyncMetadata();
		md.setMessageUuid(msgId);
		final String body = marshall(builder().tableToSyncModelClass(PersonModel.class).metadata(md).build());
		assertTrue(repo.existsByMessageId(msgId));
		final String siteId = "remote1";
		BytesMessage bytesMsg = new ActiveMQBytesMessage();
		bytesMsg.writeBytes(body.getBytes());
		bytesMsg.setStringProperty(JMS_HEADER_MSG_ID, msgId);
		bytesMsg.setStringProperty(JMS_HEADER_SITE, siteId);
		bytesMsg.setStringProperty(JMS_HEADER_TYPE, SYNC.name());
		
		EIPException thrown = Assert.assertThrows(EIPException.class, () -> listener.onMessage(bytesMsg));
		Assert.assertEquals(DataIntegrityViolationException.class, thrown.getCause().getClass());
		Mockito.verifyNoInteractions(mockStatusProcessor);
	}
	
	@Test
	public void onMessage_shouldAddTheJmsMessageBatchToTheDb() throws Exception {
		assertEquals(0, repo.count());
		final int batchSize = 3;
		SyncMetadata md = new SyncMetadata();
		md.setMessageUuid("msg-uuid-1");
		final String body1 = marshall(builder().tableToSyncModelClass(PersonModel.class).metadata(md).build());
		md = new SyncMetadata();
		md.setMessageUuid("msg-uuid-2");
		final String body2 = marshall(builder().tableToSyncModelClass(PersonModel.class).metadata(md).build());
		md = new SyncMetadata();
		md.setMessageUuid("msg-uuid-3");
		final String body3 = marshall(builder().tableToSyncModelClass(PersonModel.class).metadata(md).build());
		final String siteId = "remote1";
		BytesMessage bytesMsg = new ActiveMQBytesMessage();
		bytesMsg.writeBytes(("[" + String.join(",", List.of(body1, body2, body3)) + "]").getBytes());
		bytesMsg.setStringProperty(JMS_HEADER_MSG_ID, "batch-msg-id");
		bytesMsg.setStringProperty(JMS_HEADER_SITE, siteId);
		bytesMsg.setStringProperty(JMS_HEADER_TYPE, SYNC.name());
		bytesMsg.setIntProperty(JMS_HEADER_BATCH_SIZE, batchSize);
		
		listener.onMessage(bytesMsg);
		
		List<JmsMessage> msgs = repo.findAll();
		assertEquals(batchSize, msgs.size());
		JmsMessage msg = msgs.get(0);
		assertTrue(Arrays.equals(body1.getBytes(), msg.getBody()));
		assertEquals("msg-uuid-1", msg.getMessageId());
		assertEquals(siteId, msg.getSiteId());
		assertEquals(SYNC, msg.getType());
		msg = msgs.get(1);
		assertTrue(Arrays.equals(body2.getBytes(), msg.getBody()));
		assertEquals("msg-uuid-2", msg.getMessageId());
		assertEquals(siteId, msg.getSiteId());
		assertEquals(SYNC, msg.getType());
		msg = msgs.get(2);
		assertTrue(Arrays.equals(body3.getBytes(), msg.getBody()));
		assertEquals("msg-uuid-3", msg.getMessageId());
		assertEquals(siteId, msg.getSiteId());
		assertEquals(SYNC, msg.getType());
		Mockito.verify(mockStatusProcessor).process(ArgumentMatchers.eq(siteId));
	}
	
	@Test
	public void onMessage_shouldProcessTextMessage() throws Exception {
		final String siteId = "remote1";
		assertEquals(0, repo.count());
		SyncMetadata md = new SyncMetadata();
		md.setMessageUuid("msg-uuid-1");
		SyncModel model = builder().tableToSyncModelClass(PersonModel.class).metadata(md).build();
		final String body = marshall(model);
		TextMessage textMsg = new ActiveMQTextMessage();
		textMsg.setText(body);
		textMsg.setStringProperty(JMS_HEADER_MSG_ID, "jms-msg-uuid");
		textMsg.setStringProperty(JMS_HEADER_SITE, "remote1");
		textMsg.setStringProperty(JMS_HEADER_TYPE, SYNC.name());
		textMsg.setStringProperty(JMS_HEADER_SITE, siteId);
		
		listener.onMessage(textMsg);
		
		List<JmsMessage> msgs = repo.findAll();
		assertEquals(1, msgs.size());
		assertTrue(Arrays.equals(body.getBytes(), msgs.get(0).getBody()));
		Mockito.verify(mockStatusProcessor).process(ArgumentMatchers.eq(siteId));
	}
	
	@Test
	public void onMessage_shouldProcessStreamMessage() throws Exception {
		final String siteId = "remote1";
		assertEquals(0, repo.count());
		SyncMetadata md = new SyncMetadata();
		md.setMessageUuid("msg-uuid-1");
		SyncModel model = builder().tableToSyncModelClass(PersonModel.class).metadata(md).build();
		final String body = marshall(model);
		StreamMessage streamMsg = new ActiveMQStreamMessage();
		streamMsg.writeBytes(body.getBytes());
		streamMsg.reset();
		streamMsg.setIntProperty(JMS_HEADER_MSG_LENGTH, body.getBytes().length);
		streamMsg.setStringProperty(JMS_HEADER_MSG_ID, "jms-msg-uuid");
		streamMsg.setStringProperty(JMS_HEADER_SITE, "remote1");
		streamMsg.setStringProperty(JMS_HEADER_TYPE, SYNC.name());
		streamMsg.setStringProperty(JMS_HEADER_SITE, siteId);
		
		listener.onMessage(streamMsg);
		
		List<JmsMessage> msgs = repo.findAll();
		assertEquals(1, msgs.size());
		assertTrue(Arrays.equals(body.getBytes(), msgs.get(0).getBody()));
		Mockito.verify(mockStatusProcessor).process(ArgumentMatchers.eq(siteId));
	}
	
	@Test
	public void onMessage_shouldProcessCompressedMessage() throws Exception {
		final String siteId = "remote1";
		assertEquals(0, repo.count());
		SyncMetadata md = new SyncMetadata();
		md.setMessageUuid("msg-uuid-1");
		SyncModel model = builder().tableToSyncModelClass(PersonModel.class).metadata(md).build();
		final String body = marshall(model);
		BytesMessage bytesMsg = new ActiveMQBytesMessage();
		bytesMsg.writeBytes(Utils.compress(body.getBytes()));
		bytesMsg.setStringProperty(JMS_HEADER_MSG_ID, "jms-msg-uuid");
		bytesMsg.setStringProperty(JMS_HEADER_SITE, "remote1");
		bytesMsg.setStringProperty(JMS_HEADER_TYPE, SYNC.name());
		bytesMsg.setBooleanProperty(JMS_HEADER_COMPRESSED, true);
		bytesMsg.setStringProperty(JMS_HEADER_SITE, siteId);
		
		listener.onMessage(bytesMsg);
		
		List<JmsMessage> msgs = repo.findAll();
		assertEquals(1, msgs.size());
		assertTrue(Arrays.equals(body.getBytes(), msgs.get(0).getBody()));
		Mockito.verify(mockStatusProcessor).process(ArgumentMatchers.eq(siteId));
	}
	
}
