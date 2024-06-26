package org.openmrs.eip.app.management.service;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.IntStream.rangeClosed;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.openmrs.eip.app.SyncConstants.MGT_DATASOURCE_NAME;
import static org.openmrs.eip.app.SyncConstants.MGT_TX_MGR;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.openmrs.eip.app.management.entity.ReconciliationResponse;
import org.openmrs.eip.app.management.entity.receiver.JmsMessage;
import org.openmrs.eip.app.management.entity.receiver.MissingEntity;
import org.openmrs.eip.app.management.entity.receiver.ReceiverSyncRequest;
import org.openmrs.eip.app.management.entity.receiver.ReceiverSyncRequest.ReceiverRequestStatus;
import org.openmrs.eip.app.management.entity.receiver.ReceiverTableReconciliation;
import org.openmrs.eip.app.management.entity.receiver.ReconciliationMessage;
import org.openmrs.eip.app.management.entity.receiver.SiteInfo;
import org.openmrs.eip.app.management.entity.receiver.SiteReconciliation;
import org.openmrs.eip.app.management.repository.JmsMessageRepository;
import org.openmrs.eip.app.management.repository.MissingEntityRepository;
import org.openmrs.eip.app.management.repository.ReceiverSyncRequestRepository;
import org.openmrs.eip.app.management.repository.ReceiverTableReconcileRepository;
import org.openmrs.eip.app.management.repository.ReconciliationMsgRepository;
import org.openmrs.eip.app.management.repository.SiteReconciliationRepository;
import org.openmrs.eip.app.management.repository.SiteRepository;
import org.openmrs.eip.app.receiver.BaseReceiverTest;
import org.openmrs.eip.component.utils.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;

@Sql(scripts = {
        "classpath:mgt_site_info.sql" }, config = @SqlConfig(dataSource = MGT_DATASOURCE_NAME, transactionManager = MGT_TX_MGR))
public class ReceiverReconcileServiceTest extends BaseReceiverTest {
	
	@Autowired
	private ReceiverReconcileService service;
	
	@Autowired
	private ReconciliationMsgRepository reconcileMsgRep;
	
	@Autowired
	private JmsMessageRepository jmsMsgRepo;
	
	@Autowired
	private SiteRepository siteRepo;
	
	@Autowired
	private ReceiverSyncRequestRepository requestRepo;
	
	@Autowired
	private SiteReconciliationRepository siteRecRepo;
	
	@Autowired
	private ReceiverTableReconcileRepository tableRecRepo;
	
	@Autowired
	private MissingEntityRepository missingRepo;
	
	@Test
	public void processJmsMessage_shouldProcessAndSaveAReconcileMessage() {
		assertEquals(0, reconcileMsgRep.count());
		assertEquals(0, tableRecRepo.count());
		final String table = "person";
		final String data = "person-uuid-1,person-uuid-2";
		final String version = "test-version";
		final int batchSize = 10;
		final boolean last = true;
		ReconciliationResponse resp = new ReconciliationResponse();
		resp.setTableName(table);
		resp.setBatchSize(batchSize);
		resp.setData(data);
		resp.setLastTableBatch(last);
		String payLoad = JsonUtils.marshall(resp);
		JmsMessage jmsMsg = new JmsMessage();
		SiteInfo site = siteRepo.getReferenceById(1L);
		jmsMsg.setSiteId(site.getIdentifier());
		jmsMsg.setType(JmsMessage.MessageType.SYNC);
		jmsMsg.setBody(payLoad.getBytes(UTF_8));
		jmsMsg.setSyncVersion(version);
		jmsMsg.setDateCreated(new Date());
		jmsMsgRepo.save(jmsMsg);
		assertEquals(1, jmsMsgRepo.count());
		Long timestamp = System.currentTimeMillis();
		
		service.processJmsMessage(jmsMsg);
		
		List<ReconciliationMessage> msgs = reconcileMsgRep.findAll();
		assertEquals(1, msgs.size());
		ReconciliationMessage msg = msgs.get(0);
		assertEquals(site, msg.getSite());
		assertEquals(table, msg.getTableName());
		assertEquals(batchSize, msg.getBatchSize().intValue());
		assertTrue(msg.isLastTableBatch());
		assertEquals(data, msg.getData());
		assertEquals(0, msg.getProcessedCount());
		assertEquals(version, msg.getRemoteSyncVersion());
		assertTrue(msg.getDateCreated().getTime() == timestamp || msg.getDateCreated().getTime() > timestamp);
		assertEquals(0, tableRecRepo.count());
		assertEquals(0, jmsMsgRepo.count());
	}
	
