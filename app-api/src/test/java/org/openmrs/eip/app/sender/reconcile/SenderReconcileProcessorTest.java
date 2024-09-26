package org.openmrs.eip.app.sender.reconcile;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.openmrs.eip.app.SyncConstants.RECONCILE_MSG_SEPARATOR;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openmrs.eip.app.AppUtils;
import org.openmrs.eip.app.BaseQueueProcessor;
import org.openmrs.eip.app.management.entity.ReconciliationResponse;
import org.openmrs.eip.app.management.entity.sender.DeletedEntity;
import org.openmrs.eip.app.management.entity.sender.SenderReconcileMessage;
import org.openmrs.eip.app.management.entity.sender.SenderReconciliation;
import org.openmrs.eip.app.management.entity.sender.SenderReconciliation.SenderReconcileStatus;
import org.openmrs.eip.app.management.entity.sender.SenderTableReconciliation;
import org.openmrs.eip.app.management.repository.DeletedEntityRepository;
import org.openmrs.eip.app.management.repository.SenderReconcileMsgRepository;
import org.openmrs.eip.app.management.repository.SenderReconcileRepository;
import org.openmrs.eip.app.management.repository.SenderTableReconcileRepository;
import org.openmrs.eip.app.management.service.SenderReconcileService;
import org.openmrs.eip.component.exception.EIPException;
import org.openmrs.eip.component.utils.JsonUtils;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@RunWith(PowerMockRunner.class)
@PrepareForTest(AppUtils.class)
public class SenderReconcileProcessorTest {
	
	private static final String RECONCILE_ID = "testId";
	
	@Mock
	private SenderReconcileService mockService;
	
	@Mock
	private SenderReconcileRepository mockRecRepo;
	
	@Mock
	private SenderTableReconcileRepository mockTableRecRepo;
	
	@Mock
	private DeletedEntityRepository mockDeleteRepo;
	
	@Mock
	private SenderReconcileMsgRepository mockReconcileMsgRepo;
	
	private SenderReconcileProcessor processor;
	
	@Before
	public void setup() {
		PowerMockito.mockStatic(AppUtils.class);
		Whitebox.setInternalState(BaseQueueProcessor.class, "initialized", true);
		processor = new SenderReconcileProcessor(null, mockRecRepo, mockTableRecRepo, mockDeleteRepo, mockService,
		        mockReconcileMsgRepo);
	}
	
	@After
	public void tearDown() {
		setInternalState(BaseQueueProcessor.class, "initialized", false);
	}
	
	@Test
	public void processItem_shouldTakeSnapshotAndSaveItIfStatusIsNew() {
		SenderReconciliation rec = new SenderReconciliation();
		rec.setStatus(SenderReconcileStatus.NEW);
		List<SenderTableReconciliation> tableRecs = List.of(new SenderTableReconciliation());
		when(mockService.takeSnapshot()).thenReturn(tableRecs);
		
		processor.processItem(rec);
		
		verify(mockService).saveSnapshot(rec, tableRecs);
	}
	
	@Test
	public void processItem_shouldFinalizeACompletedReconciliation() {
		SenderReconciliation rec = new SenderReconciliation();
		rec.setStatus(SenderReconcileStatus.PROCESSING);
		final String personTable = "person";
		final String visitTable = "visit";
		when(AppUtils.getTablesToSync()).thenReturn(Set.of(personTable, visitTable));
		SenderTableReconciliation personRec = Mockito.mock(SenderTableReconciliation.class);
		when(personRec.isCompleted()).thenReturn(true);
		SenderTableReconciliation visitRec = Mockito.mock(SenderTableReconciliation.class);
		when(visitRec.isCompleted()).thenReturn(true);
		when(mockTableRecRepo.getByTableNameIgnoreCase(personTable)).thenReturn(personRec);
		when(mockTableRecRepo.getByTableNameIgnoreCase(visitTable)).thenReturn(visitRec);
		
		processor.processItem(rec);
		
		assertEquals(SenderReconcileStatus.POST_PROCESSING, rec.getStatus());
		verify(mockRecRepo).save(rec);
	}
	
