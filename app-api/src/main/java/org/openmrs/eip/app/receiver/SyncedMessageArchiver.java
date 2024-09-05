package org.openmrs.eip.app.receiver;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import org.openmrs.eip.app.AppUtils;
import org.openmrs.eip.app.BaseMovingTask;
import org.openmrs.eip.app.management.entity.receiver.SyncedMessage;
import org.openmrs.eip.app.management.repository.SyncedMessageRepository;
import org.openmrs.eip.component.SyncContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads and processes done messages in the synced message queue and moves them to the archives
 * queue
 */
public class SyncedMessageArchiver extends BaseMovingTask<SyncedMessage> {
	
	private static final Logger LOG = LoggerFactory.getLogger(SyncedMessageArchiver.class);
	
	protected static final String ARCHIVE_INSERT = "INSERT INTO receiver_sync_archive (model_class_name,identifier,"
	        + "entity_payload,site_id,is_snapshot,message_uuid,date_sent_by_sender,operation,date_created,"
	        + "date_received) VALUES (?,?,?,?,?,?,?,?,now(),?)";
	
	private SyncedMessageRepository syncedMsgRepo;
	
	public SyncedMessageArchiver() {
		syncedMsgRepo = SyncContext.getBean(SyncedMessageRepository.class);
	}
	
	@Override
	public String getTaskName() {
		return "msg archiver task";
	}
	
	@Override
	public List<SyncedMessage> getNextBatch() {
		if (LOG.isTraceEnabled()) {
			LOG.trace("Fetching next batch of " + AppUtils.getTaskPage().getPageSize() + " synced items to archive");
		}
		
		return syncedMsgRepo.getBatchOfMessagesForArchiving(AppUtils.getTaskPage());
	}
	
	@Override
	protected String getInsertQuery() {
		return ARCHIVE_INSERT;
	}
	
	@Override
	protected void addItem(PreparedStatement insertStatement, SyncedMessage item) throws SQLException {
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
