package org.openmrs.eip.app.receiver;

import static org.openmrs.eip.app.SyncConstants.BEAN_NAME_SYNC_EXECUTOR;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.camel.ProducerTemplate;
import org.openmrs.eip.app.AppUtils;
import org.openmrs.eip.app.management.entity.receiver.SyncedMessage;
import org.openmrs.eip.app.management.repository.SyncedMessageRepository;
import org.openmrs.eip.component.SyncProfiles;
import org.openmrs.eip.component.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Processes sync messages that require updating the OpenMRS search index.
 */
@Component("searchIndexUpdatingProcessor")
@Profile(SyncProfiles.RECEIVER)
public class SearchIndexUpdatingProcessor extends BaseSendToCamelPostSyncActionProcessor {
	
	protected static final Logger log = LoggerFactory.getLogger(SearchIndexUpdatingProcessor.class);
	
	public SearchIndexUpdatingProcessor(ProducerTemplate producerTemplate,
	    @Qualifier(BEAN_NAME_SYNC_EXECUTOR) ThreadPoolExecutor executor, SyncedMessageRepository repo) {
		super(ReceiverConstants.URI_UPDATE_SEARCH_INDEX, producerTemplate, executor, repo);
	}
	
	@Override
	public String getProcessorName() {
		return "search index update";
	}
	
	@Override
	public String getUniqueId(SyncedMessage item) {
		return item.getIdentifier();
	}
	
	@Override
	public String getQueueName() {
		return "search-index-update";
	}
	
	@Override
	public String getThreadName(SyncedMessage item) {
		return item.getSite().getIdentifier() + "-" + item.getMessageUuid() + "-"
		        + AppUtils.getSimpleName(item.getModelClassName()) + "-" + item.getIdentifier();
	}
	
	@Override
	public String getLogicalType(SyncedMessage item) {
		return item.getModelClassName();
	}
	
	@Override
	public List<String> getLogicalTypeHierarchy(String logicalType) {
		return Utils.getListOfModelClassHierarchy(logicalType);
	}
	
	@Override
	public void onSuccess(SyncedMessage item) {
		item.setSearchIndexUpdated(true);
		repo.save(item);
	}
	
	@Override
	public Object convertBody(SyncedMessage item) {
		return ReceiverUtils.generateSearchIndexUpdatePayload(item.getModelClassName(), item.getIdentifier(),
		    item.getOperation());
	}
	
}
