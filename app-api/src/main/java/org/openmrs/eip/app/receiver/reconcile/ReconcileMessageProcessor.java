package org.openmrs.eip.app.receiver.reconcile;

import static org.openmrs.eip.app.SyncConstants.BEAN_NAME_SYNC_EXECUTOR;
import static org.openmrs.eip.app.SyncConstants.PROP_MAX_BATCH_RECONCILE_SIZE;
import static org.openmrs.eip.app.SyncConstants.PROP_MIN_BATCH_RECONCILE_SIZE;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.eip.app.BaseQueueProcessor;
import org.openmrs.eip.app.SyncConstants;
import org.openmrs.eip.app.management.entity.receiver.ReconciliationMessage;
import org.openmrs.eip.app.management.entity.receiver.UndeletedEntity;
import org.openmrs.eip.app.management.repository.ReceiverRetryRepository;
import org.openmrs.eip.app.management.repository.SyncMessageRepository;
import org.openmrs.eip.app.management.repository.UndeletedEntityRepository;
import org.openmrs.eip.app.management.service.ReceiverReconcileService;
import org.openmrs.eip.component.SyncContext;
import org.openmrs.eip.component.SyncOperation;
import org.openmrs.eip.component.SyncProfiles;
import org.openmrs.eip.component.exception.EIPException;
import org.openmrs.eip.component.model.BaseModel;
import org.openmrs.eip.component.repository.OpenmrsRepository;
import org.openmrs.eip.component.service.TableToSyncEnum;
import org.openmrs.eip.component.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Processes a ReconciliationMessage
 */
@Component("reconcileMsgProcessor")
@Profile(SyncProfiles.RECEIVER)
public class ReconcileMessageProcessor extends BaseQueueProcessor<ReconciliationMessage> {
	
	private static final Logger LOG = LoggerFactory.getLogger(ReconcileMessageProcessor.class);
	
	private static final int DEFAULT_MIN_BATCH_RECONCILE_SIZE = 50;
	
	private static final int DEFAULT_MAX_BATCH_RECONCILE_SIZE = 500;
	
	protected static final List<SyncOperation> OPERATIONS = List.of(SyncOperation.d);
	
	@Value("${" + PROP_MIN_BATCH_RECONCILE_SIZE + ":" + DEFAULT_MIN_BATCH_RECONCILE_SIZE + "}")
	private int minReconcileBatchSize;
	
	@Value("${" + PROP_MAX_BATCH_RECONCILE_SIZE + ":" + DEFAULT_MAX_BATCH_RECONCILE_SIZE + "}")
	private int maxReconcileBatchSize;
	
	private boolean batchSizesValid;
	
	private ReceiverReconcileService service;
	
	private UndeletedEntityRepository undeletedRepo;
	
	private SyncMessageRepository syncMsgRepo;
	
	private ReceiverRetryRepository retryRepo;
	
	public ReconcileMessageProcessor(@Qualifier(BEAN_NAME_SYNC_EXECUTOR) ThreadPoolExecutor executor,
	    ReceiverReconcileService service, UndeletedEntityRepository undeletedRepo, SyncMessageRepository syncMsgRepo,
	    ReceiverRetryRepository retryRepo) {
		super(executor);
		this.service = service;
		this.undeletedRepo = undeletedRepo;
		this.syncMsgRepo = syncMsgRepo;
		this.retryRepo = retryRepo;
	}
	
	@Override
	public String getProcessorName() {
		return "reconcile msg";
	}
	
	@Override
	public String getQueueName() {
		return "reconcile-msg";
	}
	
	@Override
	public String getThreadName(ReconciliationMessage item) {
		return item.getId().toString();
	}
	
	@Override
	public String getUniqueId(ReconciliationMessage item) {
		//Items belonging to same site and table are processed serially.
		return item.getSite().getIdentifier();
	}
	
	@Override
	public String getLogicalType(ReconciliationMessage item) {
		return item.getTableName();
	}
	
	@Override
	public List<String> getLogicalTypeHierarchy(String logicalType) {
		return Utils.getListOfTablesInHierarchy(logicalType);
	}
	