	@Test
	public void processItem_shouldNotFinalizeANonCompletedReconciliation() {
		SenderReconciliation rec = new SenderReconciliation();
		rec.setStatus(SenderReconcileStatus.PROCESSING);
		final String personTable = "person";
		final String visitTable = "visit";
		when(AppUtils.getTablesToSync()).thenReturn(Set.of(personTable, visitTable));
		SenderTableReconciliation personRec = Mockito.mock(SenderTableReconciliation.class);
		when(personRec.isCompleted()).thenReturn(true);
		SenderTableReconciliation visitRec = Mockito.mock(SenderTableReconciliation.class);
		when(mockTableRecRepo.getByTableNameIgnoreCase(personTable)).thenReturn(personRec);
		when(mockTableRecRepo.getByTableNameIgnoreCase(visitTable)).thenReturn(visitRec);
		
		processor.processItem(rec);
		
		assertEquals(SenderReconcileStatus.PROCESSING, rec.getStatus());
		Mockito.verifyNoInteractions(mockRecRepo);
	}
	
	@Test
	public void processItem_shouldFailForACompletedReconciliation() {
		SenderReconciliation rec = new SenderReconciliation();
		rec.setStatus(SenderReconcileStatus.COMPLETED);
		EIPException ex = Assert.assertThrows(EIPException.class, () -> processor.processItem(rec));
		assertEquals("Reconciliation is already completed", ex.getMessage());
	}
	
	@Test
	public void send_shouldGenerateAndSendReconciliationResponse() {
		final String table = "person";
		List<String> uuids = List.of("person-uuid1", "person-uuid2");
		SenderReconciliation rec = new SenderReconciliation();
		rec.setIdentifier(RECONCILE_ID);
		ArgumentCaptor<SenderReconcileMessage> creatorArgCaptor = ArgumentCaptor.forClass(SenderReconcileMessage.class);
		long timestamp = System.currentTimeMillis();
		
		processor.send(rec, table, uuids, true);
		
		verify(mockReconcileMsgRepo).save(creatorArgCaptor.capture());
		SenderReconcileMessage msg = creatorArgCaptor.getValue();
		ReconciliationResponse response = new ReconciliationResponse();
		response.setIdentifier(RECONCILE_ID);
		response.setTableName(table);
		response.setBatchSize(uuids.size());
		response.setLastTableBatch(true);
		response.setData(StringUtils.join(uuids, RECONCILE_MSG_SEPARATOR));
		assertArrayEquals(JsonUtils.marshalToBytes(response), msg.getBody());
		assertTrue(msg.getDateCreated().getTime() == timestamp || msg.getDateCreated().getTime() > timestamp);
	}
	
	@Test
	public void processItem_shouldPostProcessTheReconciliation() {
		SenderReconciliation rec = new SenderReconciliation();
		rec.setStatus(SenderReconcileStatus.POST_PROCESSING);
		processor = spy(processor);
		doNothing().when(processor).sendDeletedUuids(rec);
		doNothing().when(processor).sendAllDeletedUuids(rec);
		
		processor.processItem(rec);
		
		verify(processor).sendDeletedUuids(rec);
		verify(processor).sendAllDeletedUuids(rec);
		assertEquals(SenderReconcileStatus.COMPLETED, rec.getStatus());
		verify(mockRecRepo).save(rec);
	}
	