	@Test
	public void processJmsMessage_shouldAddTableReconciliationForTheFirstReconcileMessage() {
		assertEquals(0, reconcileMsgRep.count());
		assertEquals(0, tableRecRepo.count());
		final String table = "person";
		final String data = "person-uuid-1,person-uuid-2";
		final int batchSize = 10;
		final boolean last = true;
		final long rowCount = 100;
		final LocalDateTime remoteStartDate = LocalDateTime.now();
		ReconciliationResponse resp = new ReconciliationResponse();
		resp.setTableName(table);
		resp.setBatchSize(batchSize);
		resp.setData(data);
		resp.setLastTableBatch(last);
		resp.setRowCount(rowCount);
		resp.setRemoteStartDate(remoteStartDate);
		String payLoad = JsonUtils.marshall(resp);
		JmsMessage jmsMsg = new JmsMessage();
		SiteInfo site = siteRepo.getReferenceById(1L);
		jmsMsg.setSiteId(site.getIdentifier());
		jmsMsg.setType(JmsMessage.MessageType.SYNC);
		jmsMsg.setBody(payLoad.getBytes(UTF_8));
		jmsMsg.setDateCreated(new Date());
		jmsMsgRepo.save(jmsMsg);
		assertEquals(1, jmsMsgRepo.count());
		SiteReconciliation siteRec = new SiteReconciliation();
		siteRec.setSite(site);
		siteRec.setDateCreated(new Date());
		siteRecRepo.save(siteRec);
		assertEquals(1, siteRecRepo.count());
		Long timestamp = System.currentTimeMillis();
		
		service.processJmsMessage(jmsMsg);
		
		assertEquals(1, reconcileMsgRep.count());
		List<ReceiverTableReconciliation> tableRecs = tableRecRepo.findAll();
		assertEquals(1, tableRecs.size());
		ReceiverTableReconciliation tableRec = tableRecs.get(0);
		assertEquals(siteRec, tableRec.getSiteReconciliation());
		assertEquals(table, tableRec.getTableName());
		assertEquals(rowCount, tableRec.getRowCount());
		assertEquals(remoteStartDate, tableRec.getRemoteStartDate());
		assertTrue(tableRec.isLastBatchReceived());
		assertEquals(0, tableRec.getProcessedCount());
		assertFalse(tableRec.isCompleted());
		assertNull(tableRec.getDateChanged());
		assertTrue(tableRec.getDateCreated().getTime() == timestamp || tableRec.getDateCreated().getTime() > timestamp);
		assertEquals(0, jmsMsgRepo.count());
	}
	
	@Test
	public void processJmsMessage_shouldAddTableReconciliationForTheFirstReconcileMessageForAnEmptyTable() {
		assertEquals(0, reconcileMsgRep.count());
		assertEquals(0, tableRecRepo.count());
		final String table = "person";
		final String data = "person-uuid-1,person-uuid-2";
		final int batchSize = 10;
		final boolean last = true;
		final long rowCount = 0;
		final LocalDateTime remoteStartDate = LocalDateTime.now();
		ReconciliationResponse resp = new ReconciliationResponse();
		resp.setTableName(table);
		resp.setBatchSize(batchSize);
		resp.setData(data);
		resp.setLastTableBatch(last);
		resp.setRowCount(rowCount);
		resp.setRemoteStartDate(remoteStartDate);
		String payLoad = JsonUtils.marshall(resp);
		JmsMessage jmsMsg = new JmsMessage();
		SiteInfo site = siteRepo.getReferenceById(1L);
		jmsMsg.setSiteId(site.getIdentifier());
		jmsMsg.setType(JmsMessage.MessageType.SYNC);
		jmsMsg.setBody(payLoad.getBytes(UTF_8));
		jmsMsg.setDateCreated(new Date());
		jmsMsgRepo.save(jmsMsg);
		assertEquals(1, jmsMsgRepo.count());
		SiteReconciliation siteRec = new SiteReconciliation();
		siteRec.setSite(site);
		siteRec.setDateCreated(new Date());
		siteRecRepo.save(siteRec);
		assertEquals(1, siteRecRepo.count());
		Long timestamp = System.currentTimeMillis();
		
		service.processJmsMessage(jmsMsg);
		
		assertEquals(1, reconcileMsgRep.count());
		List<ReceiverTableReconciliation> tableRecs = tableRecRepo.findAll();
		assertEquals(1, tableRecs.size());
		ReceiverTableReconciliation tableRec = tableRecs.get(0);
		assertEquals(siteRec, tableRec.getSiteReconciliation());
		assertEquals(table, tableRec.getTableName());
		assertEquals(rowCount, tableRec.getRowCount());
		assertEquals(remoteStartDate, tableRec.getRemoteStartDate());
		assertTrue(tableRec.isLastBatchReceived());
		assertEquals(0, tableRec.getProcessedCount());
		assertFalse(tableRec.isCompleted());
		assertNull(tableRec.getDateChanged());
		assertTrue(tableRec.getDateCreated().getTime() == timestamp || tableRec.getDateCreated().getTime() > timestamp);
		assertEquals(0, jmsMsgRepo.count());
	}
	
