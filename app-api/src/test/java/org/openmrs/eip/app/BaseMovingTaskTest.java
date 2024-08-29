package org.openmrs.eip.app;

import static org.junit.Assert.assertEquals;
import static org.openmrs.eip.app.SyncConstants.MGT_DATASOURCE_NAME;
import static org.openmrs.eip.app.SyncConstants.MGT_TX_MGR;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openmrs.eip.app.management.entity.receiver.SyncedMessage;
import org.openmrs.eip.app.management.repository.ReceiverSyncArchiveRepository;
import org.openmrs.eip.app.management.repository.SyncedMessageRepository;
import org.openmrs.eip.app.receiver.BaseReceiverTest;
import org.openmrs.eip.app.receiver.SyncedMessageArchiver;
import org.openmrs.eip.component.exception.EIPException;
import org.powermock.reflect.Whitebox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;

@Sql(scripts = { "classpath:mgt_site_info.sql",
        "classpath:mgt_receiver_synced_msg.sql" }, config = @SqlConfig(dataSource = MGT_DATASOURCE_NAME, transactionManager = MGT_TX_MGR))
public class BaseMovingTaskTest extends BaseReceiverTest {
	
	private SyncedMessageArchiver task;
	
	@Autowired
	private SyncedMessageRepository syncedMsgRepo;
	
	@Autowired
	private ReceiverSyncArchiveRepository archiveRepo;
	
	@Test
	public void process_shouldMoveAllItemsInABatch() throws Exception {
		List<SyncedMessage> items = syncedMsgRepo.findAll();
		Assert.assertFalse(items.isEmpty());
		Assert.assertEquals(0, archiveRepo.count());
		task = new SyncedMessageArchiver();
		
		task.process(items);
		
		Assert.assertEquals(items.size(), archiveRepo.count());
		Assert.assertEquals(0, syncedMsgRepo.count());
	}
	
	@Test
	public void process_shouldRollbackChangesIfAnErrorIsEncountered() {
		List<SyncedMessage> items = syncedMsgRepo.findAll();
		Assert.assertFalse(items.isEmpty());
		Assert.assertEquals(0, archiveRepo.count());
		task = new SyncedMessageArchiver();
		Whitebox.setInternalState(task, "deleteQuery", "BAD QUERY");
		
		Throwable thrown = Assert.assertThrows(EIPException.class, () -> task.process(null));
		
		assertEquals("An error occurred while processing a batch of items", thrown.getMessage());
		Assert.assertEquals(items.size(), syncedMsgRepo.count());
		Assert.assertEquals(0, archiveRepo.count());
	}
}
