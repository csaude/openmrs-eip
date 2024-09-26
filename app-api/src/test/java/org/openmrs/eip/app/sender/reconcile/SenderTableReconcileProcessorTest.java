package org.openmrs.eip.app.sender.reconcile;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.openmrs.eip.app.SyncConstants.RECONCILE_MSG_SEPARATOR;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openmrs.eip.app.BaseQueueProcessor;
import org.openmrs.eip.app.management.entity.ReconciliationResponse;
import org.openmrs.eip.app.management.entity.sender.SenderReconcileMessage;
import org.openmrs.eip.app.management.entity.sender.SenderReconciliation;
import org.openmrs.eip.app.management.entity.sender.SenderTableReconciliation;
import org.openmrs.eip.app.management.repository.SenderReconcileMsgRepository;
import org.openmrs.eip.app.management.repository.SenderReconcileRepository;
import org.openmrs.eip.app.management.repository.SenderTableReconcileRepository;
import org.openmrs.eip.component.SyncContext;
import org.openmrs.eip.component.repository.SyncEntityRepository;
import org.openmrs.eip.component.utils.JsonUtils;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.springframework.data.domain.Pageable;

@RunWith(PowerMockRunner.class)
@PrepareForTest(SyncContext.class)
public class SenderTableReconcileProcessorTest {
	
	private static final String RECONCILIATION_ID = "test_identifier";
	
	private static final int BATCH_SIZE = 5;
	
	@Mock
	private SenderTableReconcileRepository mockTableRecRepo;
	
	@Mock
	private SenderReconcileRepository mockRecRepo;
	
	@Mock
	private SenderReconcileMsgRepository mockReconcileMsgRepo;
	
	@Mock
	private SyncEntityRepository mockOpenmrsRepo;
	
	@Mock
	private SenderReconciliation mockReconciliation;
	
	private SenderTableReconcileProcessor processor;
	
	@Before
	public void setup() {
		PowerMockito.mockStatic(SyncContext.class);
		Whitebox.setInternalState(BaseQueueProcessor.class, "initialized", true);
		processor = new SenderTableReconcileProcessor(null, mockTableRecRepo, mockRecRepo, mockReconcileMsgRepo);
		when(mockReconciliation.getBatchSize()).thenReturn(BATCH_SIZE);
		when(mockRecRepo.getReconciliation()).thenReturn(mockReconciliation);
		when(mockReconciliation.getIdentifier()).thenReturn(RECONCILIATION_ID);
	}
	
	@After
	public void tearDown() {
		setInternalState(BaseQueueProcessor.class, "initialized", false);
	}
	
	private SenderTableReconciliation createReconciliation(String table, long rowCount, long endId, long lastProcId,
	                                                       boolean started) {
		SenderTableReconciliation rec = new SenderTableReconciliation();
		rec.setTableName(table);
		rec.setRowCount(rowCount);
		rec.setEndId(endId);
		rec.setLastProcessedId(lastProcId);
		rec.setStarted(started);
		return rec;
	}
	
	@Test
	public void processItem_shouldProcessBatchWithNoRowsRead() {
		final String table = "person";
		final long rowCount = 10;
		final long endId = 20;
		SenderTableReconciliation rec = createReconciliation(table, rowCount, endId, 20, false);
		LocalDateTime snapshotDate = LocalDateTime.now();
		rec.setSnapshotDate(snapshotDate);
		Assert.assertFalse(rec.isStarted());
		when(SyncContext.getRepositoryBean(table)).thenReturn(mockOpenmrsRepo);
		long timestamp = System.currentTimeMillis();
		
		processor.processItem(rec);
		
		Mockito.verify(mockTableRecRepo).save(rec);
		Assert.assertTrue(rec.isStarted());
		assertEquals(endId, rec.getLastProcessedId());
		ReconciliationResponse expectedResp = new ReconciliationResponse();
		expectedResp.setIdentifier(RECONCILIATION_ID);
		expectedResp.setTableName(table);
		expectedResp.setRemoteStartDate(snapshotDate);
		expectedResp.setRowCount(rowCount);
		expectedResp.setBatchSize(0);
		expectedResp.setData("");
		ArgumentCaptor<SenderReconcileMessage> argCaptor = ArgumentCaptor.forClass(SenderReconcileMessage.class);
		Mockito.verify(mockReconcileMsgRepo).save(argCaptor.capture());
		assertArrayEquals(JsonUtils.marshalToBytes(expectedResp), argCaptor.getValue().getBody());
		assertTrue(argCaptor.getValue().getDateCreated().getTime() == timestamp
		        || argCaptor.getValue().getDateCreated().getTime() > timestamp);
	}
	