	@Test
	@Sql(scripts = { "classpath:mgt_site_info.sql", "classpath:mgt_site_reconcile.sql",
	        "classpath:mgt_receiver_table_reconcile.sql" }, config = @SqlConfig(dataSource = MGT_DATASOURCE_NAME, transactionManager = MGT_TX_MGR))
	public void updateReconciliationMessage_shouldProcessFoundUuidsAndUpdateTheProcessedCount() {
		final String table = "person";
		final SiteInfo site = siteRepo.getReferenceById(3L);
		SiteReconciliation siteRec = siteRecRepo.getBySite(site);
		final String uuid1 = "uuid-1";
		final String uuid2 = "uuid-2";
		ReceiverTableReconciliation tableRec = tableRecRepo.getBySiteReconciliationAndTableName(siteRec, table);
		assertFalse(tableRec.isCompleted());
		assertFalse(tableRec.isLastBatchReceived());
		assertNull(tableRec.getDateChanged());
		final long originalProcessedCount = tableRec.getProcessedCount();
		ReconciliationMessage msg = new ReconciliationMessage();
		msg.setSite(site);
		msg.setTableName(table);
		msg.setBatchSize(3);
		msg.setData(uuid1 + "," + uuid2 + ",uuid-3");
		msg.setLastTableBatch(true);
		msg.setDateCreated(new Date());
		reconcileMsgRep.save(msg);
		assertEquals(0, msg.getProcessedCount());
		long timestamp = System.currentTimeMillis();
		
		service.updateReconciliationMessage(msg, true, List.of(uuid1, uuid2));
		
		assertEquals(2, msg.getProcessedCount());
		tableRec = tableRecRepo.getBySiteReconciliationAndTableName(siteRec, table);
		assertFalse(tableRec.isCompleted());
		assertTrue(tableRec.isLastBatchReceived());
		assertEquals(originalProcessedCount + msg.getProcessedCount(), tableRec.getProcessedCount());
		long dateChangedMillis = tableRec.getDateChanged().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
		assertTrue(dateChangedMillis == timestamp || dateChangedMillis > timestamp);
	}
	
	@Test
	@Sql(scripts = { "classpath:mgt_site_info.sql", "classpath:mgt_site_reconcile.sql",
	        "classpath:mgt_receiver_table_reconcile.sql" }, config = @SqlConfig(dataSource = MGT_DATASOURCE_NAME, transactionManager = MGT_TX_MGR))
	public void updateReconciliationMessage_shouldDeleteACompletedMessage() {
		final String table = "person";
		final SiteInfo site = siteRepo.getReferenceById(1L);
		final int batchSize = 5;
		ReconciliationMessage msg = new ReconciliationMessage();
		msg.setSite(site);
		msg.setTableName(table);
		msg.setBatchSize(batchSize);
		List<String> uuids = rangeClosed(1, batchSize).boxed().map(i -> "uuid-" + i).toList();
		msg.setData(StringUtils.join(uuids, ","));
		msg.setDateCreated(new Date());
		reconcileMsgRep.save(msg);
		
		service.updateReconciliationMessage(msg, true, uuids);
		
		assertFalse(reconcileMsgRep.findById(msg.getId()).isPresent());
	}
	
	@Test
	@Sql(scripts = { "classpath:mgt_site_info.sql", "classpath:mgt_site_reconcile.sql",
	        "classpath:mgt_receiver_table_reconcile.sql" }, config = @SqlConfig(dataSource = MGT_DATASOURCE_NAME, transactionManager = MGT_TX_MGR))
	public void updateReconciliationMessage_shouldNotDeleteAInCompleteMessage() {
		final String table = "person";
		final SiteInfo site = siteRepo.getReferenceById(1L);
		final int batchSize = 5;
		ReconciliationMessage msg = new ReconciliationMessage();
		msg.setSite(site);
		msg.setTableName(table);
		msg.setBatchSize(batchSize);
		List<String> uuids = rangeClosed(1, batchSize - 1).boxed().map(i -> "uuid-" + i).toList();
		msg.setData(StringUtils.join(uuids, ","));
		msg.setDateCreated(new Date());
		reconcileMsgRep.save(msg);
		
		service.updateReconciliationMessage(msg, true, uuids);
		
		assertTrue(reconcileMsgRep.findById(msg.getId()).isPresent());
	}
	
