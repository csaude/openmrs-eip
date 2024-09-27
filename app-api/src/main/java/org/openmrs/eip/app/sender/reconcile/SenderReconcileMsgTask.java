package org.openmrs.eip.app.sender.reconcile;

import java.util.List;

import org.openmrs.eip.app.AppUtils;
import org.openmrs.eip.app.BaseDelegatingQueueTask;
import org.openmrs.eip.app.management.entity.sender.SenderReconcileMessage;
import org.openmrs.eip.app.management.repository.SenderReconcileMsgRepository;
import org.openmrs.eip.app.receiver.reconcile.ReconcileMessageProcessor;
import org.openmrs.eip.component.SyncContext;

/**
 * Reads a batch of table reconciliations and forwards them to the
 * {@link ReconcileMessageProcessor}.
 */
public class SenderReconcileMsgTask extends BaseDelegatingQueueTask<SenderReconcileMessage, SenderReconcileMsgProcessor> {
	
	private SenderReconcileMsgRepository repo;
	
	public SenderReconcileMsgTask() {
		super(SyncContext.getBean(SenderReconcileMsgProcessor.class));
		this.repo = SyncContext.getBean(SenderReconcileMsgRepository.class);
	}
	
	@Override
	public String getTaskName() {
		return "reconcile msg task";
	}
	
	@Override
	public List<SenderReconcileMessage> getNextBatch() {
		return repo.getBatch(AppUtils.getTaskPage());
	}
	
}