	@Test
	public void processItem_shouldReadAndSubmitABatchOfUuids() {
		final String table = "person";
		final long rowCount = 20;
		final long endId = 25;
		final long lastProcId = 10;
		SenderTableReconciliation rec = createReconciliation(table, rowCount, endId, lastProcId, true);
		when(SyncContext.getRepositoryBean(table)).thenReturn(mockOpenmrsRepo);
		Pageable page = Pageable.ofSize(BATCH_SIZE);
		List<Object[]> batch = new ArrayList<>();
		batch.add(new Object[] { 11L, "uuid-11" });
		batch.add(new Object[] { 12L, "uuid-12" });
		batch.add(new Object[] { 13L, "uuid-13" });
		batch.add(new Object[] { 14L, "uuid-14" });
		batch.add(new Object[] { 15L, "uuid-15" });
		when(mockOpenmrsRepo.getIdAndUuidBatchToReconcile(lastProcId, endId, page)).thenReturn(batch);
		long timestamp = System.currentTimeMillis();
		
		processor.processItem(rec);
		
		Mockito.verify(mockTableRecRepo).save(rec);
		assertEquals(15L, rec.getLastProcessedId());
		ReconciliationResponse expectedResp = new ReconciliationResponse();
		expectedResp.setIdentifier(RECONCILIATION_ID);
		expectedResp.setTableName(table);
		expectedResp.setBatchSize(batch.size());
		List<String> uuids = batch.stream().map(entry -> entry[1].toString()).collect(Collectors.toList());
		expectedResp.setData(StringUtils.join(uuids, RECONCILE_MSG_SEPARATOR));
		ArgumentCaptor<SenderReconcileMessage> argCaptor = ArgumentCaptor.forClass(SenderReconcileMessage.class);
		Mockito.verify(mockReconcileMsgRepo).save(argCaptor.capture());
		assertArrayEquals(JsonUtils.marshalToBytes(expectedResp), argCaptor.getValue().getBody());
		assertTrue(argCaptor.getValue().getDateCreated().getTime() == timestamp
		        || argCaptor.getValue().getDateCreated().getTime() > timestamp);
	}
	
	@Test
	public void processItem_shouldFinalizeReconciliation() {
		final String table = "person";
		final long rowCount = 15;
		final long endId = 15;
		final long lastProcId = 10;
		SenderTableReconciliation rec = createReconciliation(table, rowCount, endId, lastProcId, true);
		when(SyncContext.getRepositoryBean(table)).thenReturn(mockOpenmrsRepo);
		Pageable page = Pageable.ofSize(BATCH_SIZE);
		List<Object[]> batch = new ArrayList<>();
		batch.add(new Object[] { 11L, "uuid-11" });
		batch.add(new Object[] { 12L, "uuid-12" });
		batch.add(new Object[] { 13L, "uuid-13" });
		batch.add(new Object[] { 14L, "uuid-14" });
		batch.add(new Object[] { 15L, "uuid-15" });
		when(mockOpenmrsRepo.getIdAndUuidBatchToReconcile(lastProcId, endId, page)).thenReturn(batch);
		long timestamp = System.currentTimeMillis();
		
		processor.processItem(rec);
		
		Mockito.verify(mockTableRecRepo).save(rec);
		assertEquals(15L, rec.getLastProcessedId());
		ReconciliationResponse expectedResp = new ReconciliationResponse();
		expectedResp.setIdentifier(RECONCILIATION_ID);
		expectedResp.setTableName(table);
		expectedResp.setBatchSize(batch.size());
		List<String> uuids = batch.stream().map(entry -> entry[1].toString()).collect(Collectors.toList());
		expectedResp.setData(StringUtils.join(uuids, RECONCILE_MSG_SEPARATOR));
		ArgumentCaptor<SenderReconcileMessage> argCaptor = ArgumentCaptor.forClass(SenderReconcileMessage.class);
		Mockito.verify(mockReconcileMsgRepo).save(argCaptor.capture());
		assertArrayEquals(JsonUtils.marshalToBytes(expectedResp), argCaptor.getValue().getBody());
		assertTrue(argCaptor.getValue().getDateCreated().getTime() == timestamp
		        || argCaptor.getValue().getDateCreated().getTime() > timestamp);
	}
	
}