	@Test
	@Sql(scripts = { "classpath:mgt_site_info.sql", "classpath:mgt_site_reconcile.sql",
	        "classpath:mgt_receiver_table_reconcile.sql" }, config = @SqlConfig(dataSource = MGT_DATASOURCE_NAME, transactionManager = MGT_TX_MGR))
	public void updateReconciliationMessage_shouldMarkTableReconciliationAsCompleted() {
		final String table = "person";
		final SiteInfo site = siteRepo.getReferenceById(3L);
		SiteReconciliation siteRec = siteRecRepo.getBySite(site);
		ReceiverTableReconciliation tableRec = tableRecRepo.getBySiteReconciliationAndTableName(siteRec, table);
		assertFalse(tableRec.isCompleted());
		assertFalse(tableRec.isLastBatchReceived());
		assertNull(tableRec.getDateChanged());
		final long originalProcessedCount = tableRec.getProcessedCount();
		final int batchSize = 50;
		ReconciliationMessage msg = new ReconciliationMessage();
		msg.setSite(site);
		msg.setTableName(table);
		msg.setBatchSize(batchSize);
		msg.setLastTableBatch(true);
		List<String> uuids = rangeClosed(1, batchSize).boxed().map(i -> "uuid-" + i).toList();
		msg.setData(StringUtils.join(uuids, ","));
		msg.setDateCreated(new Date());
		reconcileMsgRep.save(msg);
		long timestamp = System.currentTimeMillis();
		
		service.updateReconciliationMessage(msg, true, uuids);
		
		tableRec = tableRecRepo.getBySiteReconciliationAndTableName(siteRec, table);
		assertEquals(originalProcessedCount + batchSize, tableRec.getProcessedCount());
		assertTrue(tableRec.isCompleted());
		assertTrue(tableRec.isLastBatchReceived());
		long dateChangedMillis = tableRec.getDateChanged().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
		assertTrue(dateChangedMillis == timestamp || dateChangedMillis > timestamp);
	}
	
	@Test
	@Sql(scripts = { "classpath:mgt_site_info.sql", "classpath:mgt_site_reconcile.sql",
	        "classpath:mgt_receiver_table_reconcile.sql" }, config = @SqlConfig(dataSource = MGT_DATASOURCE_NAME, transactionManager = MGT_TX_MGR))
	public void updateReconciliationMessage_shouldRequestForNotFoundUuidsAndUpdateTheProcessedCount() {
		assertEquals(0, requestRepo.count());
		assertEquals(0, missingRepo.count());
		final String uuid1 = "uuid-1";
		final String uuid2 = "uuid-2";
		final String table = "person";
		ReconciliationMessage msg = new ReconciliationMessage();
		final SiteInfo site = siteRepo.getReferenceById(1L);
		msg.setSite(site);
		msg.setTableName(table);
		msg.setBatchSize(10);
		msg.setData("uuid1");
		msg.setDateCreated(new Date());
		reconcileMsgRep.save(msg);
		assertEquals(0, msg.getProcessedCount());
		long timestamp = System.currentTimeMillis();
		
		service.updateReconciliationMessage(msg, false, List.of(uuid1, uuid2));
		
		assertEquals(2, msg.getProcessedCount());
		List<MissingEntity> missingEntities = missingRepo.findAll();
		assertEquals(2, missingEntities.size());
		List<String> missingUuids = missingEntities.stream().map(r -> r.getIdentifier()).collect(Collectors.toList());
		assertTrue(missingUuids.contains(uuid1));
		assertTrue(missingUuids.contains(uuid2));
		for (MissingEntity m : missingEntities) {
			assertEquals(site, m.getSite());
			assertEquals(table, m.getTableName());
			assertTrue(m.getDateCreated().getTime() == timestamp || m.getDateCreated().getTime() > timestamp);
		}
		
		List<ReceiverSyncRequest> requests = requestRepo.findAll();
		assertEquals(2, requests.size());
		List<String> entityUuids = requests.stream().map(r -> r.getIdentifier()).collect(Collectors.toList());
		assertTrue(entityUuids.contains(uuid1));
		assertTrue(entityUuids.contains(uuid2));
		for (ReceiverSyncRequest r : requests) {
			assertEquals(ReceiverRequestStatus.NEW, r.getStatus());
			assertEquals(site, r.getSite());
			assertEquals(table, r.getTableName());
			assertTrue(r.getDateCreated().getTime() == timestamp || r.getDateCreated().getTime() > timestamp);
		}
	}
	
}