	@Test
	public void sendDeletedUuids_shouldOnlySendUuidsForEntitiesDeletedDuringReconciliation() {
		final String personTable = "person";
		final String visitTable = "visit";
		final Long personId1 = 1L;
		final Long personId2 = 2L;
		final Long personId3 = 3L;
		final String personUuid1 = "person-uuid1";
		final String personUuid2 = "person-uuid2";
		final String personUuid3 = "person-uuid3";
		final Date recDate = new Date();
		SenderReconciliation rec = new SenderReconciliation();
		rec.setStatus(SenderReconcileStatus.POST_PROCESSING);
		rec.setDateCreated(recDate);
		SenderTableReconciliation personRec = new SenderTableReconciliation();
		personRec.setTableName(personTable);
		personRec.setEndId(personId2);
		SenderTableReconciliation visitRec = new SenderTableReconciliation();
		visitRec.setTableName(visitTable);
		when(mockTableRecRepo.findAll()).thenReturn(List.of(personRec, visitRec));
		DeletedEntity deletedPerson1 = new DeletedEntity();
		deletedPerson1.setPrimaryKeyId(personId1.toString());
		deletedPerson1.setIdentifier(personUuid1);
		DeletedEntity deletedPerson2 = new DeletedEntity();
		deletedPerson2.setPrimaryKeyId(personId2.toString());
		deletedPerson2.setIdentifier(personUuid2);
		DeletedEntity deletedPerson3 = new DeletedEntity();
		deletedPerson3.setPrimaryKeyId(personId3.toString());
		deletedPerson3.setIdentifier(personUuid3);
		List<DeletedEntity> deletedPersons = List.of(deletedPerson1, deletedPerson2, deletedPerson3);
		when(mockDeleteRepo.getByTableNameIgnoreCaseAndDateCreatedGreaterThanEqual(personTable, recDate))
		        .thenReturn(deletedPersons);
		processor = spy(processor);
		doNothing().when(processor).send(eq(rec), anyString(), anyList(), anyBoolean());
		
		processor.sendDeletedUuids(rec);
		
		verify(processor).send(rec, personTable, List.of(personUuid1, personUuid2), false);
		verify(mockDeleteRepo).getByTableNameIgnoreCaseAndDateCreatedGreaterThanEqual(visitTable, recDate);
	}
	
	@Test
	public void sendAllDeletedUuids_shouldSendAllDeletedUuids() {
		final String personTable = "person";
		final String visitTable = "visit";
		final Long personId1 = 1L;
		final Long personId2 = 2L;
		final String personUuid1 = "person-uuid1";
		final String personUuid2 = "person-uuid2";
		final Date recDate = new Date();
		SenderReconciliation rec = new SenderReconciliation();
		rec.setStatus(SenderReconcileStatus.POST_PROCESSING);
		rec.setDateCreated(recDate);
		SenderTableReconciliation personRec = new SenderTableReconciliation();
		personRec.setTableName(personTable);
		personRec.setEndId(personId2);
		SenderTableReconciliation visitRec = new SenderTableReconciliation();
		visitRec.setTableName(visitTable);
		when(mockTableRecRepo.findAll()).thenReturn(List.of(personRec, visitRec));
		DeletedEntity deletedPerson1 = new DeletedEntity();
		deletedPerson1.setPrimaryKeyId(personId1.toString());
		deletedPerson1.setIdentifier(personUuid1);
		DeletedEntity deletedPerson2 = new DeletedEntity();
		deletedPerson2.setPrimaryKeyId(personId2.toString());
		deletedPerson2.setIdentifier(personUuid2);
		List<DeletedEntity> deletedPersons = List.of(deletedPerson1, deletedPerson2);
		Set<String> syncTables = new LinkedHashSet<>();
		syncTables.add(personTable);
		syncTables.add(visitTable);
		when(AppUtils.getTablesToSync()).thenReturn(syncTables);
		when(mockDeleteRepo.getByTableNameIgnoreCase(personTable)).thenReturn(deletedPersons);
		processor = spy(processor);
		doNothing().when(processor).send(eq(rec), anyString(), anyList(), anyBoolean());
		
		processor.sendAllDeletedUuids(rec);
		
		verify(processor, times(2)).send(eq(rec), anyString(), anyList(), anyBoolean());
		verify(processor).send(rec, personTable, List.of(personUuid1, personUuid2), true);
		verify(processor).send(rec, visitTable, Collections.emptyList(), true);
		verify(mockDeleteRepo).getByTableNameIgnoreCase(personTable);
		verify(mockDeleteRepo).getByTableNameIgnoreCase(visitTable);
	}
	
}
