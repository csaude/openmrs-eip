package org.openmrs.eip.app.receiver.task;

import java.util.List;

import org.openmrs.eip.app.AppUtils;
import org.openmrs.eip.app.BaseDelegatingQueueTask;
import org.openmrs.eip.app.management.entity.receiver.JmsMessage;
import org.openmrs.eip.app.management.entity.receiver.JmsMessage.MessageType;
import org.openmrs.eip.app.management.repository.JmsMessageRepository;
import org.openmrs.eip.app.receiver.ReceiverJmsMessageProcessor;
import org.openmrs.eip.component.SyncContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads a batch of reconcile JmsMessages and submits them to the
 * {@link ReceiverJmsMessageProcessor} for processing.
 */
public class ReceiverReconcileJmsMessageTask extends BaseDelegatingQueueTask<JmsMessage, ReceiverJmsMessageProcessor> {
	
	protected static final Logger log = LoggerFactory.getLogger(ReceiverReconcileJmsMessageTask.class);
	
	private JmsMessageRepository repo;
	
	public ReceiverReconcileJmsMessageTask() {
		super(SyncContext.getBean(ReceiverJmsMessageProcessor.class));
		this.repo = SyncContext.getBean(JmsMessageRepository.class);
	}
	
	@Override
	public String getTaskName() {
		return "reconcile jms msg task";
	}
	
	@Override
	public List<JmsMessage> getNextBatch() {
		return repo.findByType(MessageType.RECONCILE, AppUtils.getTaskPage());
	}
	
}