	@Override
	public void processItem(ReconciliationMessage msg) {
		if (!batchSizesValid) {
			if (minReconcileBatchSize > maxReconcileBatchSize) {
				throw new EIPException("The value for " + PROP_MIN_BATCH_RECONCILE_SIZE + " can't be less than that of "
				        + PROP_MAX_BATCH_RECONCILE_SIZE);
			}
			
			batchSizesValid = true;
		}
		
		String[] uuids = StringUtils.split(msg.getData().trim(), SyncConstants.RECONCILE_MSG_SEPARATOR);
		if (uuids.length != msg.getBatchSize()) {
			throw new EIPException("Batch size and item count don't match for the reconciliation message");
		}
		
		List<String> allUuids = Arrays.stream(uuids).collect(Collectors.toUnmodifiableList());
		//Pick up from where we left off
		if (msg.getProcessedCount() > 0) {
			uuids = Arrays.copyOfRange(uuids, msg.getProcessedCount(), uuids.length);
		}
		
		OpenmrsRepository repo = SyncContext.getRepositoryBean(msg.getTableName());
		List<String> uuidList = Arrays.stream(uuids).toList();
		reconcile(uuidList, allUuids, msg, repo);
	}
	
	private void reconcile(List<String> uuids, List<String> allUuids, ReconciliationMessage msg, OpenmrsRepository repo) {
		final int size = uuids.size();
		if (size == 0 || msg.isLastTableBatch()) {
			if (msg.isLastTableBatch()) {
				verifyDeletedEntities(uuids, allUuids, msg, repo);
			}
			
			service.updateReconciliationMessage(msg, true, uuids);
			return;
		}
		
		if (size > maxReconcileBatchSize) {
			bisectAndReconcile(uuids, allUuids, msg, repo);
			return;
		}
		
		if (LOG.isTraceEnabled()) {
			LOG.trace("Reconciling batch of {} items from index {} to {} in table {}", size, allUuids.indexOf(uuids.get(0)),
			    allUuids.indexOf(uuids.get(size - 1)), msg.getTableName());
		}
		
		final int matchCount = repo.countByUuidIn(uuids);
		if (matchCount == 0 || matchCount == size) {
			boolean found = matchCount == size;
			if (LOG.isTraceEnabled()) {
				LOG.trace("Updating reconciliation msg with {} {} uuid(s) in table {}", size, (found ? "found" : "missing"),
				    msg.getTableName());
			}
			
			//All uuids are missing or existing
			service.updateReconciliationMessage(msg, found, uuids);
			return;
		}
		
		//Give up on the split approach and process uuids individually
		if (size < minReconcileBatchSize) {
			for (String uuid : uuids) {
				boolean found = repo.existsByUuid(uuid);
				if (LOG.isTraceEnabled()) {
					LOG.trace("Updating reconciliation after {} uuid in table {}", (found ? "found" : "missing"),
					    msg.getTableName());
				}
				
				service.updateReconciliationMessage(msg, found, List.of(uuid));
			}
			
			return;
		}
		
		//Recursively split and check until we find a left half with no missing uuids and then proceed to the right.
		bisectAndReconcile(uuids, allUuids, msg, repo);
	}
	
	private void bisectAndReconcile(List<String> uuids, List<String> allUuids, ReconciliationMessage msg,
	                                OpenmrsRepository repo) {
		int midIndex = uuids.size() / 2;
		List<String> left = uuids.subList(0, midIndex);
		List<String> right = uuids.subList(midIndex, uuids.size());
		reconcile(left, allUuids, msg, repo);
		reconcile(right, allUuids, msg, repo);
	}
	
	protected void verifyDeletedEntities(List<String> uuids, List<String> allUuids, ReconciliationMessage msg,
	                                     OpenmrsRepository repo) {
		final int size = uuids.size();
		if (size == 0) {
			return;
		}
		
		final String table = msg.getTableName();
		if (LOG.isDebugEnabled()) {
			LOG.debug("Verifying batch of {} deleted items from index {} to {} in table {}", size,
			    allUuids.indexOf(uuids.get(0)), allUuids.indexOf(uuids.get(size - 1)), table);
		}
		
		Class<? extends BaseModel> modelClass = TableToSyncEnum.getTableToSyncEnum(table.toUpperCase()).getModelClass();
		List<String> classNames = Utils.getListOfModelClassHierarchy(modelClass.getName());
		//TODO Add the bisect and reconcile logic
		for (String uuid : uuids) {
			if (!repo.existsByUuid(uuid)) {
				continue;
			}
			
			if (LOG.isTraceEnabled()) {
				LOG.trace("Found undeleted entity with uuid {} in table {}", uuid, table);
			}
			
			UndeletedEntity entity = new UndeletedEntity();
			entity.setTableName(table);
			entity.setIdentifier(uuid);
			entity.setSite(msg.getSite());
			entity.setDateCreated(new Date());
			entity.setInErrorQueue(
			    retryRepo.existsByIdentifierAndModelClassNameInAndOperationIn(uuid, classNames, OPERATIONS));
			entity.setInSyncQueue(
			    syncMsgRepo.existsByIdentifierAndModelClassNameInAndOperationIn(uuid, classNames, OPERATIONS));
			undeletedRepo.save(entity);
		}
	}
	
}
