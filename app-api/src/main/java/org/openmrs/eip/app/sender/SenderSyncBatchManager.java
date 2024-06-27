package org.openmrs.eip.app.sender;

import static org.openmrs.eip.app.SyncConstants.MGT_DATASOURCE_NAME;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.eip.app.AppUtils;
import org.openmrs.eip.app.management.entity.sender.SenderSyncMessage;
import org.openmrs.eip.component.SyncProfiles;
import org.openmrs.eip.component.camel.utils.CamelUtils;
import org.openmrs.eip.component.model.SyncModel;
import org.openmrs.eip.component.utils.JsonUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import jakarta.jms.ConnectionFactory;

@Component("senderSyncBatchManager")
@Profile(SyncProfiles.SENDER)
public class SenderSyncBatchManager extends BaseSyncBatchManager<SenderSyncMessage, SyncModel> {
	
	protected static final int DEFAULT_BATCH_SIZE = 200;
	
	@Value("${" + SenderConstants.PROP_SENDER_ID + "}")
	private String senderId;
	
	@Value("${" + SenderConstants.PROP_JMS_SEND_BATCH_SIZE + ":" + DEFAULT_BATCH_SIZE + "}")
	private int batchSize;
	
	public SenderSyncBatchManager(ConnectionFactory connectionFactory) {
		super(connectionFactory);
	}
	
	@Override
	protected int getBatchSize() {
		return batchSize;
	}
	
	@Override
	protected String getQueueName() {
		return SenderUtils.getQueueName();
	}
	
	@Override
	protected SyncModel convert(SenderSyncMessage message) {
		SyncModel syncModel = JsonUtils.unmarshalSyncModel(message.getData());
		syncModel.getMetadata().setDateSent(LocalDateTime.now());
		syncModel.getMetadata().setSyncVersion(AppUtils.getVersion());
		return syncModel;
	}
	
	@Override
	protected void updateItems(List<Long> messageIds) {
		String myIds = StringUtils.join(messageIds, ",");
		CamelUtils.send("sql:UPDATE sender_sync_message SET status = 'SENT', date_sent = now(), sync_version = '"
		        + AppUtils.getVersion() + "' WHERE id IN (" + myIds + ")?dataSource=#" + MGT_DATASOURCE_NAME);
	}
	
}
