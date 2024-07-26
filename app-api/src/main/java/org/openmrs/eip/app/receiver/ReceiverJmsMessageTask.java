package org.openmrs.eip.app.receiver;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.eip.app.AppUtils;
import org.openmrs.eip.app.BaseQueueTask;
import org.openmrs.eip.app.SyncConstants;
import org.openmrs.eip.app.management.entity.receiver.JmsMessage;
import org.openmrs.eip.app.management.repository.JmsMessageRepository;
import org.openmrs.eip.component.SyncContext;
import org.openmrs.eip.component.model.SyncMetadata;
import org.openmrs.eip.component.model.SyncModel;
import org.openmrs.eip.component.utils.JsonUtils;

/**
 * Reads a batch of JmsMessages and submits them to the {@link ReceiverJmsMessageProcessor} for
 * processing.
 */
public class ReceiverJmsMessageTask extends BaseQueueTask<JmsMessage> {
	
	private static final String SYNC_INSERT = "INSERT INTO receiver_sync_msg (model_class_name,identifier,"
	        + "entity_payload,site_id,is_snapshot,message_uuid,date_sent_by_sender,operation,date_created,"
	        + "date_received,sync_version) VALUES (?,?,?,?,?,?,?,?,now(),?,?)";
	
	private JmsMessageRepository repo;
	
	private DataSource dataSource;
	
	public ReceiverJmsMessageTask() {
		this.repo = SyncContext.getBean(JmsMessageRepository.class);
		this.dataSource = SyncContext.getBean(SyncConstants.MGT_DATASOURCE_NAME);
	}
	
	@Override
	public String getTaskName() {
		return "jms msg task";
	}
	
	@Override
	public List<JmsMessage> getNextBatch() {
		//TODO Only process sync messages that are not requests
		return repo.findAll(AppUtils.getTaskPage()).getContent();
	}
	
	@Override
	public void process(List<JmsMessage> items) throws Exception {
		try (Connection conn = dataSource.getConnection();
		        PreparedStatement insertStmt = conn.prepareStatement(SYNC_INSERT);
		        Statement deleteStmt = conn.createStatement()) {
			boolean autoCommit = conn.getAutoCommit();
			try {
				conn.setAutoCommit(false);
				List<Long> ids = new ArrayList<>();
				for (JmsMessage jmsMessage : items) {
					String body = new String(jmsMessage.getBody(), StandardCharsets.UTF_8);
					SyncModel syncModel = JsonUtils.unmarshalSyncModel(body);
					SyncMetadata md = syncModel.getMetadata();
					insertStmt.setString(1, syncModel.getTableToSyncModelClass().getName());
					insertStmt.setString(2, syncModel.getModel().getUuid());
					insertStmt.setString(3, body);
					insertStmt.setLong(4, ReceiverContext.getSiteInfo(md.getSourceIdentifier()).getId());
					insertStmt.setBoolean(5, md.getSnapshot());
					insertStmt.setString(6, md.getMessageUuid());
					insertStmt.setObject(7, md.getDateSent());
					insertStmt.setString(8, md.getOperation());
					insertStmt.setObject(9, jmsMessage.getDateCreated());
					insertStmt.setString(10, md.getSyncVersion());
					ids.add(jmsMessage.getId());
					insertStmt.addBatch();
				}
				
				if (log.isDebugEnabled()) {
					log.debug("Saving sync messages in batch");
				}
				
				int[] rows = insertStmt.executeBatch();
				int count = items.size();
				if (rows.length != count) {
					throw new Exception("Expected " + count + " sync items to be inserted but was " + rows.length);
				}
				
				if (log.isDebugEnabled()) {
					log.debug("Removing JMS message in batch");
				}
				
				int deleted = deleteStmt
				        .executeUpdate("DELETE FROM jms_msg WHERE id IN (" + StringUtils.join(ids, ",") + ")");
				if (deleted != count) {
					throw new Exception("Expected " + count + " JMS messages to be deleted but was " + deleted);
				}
			}
			finally {
				conn.setAutoCommit(autoCommit);
			}
		}
	}
	
}
