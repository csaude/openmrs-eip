package org.openmrs.eip.app.receiver.task;

import static org.openmrs.eip.app.SyncConstants.MGT_DATASOURCE_NAME;
import static org.openmrs.eip.app.SyncConstants.MGT_TX_MGR;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openmrs.eip.app.AppUtils;
import org.openmrs.eip.app.management.entity.receiver.JmsMessage;
import org.openmrs.eip.app.management.repository.JmsMessageRepository;
import org.openmrs.eip.app.management.repository.SiteRepository;
import org.openmrs.eip.app.management.repository.SyncMessageRepository;
import org.openmrs.eip.app.receiver.BaseReceiverTest;
import org.openmrs.eip.app.receiver.ReceiverJmsMessageTask;
import org.openmrs.eip.component.SyncOperation;
import org.openmrs.eip.component.model.PersonModel;
import org.openmrs.eip.component.model.SyncMetadata;
import org.openmrs.eip.component.model.SyncModel;
import org.openmrs.eip.component.utils.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;

@Sql(scripts = {
        "classpath:mgt_site_info.sql" }, config = @SqlConfig(dataSource = MGT_DATASOURCE_NAME, transactionManager = MGT_TX_MGR))
public class ReceiverJmsMessageTaskTest extends BaseReceiverTest {
	
	private ReceiverJmsMessageTask task;
	
	@Autowired
	private SyncMessageRepository synMsgRepo;
	
	@Autowired
	private JmsMessageRepository jmsMsgRepo;
	
	@Autowired
	private SiteRepository siteRepo;
	
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
		Assert.assertEquals(COUNT, jmsMsgRepo.count());
		task = new ReceiverJmsMessageTask();
		
		task.process(msgs);
		
		Assert.assertEquals(COUNT, synMsgRepo.count());
		Assert.assertEquals(0, jmsMsgRepo.count());
	}
	
}
