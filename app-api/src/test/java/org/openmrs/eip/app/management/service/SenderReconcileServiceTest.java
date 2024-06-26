package org.openmrs.eip.app.management.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.openmrs.eip.app.SyncConstants.MGT_DATASOURCE_NAME;
import static org.openmrs.eip.app.SyncConstants.MGT_TX_MGR;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.openmrs.eip.app.AppUtils;
import org.openmrs.eip.app.management.entity.sender.SenderReconciliation;
import org.openmrs.eip.app.management.entity.sender.SenderReconciliation.SenderReconcileStatus;
import org.openmrs.eip.app.management.entity.sender.SenderTableReconciliation;
import org.openmrs.eip.app.management.repository.SenderReconcileRepository;
import org.openmrs.eip.app.management.repository.SenderTableReconcileRepository;
import org.openmrs.eip.app.sender.BaseSenderTest;
import org.openmrs.eip.component.repository.PersonNameRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;

public class SenderReconcileServiceTest extends BaseSenderTest {
	
	@Autowired
	private SenderReconcileService service;
	
	@Autowired
	private SenderReconcileRepository recRepo;
	
	@Autowired
	private SenderTableReconcileRepository tableRecRepo;
	
	@Autowired
	private PersonNameRepository nameRepo;
	
	@Test
	@Sql(scripts = { "classpath:openmrs_core_data.sql", "classpath:openmrs_patient.sql" })
	public void takeSnapshot_shouldTakeInitialSnapshotForExistingRowsForEachSyncedTable() {
		assertEquals(0, tableRecRepo.count());
		assertEquals(2, nameRepo.count());
		long timestamp = System.currentTimeMillis();
		
		List<SenderTableReconciliation> recs = service.takeSnapshot();
		
		assertEquals(AppUtils.getTablesToSync().size(), recs.size());
		recs.forEach(r -> {
			if ("person".equalsIgnoreCase(r.getTableName())) {
				assertEquals(2, r.getRowCount());
				assertEquals(101, r.getEndId());
			} else if ("person_name".equalsIgnoreCase(r.getTableName())) {
				assertEquals(2, r.getRowCount());
				assertEquals(2, r.getEndId());
			} else if ("person_attribute".equalsIgnoreCase(r.getTableName())) {
				assertEquals(4, r.getRowCount());
				assertEquals(4, r.getEndId());
			} else if ("patient".equalsIgnoreCase(r.getTableName())) {
				assertEquals(1, r.getRowCount());
				assertEquals(101, r.getEndId());
			} else if ("patient_identifier".equalsIgnoreCase(r.getTableName())) {
				assertEquals(2, r.getRowCount());
				assertEquals(2, r.getEndId());
			} else {
				assertEquals(0, r.getRowCount());
				assertEquals(0, r.getEndId());
			}
			
			assertEquals(0, r.getLastProcessedId());
			long startDateMillis = r.getSnapshotDate().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
			assertTrue(startDateMillis == timestamp || startDateMillis > timestamp);
			assertTrue(r.getDateCreated().getTime() == timestamp || r.getDateCreated().getTime() > timestamp);
		});
	}
	
	@Test
	@Sql(scripts = "classpath:mgt_sender_reconcile.sql", config = @SqlConfig(dataSource = MGT_DATASOURCE_NAME, transactionManager = MGT_TX_MGR))
	public void saveSnapshot_shouldUpdateReconciliationAndSaveAllTheReconciliations() {
		assertEquals(0, tableRecRepo.count());
		SenderTableReconciliation tableRec1 = new SenderTableReconciliation();
		SenderReconciliation rec = recRepo.getReconciliation();
		assertEquals(SenderReconcileStatus.PROCESSING, rec.getStatus());
		tableRec1.setTableName("person");
		tableRec1.setDateCreated(new Date());
		tableRec1.setLastProcessedId(0);
		tableRec1.setRowCount(0);
		tableRec1.setEndId(1);
		tableRec1.setSnapshotDate(LocalDateTime.now());
		SenderTableReconciliation tableRec2 = new SenderTableReconciliation();
		tableRec2.setTableName("visit");
		tableRec2.setDateCreated(new Date());
		tableRec2.setLastProcessedId(0);
		tableRec2.setRowCount(0);
		tableRec2.setEndId(1);
		tableRec2.setSnapshotDate(LocalDateTime.now());
		
		service.saveSnapshot(rec, List.of(tableRec1, tableRec2));
		
		assertEquals(SenderReconcileStatus.PROCESSING, rec.getStatus());
		assertEquals(2, tableRecRepo.count());
	}
	
}
