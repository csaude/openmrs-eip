package org.openmrs.eip.app.sender;

import static org.openmrs.eip.app.SyncConstants.BEAN_NAME_SYNC_EXECUTOR;
import static org.openmrs.eip.component.SyncOperation.d;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

import org.openmrs.eip.app.AppUtils;
import org.openmrs.eip.app.BaseQueueProcessor;
import org.openmrs.eip.app.management.entity.sender.SenderSyncMessage;
import org.openmrs.eip.app.management.repository.SenderSyncMessageRepository;
import org.openmrs.eip.component.SyncProfiles;
import org.openmrs.eip.component.model.SyncModel;
import org.openmrs.eip.component.utils.JsonUtils;
import org.openmrs.eip.component.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Component("senderSyncMsgProcessor")
@Profile(SyncProfiles.SENDER)
public class SenderSyncMessageProcessor extends BaseQueueProcessor<SenderSyncMessage> {
	
	private static final Logger LOG = LoggerFactory.getLogger(SenderSyncMessageProcessor.class);
	
	@Value("${" + SenderConstants.PROP_SENDER_ID + "}")
	private String senderId;
	
	@Value("${" + SenderConstants.PROP_JMS_SEND_BATCH_DISABLED + ":false}")
	private boolean batchDisabled;
	
	private JmsTemplate jmsTemplate;
	
	private SenderSyncMessageRepository repo;
	
	private SenderSyncBatchManager batchManager;
	
	public SenderSyncMessageProcessor(@Qualifier(BEAN_NAME_SYNC_EXECUTOR) ThreadPoolExecutor executor,
	    JmsTemplate jmsTemplate, SenderSyncMessageRepository repo, SenderSyncBatchManager batchManager) {
		super(executor);
		this.jmsTemplate = jmsTemplate;
		this.repo = repo;
		this.batchManager = batchManager;
	}
	
	@Override
	public String getProcessorName() {
		return "sync msg";
	}
	
	@Override
	public String getThreadName(SenderSyncMessage msg) {
		return msg.getTableName() + "-" + msg.getIdentifier() + "-" + msg.getMessageUuid();
	}
	
	@Override
	public String getUniqueId(SenderSyncMessage item) {
		return item.getIdentifier();
	}
	
	@Override
	public String getQueueName() {
		return "sync-msg";
	}
	
	@Override
	public String getLogicalType(SenderSyncMessage item) {
		return item.getTableName();
	}
	
	@Override
	public List<String> getLogicalTypeHierarchy(String logicalType) {
		return Utils.getListOfTablesInHierarchy(logicalType);
	}
	
	@Override
	public void processWork(List<SenderSyncMessage> items) throws Exception {
		//Squash events for the same row so that exactly one message is processed in case of multiple in this run in.
		//Delete being a terminal event, squash for a single entity will stop at the last event before a delete event
		//to ensure we don't re-process a non-existent row
		Map<String, SenderSyncMessage> keyAndLatestMap = new LinkedHashMap(items.size());
		List<SenderSyncMessage> squashedMsgs = new ArrayList();
		items.stream().forEach(msg -> {
			String table = msg.getTableName();
			String key = table + "#" + msg.getIdentifier();
			if (keyAndLatestMap.containsKey(key) && d.name().equals(msg.getOperation())) {
				if (LOG.isTraceEnabled()) {
					LOG.trace("Squashing stopped for {}, postponing processing of delete event: {}", key, msg);
				}
			} else {
				SenderSyncMessage previousMsg = keyAndLatestMap.put(key, msg);
				if (previousMsg != null) {
					squashedMsgs.add(previousMsg);
					
					if (LOG.isTraceEnabled()) {
						LOG.trace("Squashing entity event: {}", previousMsg);
					}
				}
			}
		});
		
		doProcessWork(keyAndLatestMap.values().stream().toList());
		repo.deleteAll(squashedMsgs);
	}
	
	@Override
	public void processItem(SenderSyncMessage syncMsg) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Preparing sync payload to send");
		}
		
		if (!batchDisabled) {
			batchManager.add(syncMsg);
		} else {
			SyncModel syncModel = JsonUtils.unmarshalSyncModel(syncMsg.getData());
			syncModel.getMetadata().setDateSent(LocalDateTime.now());
			syncModel.getMetadata().setSyncVersion(AppUtils.getVersion());
			String syncData = JsonUtils.marshall(syncModel);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Sync payload -> " + syncData);
			}
			
			jmsTemplate.send(SenderUtils.getQueueName(),
			    new SyncMessageCreator(syncData, syncMsg.getMessageUuid(), senderId));
			
			if (LOG.isDebugEnabled()) {
				LOG.debug("Sync payload sent!");
			}
			
			syncMsg.setSyncVersion(syncModel.getMetadata().getSyncVersion());
			syncMsg.markAsSent(syncModel.getMetadata().getDateSent());
			
			if (LOG.isDebugEnabled()) {
				LOG.debug("Updating sender sync message status to " + syncMsg.getStatus());
			}
			
			repo.save(syncMsg);
			
			if (LOG.isDebugEnabled()) {
				LOG.debug("Successfully sent and updated status for sync message");
			}
		}
	}
	
	@Override
	protected void flush() {
		if (!batchDisabled) {
			batchManager.send(true);
		}
	}
	
	protected void doProcessWork(List<SenderSyncMessage> items) throws Exception {
		super.processWork(items);
	}
	
}
