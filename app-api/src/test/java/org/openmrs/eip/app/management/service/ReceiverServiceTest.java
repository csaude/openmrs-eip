package org.openmrs.eip.app.management.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.openmrs.eip.app.SyncConstants.MGT_DATASOURCE_NAME;
import static org.openmrs.eip.app.SyncConstants.MGT_TX_MGR;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openmrs.eip.app.management.entity.receiver.ReceiverPrunedItem;
import org.openmrs.eip.app.management.entity.receiver.ReceiverRetryQueueItem;
import org.openmrs.eip.app.management.entity.receiver.ReceiverSyncArchive;
import org.openmrs.eip.app.management.entity.receiver.SyncMessage;
import org.openmrs.eip.app.management.entity.receiver.SyncedMessage;
import org.openmrs.eip.app.management.entity.receiver.SyncedMessage.SyncOutcome;
import org.openmrs.eip.app.management.repository.ReceiverPrunedItemRepository;
import org.openmrs.eip.app.management.repository.ReceiverRetryRepository;
import org.openmrs.eip.app.management.repository.ReceiverSyncArchiveRepository;
import org.openmrs.eip.app.management.repository.SyncMessageRepository;
import org.openmrs.eip.app.management.repository.SyncedMessageRepository;
import org.openmrs.eip.app.receiver.BaseReceiverTest;
import org.openmrs.eip.component.management.hash.entity.BaseHashEntity;
import org.openmrs.eip.component.management.hash.entity.PatientHash;
import org.openmrs.eip.component.model.PatientModel;
import org.openmrs.eip.component.service.impl.PatientService;
import org.openmrs.eip.component.utils.HashUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;

public class ReceiverServiceTest extends BaseReceiverTest {
	
	@Autowired
	private SyncMessageRepository syncMsgRepo;
	
	@Autowired
	private SyncedMessageRepository syncedMsgRepo;
	
	@Autowired
	private ReceiverSyncArchiveRepository archiveRepo;
	
	@Autowired
	private ReceiverRetryRepository retryRepo;
	
	@Autowired
	private ReceiverPrunedItemRepository prunedRepo;
	
	@Autowired
	private ReceiverService service;
	
	@Autowired
	private PatientService patientService;
	
	@Test
	@Sql(scripts = { "classpath:mgt_site_info.sql",
	        "classpath:mgt_receiver_sync_archive.sql" }, config = @SqlConfig(dataSource = MGT_DATASOURCE_NAME, transactionManager = MGT_TX_MGR))
	public void prune_shouldMoveAnArchiveToThePrunedQueue() {
		final Long id = 1L;
		ReceiverSyncArchive archive = archiveRepo.findById(id).get();
		assertEquals(0, prunedRepo.count());
		
		service.prune(archive);
		
		assertFalse(archiveRepo.findById(id).isPresent());
		List<ReceiverPrunedItem> prunedItems = prunedRepo.findAll();
		assertEquals(1, prunedItems.size());
		assertEquals(archive.getMessageUuid(), prunedItems.get(0).getMessageUuid());
	}
	
	@Test
	@Sql(scripts = { "classpath:mgt_site_info.sql",
	        "classpath:mgt_receiver_sync_msg.sql" }, config = @SqlConfig(dataSource = MGT_DATASOURCE_NAME, transactionManager = MGT_TX_MGR))
	public void moveToSyncedQueue_shouldMoveTheMessageToTheSyncedQueue() {
		final Long id = 1L;
		SyncMessage msg = syncMsgRepo.findById(id).get();
		assertEquals(0, syncedMsgRepo.count());
		
		service.moveToSyncedQueue(msg, SyncOutcome.SUCCESS);
		
		assertFalse(syncMsgRepo.findById(id).isPresent());
		List<SyncedMessage> syncedItems = syncedMsgRepo.findAll();
		assertEquals(1, syncedItems.size());
		assertEquals(msg.getMessageUuid(), syncedItems.get(0).getMessageUuid());
		assertEquals(SyncOutcome.SUCCESS, syncedItems.get(0).getOutcome());
	}
	
	@Test
	@Sql(scripts = { "classpath:mgt_site_info.sql",
	        "classpath:mgt_receiver_synced_msg.sql" }, config = @SqlConfig(dataSource = MGT_DATASOURCE_NAME, transactionManager = MGT_TX_MGR))
	public void moveToSyncedQueue_shouldMoveTheSyncedMessageToTheArchiveQueue() {
		final Long id = 1L;
		SyncedMessage msg = syncedMsgRepo.findById(id).get();
		assertEquals(0, archiveRepo.count());
		long timestamp = System.currentTimeMillis();
		
		service.archiveSyncedMessage(msg);
		
		assertFalse(syncedMsgRepo.findById(id).isPresent());
		List<ReceiverSyncArchive> archives = archiveRepo.findAll();
		assertEquals(1, archives.size());
		ReceiverSyncArchive a = archives.get(0);
		assertEquals(msg.getMessageUuid(), a.getMessageUuid());
		assertTrue(a.getDateCreated().getTime() == timestamp || a.getDateCreated().getTime() > timestamp);
	}
	
	@Test
	@Sql(scripts = { "classpath:mgt_site_info.sql",
	        "classpath:mgt_receiver_retry_queue.sql" }, config = @SqlConfig(dataSource = MGT_DATASOURCE_NAME, transactionManager = MGT_TX_MGR))
	public void archiveRetry_shouldMoveTheSyncedMessageToTheArchiveQueue() {
		final Long id = 1L;
		ReceiverRetryQueueItem retry = retryRepo.findById(id).get();
		assertEquals(0, archiveRepo.count());
		long timestamp = System.currentTimeMillis();
		
		service.archiveRetry(retry);
		
		assertFalse(retryRepo.findById(id).isPresent());
		List<ReceiverSyncArchive> archives = archiveRepo.findAll();
		assertEquals(1, archives.size());
		ReceiverSyncArchive a = archives.get(0);
		assertEquals(retry.getMessageUuid(), a.getMessageUuid());
		assertTrue(a.getDateCreated().getTime() == timestamp || a.getDateCreated().getTime() > timestamp);
	}
	
	@Test
	@Sql(scripts = { "classpath:openmrs_core_data.sql", "classpath:openmrs_patient.sql" })
	public void moveToArchiveQueue_shouldMoveTheConflictToTheArchiveQueueAndUpdateTheHash() {
		final String uuid = "abfd940e-32dc-491f-8038-a8f3afe3e35b";
		BaseHashEntity hashEntity = new PatientHash();
		hashEntity.setIdentifier(uuid);
		final String expectedNewHash = HashUtils.computeHash(patientService.getModel(uuid));
		final String currentHash = "current-hash";
		assertNotEquals(currentHash, expectedNewHash);
		hashEntity.setHash(currentHash);
		hashEntity.setDateCreated(LocalDateTime.now());
		HashUtils.saveHash(hashEntity, producerTemplate, false);
		Assert.assertNull(hashEntity.getDateChanged());
		final long timestamp = System.currentTimeMillis();
		
		service.updateHash(PatientModel.class.getName(), uuid);
		
		hashEntity = HashUtils.getStoredHash(uuid, PatientHash.class, producerTemplate);
		assertEquals(expectedNewHash, hashEntity.getHash());
		long dateChangedMillis = hashEntity.getDateChanged().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
		assertTrue(dateChangedMillis == timestamp || dateChangedMillis > timestamp);
	}
	
}