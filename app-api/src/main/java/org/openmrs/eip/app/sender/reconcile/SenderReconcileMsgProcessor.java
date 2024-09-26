package org.openmrs.eip.app.sender.reconcile;

import static org.openmrs.eip.app.SyncConstants.BEAN_NAME_SYNC_EXECUTOR;

import java.util.concurrent.ThreadPoolExecutor;

import org.openmrs.eip.app.BasePureParallelQueueProcessor;
import org.openmrs.eip.app.management.entity.sender.SenderReconcileMessage;
import org.openmrs.eip.app.management.repository.SenderReconcileMsgRepository;
import org.openmrs.eip.app.sender.SenderConstants;
import org.openmrs.eip.component.SyncProfiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

/**
 * Processes a SenderReconcileMessage item
 */
@Component("senderReconcileMsgProcessor")
@Profile(SyncProfiles.SENDER)
public class SenderReconcileMsgProcessor extends BasePureParallelQueueProcessor<SenderReconcileMessage> {
	
	private static final Logger LOG = LoggerFactory.getLogger(SenderReconcileMsgProcessor.class);
	
	private SenderReconcileMsgRepository repo;
	
	private JmsTemplate jmsTemplate;
	
	@Value("${" + SenderConstants.PROP_SENDER_ID + "}")
	private String siteId;
	
	public SenderReconcileMsgProcessor(@Qualifier(BEAN_NAME_SYNC_EXECUTOR) ThreadPoolExecutor executor,
	    SenderReconcileMsgRepository repo, JmsTemplate jmsTemplate) {
		super(executor);
		this.repo = repo;
		this.jmsTemplate = jmsTemplate;
	}
	
	@Override
	public String getProcessorName() {
		return "reconcile msg";
	}
	
	@Override
	public String getQueueName() {
		return "reconcile msg";
	}
	
	@Override
	public String getThreadName(SenderReconcileMessage item) {
		return item.getId().toString();
	}
	
	@Override
	public void processItem(SenderReconcileMessage item) {
		if (LOG.isTraceEnabled()) {
			LOG.trace("");
		}
		
		jmsTemplate.send(new ReconcileResponseCreator(item.getBody(), siteId));
		repo.delete(item);
	}
	
}
