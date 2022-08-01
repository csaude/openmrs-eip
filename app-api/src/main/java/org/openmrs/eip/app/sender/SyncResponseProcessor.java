package org.openmrs.eip.app.sender;

import static java.util.Collections.singletonMap;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.jpa.JpaConstants;
import org.apache.commons.collections.CollectionUtils;
import org.openmrs.eip.app.management.entity.SenderSyncMessage;
import org.openmrs.eip.app.management.entity.SenderSyncResponse;
import org.openmrs.eip.component.SyncProfiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component("syncResponseProcessor")
@Profile(SyncProfiles.SENDER)
public class SyncResponseProcessor implements Processor {
	
	private static final Logger log = LoggerFactory.getLogger(SyncResponseProcessor.class);
	
	private static final String SENDER_SYNC_MESSAGE_ENTITY = SenderSyncMessage.class.getSimpleName();
	
	private static final String SENDER_SYNC_RESPONSE_ENTITY = SenderSyncResponse.class.getSimpleName();
	
	private static final String PARAM_ID = "id";
	
	private static final String PARAM_MESSAGE_UUID = "messageUuid";
	
	private static final String JPA_SENDER_SYNC_MESSAGE_QUERY = MessageFormat.format(
	    "jpa:{0}?query=SELECT m FROM {0} m WHERE m.messageUuid = :{1}", SENDER_SYNC_MESSAGE_ENTITY, PARAM_MESSAGE_UUID);
	
	private static final String JPA_SENDER_SYNC_MESSAGE_DELETE_QUERY = MessageFormat.format(
	    "jpa:{0}?query=DELETE FROM {0} WHERE messageUuid = :{1}", SENDER_SYNC_MESSAGE_ENTITY, PARAM_MESSAGE_UUID);
	
	private static final String JPA_SENDER_SYNC_RESPONSE_DELETE_QUERY = MessageFormat
	        .format("jpa:{0}?query=DELETE FROM {0} WHERE id = :{1}", SENDER_SYNC_RESPONSE_ENTITY, PARAM_ID);
	
	@Autowired
	private ProducerTemplate producerTemplate;
	
	@Override
	public void process(Exchange exchange) throws Exception {
		List<SenderSyncResponse> responses = exchange.getIn().getBody(List.class);
		List<String> processedResponsesUUIDs = new ArrayList<>(responses.size());
		exchange.setProperty(SenderConstants.EX_PROP_PROCESSED_RESPONSES_UUIDS, processedResponsesUUIDs);
		
		for (SenderSyncResponse response : responses) {
			log.info("Processing sender sync response: {}", response);
			
			List<SenderSyncMessage> messages = producerTemplate.requestBodyAndHeader(JPA_SENDER_SYNC_MESSAGE_QUERY, null,
			    JpaConstants.JPA_PARAMETERS_HEADER, singletonMap(PARAM_MESSAGE_UUID, response.getMessageUuid()), List.class);
			
			if (CollectionUtils.size(messages) > 0) {
				log.info("Fetched {} sender sync message(s) matching message uuid {}", messages.size(),
				    response.getMessageUuid());
				
				if (log.isDebugEnabled()) {
					log.debug("Sender sync message(s) found: {}", messages);
					log.debug("Removing Sender sync message(s) with uuid {}", response.getMessageUuid());
				}
				
				producerTemplate.sendBodyAndHeader(JPA_SENDER_SYNC_MESSAGE_DELETE_QUERY, null,
				    JpaConstants.JPA_PARAMETERS_HEADER, singletonMap(PARAM_MESSAGE_UUID, response.getMessageUuid()));
				
				log.info("Successfully removed Sender sync message(s) with uuid {}", response.getMessageUuid());
			} else {
				log.info("No Sender sync message was found with uuid {}", response.getMessageUuid());
			}
			
			if (log.isDebugEnabled()) {
				log.debug("Removing processed sender sync response with uuid {}", response.getMessageUuid());
			}
			
			producerTemplate.sendBodyAndHeader(JPA_SENDER_SYNC_RESPONSE_DELETE_QUERY, null,
			    JpaConstants.JPA_PARAMETERS_HEADER, singletonMap(PARAM_ID, response.getId()));
			
			if (log.isDebugEnabled()) {
				log.debug("Successfully removed sender sync response with uuid  {}", response.getMessageUuid());
			}
			
			processedResponsesUUIDs.add(response.getMessageUuid());
		}
	}	
}
