package org.openmrs.eip.app.sender;

import static org.openmrs.eip.app.SyncConstants.BEAN_NAME_SYNC_EXECUTOR;
import static org.openmrs.eip.component.SyncOperation.d;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.camel.ProducerTemplate;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.eip.app.BaseFromCamelToCamelEndpointProcessor;
import org.openmrs.eip.app.management.entity.sender.DebeziumEvent;
import org.openmrs.eip.app.management.repository.DebeziumEventRepository;
import org.openmrs.eip.component.SyncProfiles;
import org.openmrs.eip.component.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component("debeziumEventProcessor")
@Profile(SyncProfiles.SENDER)
public class DebeziumEventProcessor extends BaseFromCamelToCamelEndpointProcessor<DebeziumEvent> {
	
	private static final Logger LOG = LoggerFactory.getLogger(DebeziumEventProcessor.class);
	
	private DebeziumEventRepository repo;
	
	public DebeziumEventProcessor(ProducerTemplate producerTemplate,
	    @Qualifier(BEAN_NAME_SYNC_EXECUTOR) ThreadPoolExecutor executor, DebeziumEventRepository repo) {
		super(SenderConstants.URI_DBZM_EVENT_PROCESSOR, producerTemplate, executor);
		this.repo = repo;
	}
	
	@Override
	public String getProcessorName() {
		return "db event";
	}
	
	@Override
	public String getThreadName(DebeziumEvent event) {
		String name = event.getEvent().getTableName() + "-" + event.getEvent().getPrimaryKeyId();
		if (StringUtils.isNotBlank(event.getEvent().getIdentifier())) {
			name += ("-" + event.getEvent().getIdentifier());
		}
		
		return name;
	}
	
	@Override
	public String getUniqueId(DebeziumEvent item) {
		return item.getEvent().getPrimaryKeyId();
	}
	
	@Override
	public String getQueueName() {
		return "db-event";
	}
	
	@Override
	public String getLogicalType(DebeziumEvent item) {
		return item.getEvent().getTableName();
	}
	
	@Override
	public List<String> getLogicalTypeHierarchy(String logicalType) {
		return Utils.getListOfTablesInHierarchy(logicalType);
	}
	
	@Override
	public void processWork(List<DebeziumEvent> items) throws Exception {
		//Squash events for the same row so that exactly one message is processed in case of multiple in this run in.
		//Delete being a terminal event, squash for a single entity will stop at the last event before a delete event
		//to ensure we don't re-process a non-existent entity
		Map<String, DebeziumEvent> keyAndLatestMap = new LinkedHashMap(items.size());
		List<DebeziumEvent> squashedEvents = new ArrayList();
		items.stream().forEach(dbzmEvent -> {
			String table = dbzmEvent.getEvent().getTableName();
			String key = table + "#" + dbzmEvent.getEvent().getPrimaryKeyId();
			if (keyAndLatestMap.containsKey(key) && d.name().equals(dbzmEvent.getEvent().getOperation())) {
				if (LOG.isTraceEnabled()) {
					LOG.trace("Squashing stopped for {}, postponing processing of delete event: {}", key, dbzmEvent);
				}
			} else {
				DebeziumEvent previousEvent = keyAndLatestMap.put(key, dbzmEvent);
				if (previousEvent != null) {
					squashedEvents.add(previousEvent);
					
					if (LOG.isTraceEnabled()) {
						LOG.trace("Squashing entity event: {}", previousEvent);
					}
				}
			}
		});
		
		doProcessWork(keyAndLatestMap.values().stream().toList());
		repo.deleteAll(squashedEvents);
	}
	
	protected void doProcessWork(List<DebeziumEvent> items) throws Exception {
		super.processWork(items);
	}
	
}
