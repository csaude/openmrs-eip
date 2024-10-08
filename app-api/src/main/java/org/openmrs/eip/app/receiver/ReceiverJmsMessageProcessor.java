package org.openmrs.eip.app.receiver;

import static org.openmrs.eip.app.SyncConstants.BEAN_NAME_SYNC_EXECUTOR;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.eip.app.BaseQueueProcessor;
import org.openmrs.eip.app.management.entity.receiver.JmsMessage;
import org.openmrs.eip.app.management.entity.receiver.JmsMessage.MessageType;
import org.openmrs.eip.app.management.service.ReceiverReconcileService;
import org.openmrs.eip.app.management.service.ReceiverService;
import org.openmrs.eip.component.SyncProfiles;
import org.openmrs.eip.component.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Processes a jms message by moving it to the appropriate queue
 */
@Component("receiverJmsMsgProcessor")
@Profile(SyncProfiles.RECEIVER)
public class ReceiverJmsMessageProcessor extends BaseQueueProcessor<JmsMessage> {
	
	protected static final Logger log = LoggerFactory.getLogger(ReceiverJmsMessageProcessor.class);
	
	private ReceiverService receiverService;
	
	private ReceiverReconcileService reconcileService;
	
	public ReceiverJmsMessageProcessor(@Qualifier(BEAN_NAME_SYNC_EXECUTOR) ThreadPoolExecutor executor,
	    ReceiverService receiverService, ReceiverReconcileService reconcileService) {
		super(executor);
		this.receiverService = receiverService;
		this.reconcileService = reconcileService;
	}
	
	@Override
	public String getProcessorName() {
		return "jms msg processor";
	}
	
	@Override
	public String getQueueName() {
		return "jms-msg-processor";
	}
	
	@Override
	public String getUniqueId(JmsMessage item) {
		//Model is null for a sync request and the entity was not found by the remote site.
		if (item.getType() == MessageType.RECONCILE || item.getSyncModel().getModel() == null) {
			//Process messages from same site serially
			return item.getSiteId();
		}
		
		if (item.getType() == MessageType.SYNC) {
			//Process events for different entities in parallel
			return item.getSyncModel().getModel().getUuid();
		}
		
		//Process all messages serially
		//We currently ignore site because it will be null in prod for existing messages at time of upgrade.
		return getLogicalType(item);
	}
	
	@Override
	public String getThreadName(JmsMessage item) {
		String name = StringUtils.isNotBlank(item.getSiteId()) ? item.getSiteId() + "-" : "";
		name += (item.getId().toString());
		return name;
	}
	
	@Override
	public String getLogicalType(JmsMessage item) {
		if (item.getType() == MessageType.SYNC && item.getSyncModel().getModel() != null) {
			return item.getSyncModel().getTableToSyncModelClass().getName();
		}
		
		return item.getType().name();
	}
	
	@Override
	public List<String> getLogicalTypeHierarchy(String logicalType) {
		if (MessageType.RECONCILE.name().equals(logicalType) || MessageType.SYNC.name().equals(logicalType)) {
			return null;
		}
		
		return Utils.getListOfModelClassHierarchy(logicalType);
	}
	
	@Override
	public void processItem(JmsMessage item) {
		if (item.getType() == MessageType.SYNC) {
			receiverService.processJmsMessage(item);
		} else if (item.getType() == MessageType.RECONCILE) {
			reconcileService.processJmsMessage(item);
		}
	}
	
}
