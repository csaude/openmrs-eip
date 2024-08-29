package org.openmrs.eip.app.receiver;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.openmrs.eip.app.AppUtils;
import org.openmrs.eip.app.BaseMovingTask;
import org.openmrs.eip.app.management.entity.receiver.ReceiverSyncArchive;
import org.openmrs.eip.app.management.repository.ReceiverSyncArchiveRepository;
import org.openmrs.eip.component.SyncContext;
import org.openmrs.eip.component.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads a batch of sync archives that are older than a specific age in days and moves them to the
 * receiver_pruned_item table.
 */
public class ReceiverArchivePruningTask extends BaseMovingTask<ReceiverSyncArchive> {
	
	private static final Logger LOG = LoggerFactory.getLogger(ReceiverArchivePruningTask.class);
	
	protected static final String PRUNE_INSERT = "INSERT INTO receiver_pruned_item (model_class_name,identifier,"
	        + "entity_payload,site_id,is_snapshot,message_uuid,date_sent_by_sender,operation,date_created,"
	        + "date_received,sync_version) VALUES (?,?,?,?,?,?,?,?,now(),?,?)";
	
	private ReceiverSyncArchiveRepository repo;
	
	private int maxAgeDays;
	
	public ReceiverArchivePruningTask(int maxAgeDays) {
		this.maxAgeDays = maxAgeDays;
		this.repo = SyncContext.getBean(ReceiverSyncArchiveRepository.class);
	}
	
	@Override
	public String getTaskName() {
		return "prune task";
	}
	
	@Override
	public List<ReceiverSyncArchive> getNextBatch() {
		Date maxDateCreated = DateUtils.subtractDays(new Date(), maxAgeDays);
		LOG.info("Pruning sync archives created on or before: " + maxDateCreated);
		
		return repo.findByDateCreatedLessThanEqual(maxDateCreated, AppUtils.getTaskPage());
	}
	
	@Override
	protected String getInsertQuery() {
		return PRUNE_INSERT;
	}
	
	@Override
	protected void addItem(PreparedStatement insertStatement, ReceiverSyncArchive item) throws SQLException {
		insertStatement.setString(1, item.getModelClassName());
		insertStatement.setString(2, item.getIdentifier());
		insertStatement.setString(3, item.getEntityPayload());
		insertStatement.setLong(4, item.getSite().getId());
		insertStatement.setBoolean(5, item.getSnapshot());
		insertStatement.setString(6, item.getMessageUuid());
		insertStatement.setObject(7, item.getDateSentBySender());
		insertStatement.setString(8, item.getOperation().name());
		insertStatement.setObject(9, item.getDateReceived());
	}
}
