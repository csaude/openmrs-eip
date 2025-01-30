package org.openmrs.eip.app.receiver;

import static org.junit.Assert.assertEquals;
import static org.openmrs.eip.app.SyncConstants.MGT_DATASOURCE_NAME;
import static org.openmrs.eip.app.SyncConstants.MGT_TX_MGR;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.openmrs.eip.app.AppUtils;
import org.openmrs.eip.app.management.entity.receiver.JmsMessage;
import org.openmrs.eip.app.management.repository.JmsMessageRepository;
import org.openmrs.eip.app.management.repository.SyncMessageRepository;
import org.openmrs.eip.app.route.TestUtils;
import org.openmrs.eip.component.SyncOperation;
import org.openmrs.eip.component.exception.EIPException;
import org.openmrs.eip.component.model.PersonModel;
import org.openmrs.eip.component.model.SyncMetadata;
import org.openmrs.eip.component.model.SyncModel;
import org.openmrs.eip.component.utils.JsonUtils;
import org.powermock.reflect.Whitebox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;

@Sql(scripts = {
        "classpath:mgt_site_info.sql" }, config = @SqlConfig(dataSource = MGT_DATASOURCE_NAME, transactionManager = MGT_TX_MGR))
public class ReceiverSyncJmsMessageTaskTest extends BaseReceiverTest {
	
	private ReceiverSyncJmsMessageTask task;
	
	@Autowired
	private SyncMessageRepository synMsgRepo;
	
	@Autowired
	private JmsMessageRepository jmsMsgRepo;
	
	private final String ORIGINAL_DELETE_QUERY = ReceiverSyncJmsMessageTask.jmsDeleteQuery;
	
	@After
	public void tearDown() {
		Whitebox.setInternalState(ReceiverSyncJmsMessageTask.class, "jmsDeleteQuery", ORIGINAL_DELETE_QUERY);
	}
	
	private JmsMessage createMessage(int index) {
		JmsMessage msg = new JmsMessage();
		SyncMetadata md = new SyncMetadata();
		md.setMessageUuid("msg-uuid-" + index);
		md.setOperation(SyncOperation.c.name());
		md.setDateSent(LocalDateTime.now());
		md.setSyncVersion(AppUtils.getVersion());
		md.setSourceIdentifier("remote1");
		PersonModel m = new PersonModel();
		m.setUuid("entity-uuid-" + index);
		SyncModel synModel = SyncModel.builder().tableToSyncModelClass(m.getClass()).model(m).metadata(md).build();
		msg.setBody(JsonUtils.marshalToBytes(synModel));
		msg.setDateCreated(new Date());
		msg.setType(JmsMessage.MessageType.SYNC);
		jmsMsgRepo.save(msg);
		return msg;
	}
	
	@Test
	public void process_shouldProcessAllJmsMessagesInABatch() throws Exception {
		Assert.assertEquals(0, synMsgRepo.count());
		Assert.assertEquals(0, jmsMsgRepo.count());
		final int COUNT = 50;
		final List<JmsMessage> msgs = new ArrayList<>(COUNT);
		for (int i = 0; i < COUNT; i++) {
			msgs.add(createMessage(i));
		}
		TestUtils.flush();
		Assert.assertEquals(COUNT, jmsMsgRepo.count());
		task = new ReceiverSyncJmsMessageTask();
		
		task.process(msgs);
		
		Assert.assertEquals(COUNT, synMsgRepo.count());
		Assert.assertEquals(0, jmsMsgRepo.count());
	}
	
	@Test
	public void process_shouldRollbackChangesIfAnErrorIsEncountered() {
		Assert.assertEquals(0, synMsgRepo.count());
		Assert.assertEquals(0, jmsMsgRepo.count());
		final int COUNT = 5;
		final List<JmsMessage> msgs = new ArrayList<>(COUNT);
		for (int i = 0; i < COUNT; i++) {
			msgs.add(createMessage(i));
		}
		TestUtils.flush();
		Assert.assertEquals(COUNT, jmsMsgRepo.count());
		task = new ReceiverSyncJmsMessageTask();
		Whitebox.setInternalState(ReceiverSyncJmsMessageTask.class, "jmsDeleteQuery", "BAD QUERY");
		
		Throwable thrown = Assert.assertThrows(EIPException.class, () -> task.process(msgs));
		
		assertEquals("An error occurred while processing a batch of JMS messages", thrown.getMessage());
		Assert.assertEquals(COUNT, jmsMsgRepo.count());
		Assert.assertEquals(0, synMsgRepo.count());
	}
	
}
