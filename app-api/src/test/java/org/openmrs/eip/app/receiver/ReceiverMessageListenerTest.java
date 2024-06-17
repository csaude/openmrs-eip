package org.openmrs.eip.app.receiver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.openmrs.eip.app.SyncConstants.JMS_HEADER_MSG_ID;
import static org.openmrs.eip.app.SyncConstants.JMS_HEADER_SITE;
import static org.openmrs.eip.app.SyncConstants.JMS_HEADER_TYPE;
import static org.openmrs.eip.app.SyncConstants.MGT_DATASOURCE_NAME;
import static org.openmrs.eip.app.SyncConstants.MGT_TX_MGR;
import static org.openmrs.eip.app.management.entity.receiver.JmsMessage.MessageType.SYNC;

import java.util.Arrays;
import java.util.List;

import org.apache.activemq.command.ActiveMQBytesMessage;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.eip.app.management.entity.receiver.JmsMessage;
import org.openmrs.eip.app.management.repository.JmsMessageRepository;
import org.openmrs.eip.component.exception.EIPException;
import org.powermock.reflect.Whitebox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;

import jakarta.jms.BytesMessage;

public class ReceiverMessageListenerTest extends BaseReceiverTest {
	
	@Autowired
	private ReceiverMessageListener listener;
	
	@Autowired
	private JmsMessageRepository repo;
	
	private boolean skipDuplicatesOriginal;
	
	@Before
	public void setup() {
		skipDuplicatesOriginal = Whitebox.getInternalState(listener, "skipDuplicates");
	}
	
	@After
	public void tearDown() {
		Whitebox.setInternalState(listener, "skipDuplicates", skipDuplicatesOriginal);
	}
	
	@Test
	@Sql(scripts = {
	        "classpath:mgt_site_info.sql" }, config = @SqlConfig(dataSource = MGT_DATASOURCE_NAME, transactionManager = MGT_TX_MGR))
	public void onMessage_shouldAddTheJmsMessageToTheDb() throws Exception {
		assertEquals(0, repo.count());
		final String body = "{}";
		final String siteId = "remote1";
		final String msgId = "jms-msg-uuid";
		BytesMessage bytesMsg = new ActiveMQBytesMessage();
		bytesMsg.writeBytes(body.getBytes());
		bytesMsg.setStringProperty(JMS_HEADER_MSG_ID, msgId);
		bytesMsg.setStringProperty(JMS_HEADER_SITE, siteId);
		bytesMsg.setStringProperty(JMS_HEADER_TYPE, SYNC.name());
		
		listener.onMessage(bytesMsg);
		
		List<JmsMessage> msgs = repo.findAll();
		assertEquals(1, msgs.size());
		JmsMessage msg = msgs.get(0);
		assertTrue(Arrays.equals(body.getBytes(), msg.getBody()));
		assertEquals(msgId, msg.getMessageId());
		assertEquals(siteId, msg.getSiteId());
		assertEquals(SYNC, msg.getType());
	}
	
	@Test
	@Sql(scripts = { "classpath:mgt_site_info.sql",
	        "classpath:mgt_jms_msg.sql" }, config = @SqlConfig(dataSource = MGT_DATASOURCE_NAME, transactionManager = MGT_TX_MGR))
	public void onMessage_shouldSkipADuplicateMessage() throws Exception {
		final String msgId = "1cef940e-32dc-491f-8038-a8f3afe3e37d";
		assertTrue(repo.existsByMessageId(msgId));
		final long originalCount = repo.count();
		final String body = "{}";
		final String siteId = "remote1";
		BytesMessage bytesMsg = new ActiveMQBytesMessage();
		bytesMsg.writeBytes(body.getBytes());
		bytesMsg.setStringProperty(JMS_HEADER_MSG_ID, msgId);
		bytesMsg.setStringProperty(JMS_HEADER_SITE, siteId);
		bytesMsg.setStringProperty(JMS_HEADER_TYPE, SYNC.name());
		
		listener.onMessage(bytesMsg);
		
		assertEquals(originalCount, repo.count());
	}
	
	@Test
	@Sql(scripts = { "classpath:mgt_site_info.sql",
	        "classpath:mgt_jms_msg.sql" }, config = @SqlConfig(dataSource = MGT_DATASOURCE_NAME, transactionManager = MGT_TX_MGR))
	public void onMessage_shouldNotSkipADuplicateMessage() throws Exception {
		Whitebox.setInternalState(listener, "skipDuplicates", false);
		final String msgId = "1cef940e-32dc-491f-8038-a8f3afe3e37d";
		assertTrue(repo.existsByMessageId(msgId));
		final String body = "{}";
		final String siteId = "remote1";
		BytesMessage bytesMsg = new ActiveMQBytesMessage();
		bytesMsg.writeBytes(body.getBytes());
		bytesMsg.setStringProperty(JMS_HEADER_MSG_ID, msgId);
		bytesMsg.setStringProperty(JMS_HEADER_SITE, siteId);
		bytesMsg.setStringProperty(JMS_HEADER_TYPE, SYNC.name());
		
		EIPException thrown = Assert.assertThrows(EIPException.class, () -> listener.onMessage(bytesMsg));
		Assert.assertEquals(DataIntegrityViolationException.class, thrown.getCause().getClass());
	}
	
}
