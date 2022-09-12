package org.openmrs.eip.app.sender;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.openmrs.eip.app.config.JMSApplicationContextInitializer;
import org.openmrs.eip.app.management.entity.SenderSyncMessage;
import org.openmrs.eip.app.management.repository.SenderSyncMessageRepository;
import org.openmrs.eip.component.DatabaseOperation;
import org.openmrs.eip.component.SyncProfiles;
import org.openmrs.eip.component.model.BaseModel;
import org.openmrs.eip.component.model.SyncMetadata;
import org.openmrs.eip.component.model.SyncModel;
import org.openmrs.eip.component.service.TableToSyncEnum;
import org.openmrs.eip.component.service.security.PGPEncryptService;
import org.openmrs.eip.component.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component("senderActiveMqPublisherProcessor")
@Profile(SyncProfiles.SENDER)
public class SenderActiveMqPublisherProcessor implements Processor {
	
	private static final Logger log = LoggerFactory.getLogger(SenderActiveMqPublisherProcessor.class);
	
	private static final String OPENMRS_EXTRACT_ENDPOINT = "openmrs:extract?tableToSync={0}&uuid={1}";
	
	@Autowired
	private ProducerTemplate producerTemplate;
	
	@Autowired
	private PGPEncryptService pgpEncryptService;
	
	@Autowired
	private SenderSyncMessageRepository senderSyncMessageRepository;
	
	@Value("${" + SenderConstants.PROP_SENDER_ID + "}")
	private String senderId;
	
	@Value("${" + SenderConstants.PROP_ENCRYPTION_ENABLED + ":false}")
	private boolean encryptionEnabled;
	
	@Value("${" + SenderConstants.PROP_CAMEL_OUTPUT_ENDPOINT + "}")
	private String camelOutputEndpoint;
	
	@Override
	public void process(Exchange exchange) throws Exception {
		List<SenderSyncMessage> messages = exchange.getIn().getBody(List.class);
		
		for (SenderSyncMessage message : messages) {
			log.info("Preparing sender sync message: {}", message);
			
			SyncModel syncModel = loadSyncModel(message);
			
			if (syncModel != null || DatabaseOperation.r.name().equals(message.getOperation())) {
				
				if (syncModel == null) {
					log.info("Entity not found for request with uuid: {}", message.getRequestUuid());
					
					syncModel = new SyncModel();
					syncModel.setMetadata(new SyncMetadata());
				}
				
				SyncMetadata metadata = syncModel.getMetadata();
				metadata.setSourceIdentifier(senderId);
				metadata.setDateSent(LocalDateTime.now());
				metadata.setOperation(message.getOperation());
				metadata.setRequestUuid(message.getRequestUuid());
				metadata.setMessageUuid(message.getMessageUuid());
				metadata.setSnapshot(message.isSnapshot());
				
				String syncModelPayload = JsonUtils.marshall(syncModel);
				
				if (log.isDebugEnabled()) {
					log.debug("Sync payload: {}", syncModelPayload);
				}
				
				if (encryptionEnabled) {
					log.info("Encrypting entity payload.");
					syncModelPayload = pgpEncryptService.encryptAndSign(syncModelPayload);
					
					if (log.isTraceEnabled()) {
						log.trace("Encrypted entity payload: {}", syncModelPayload);
					}
				}
				
				String camelOutputEndpointForReceiver = String.format(camelOutputEndpoint,
				    JMSApplicationContextInitializer.getConnectionFactoryId(message.getBroker()));
				log.info("Sending sender sync message to: {}.", camelOutputEndpointForReceiver);
				producerTemplate.sendBody(camelOutputEndpointForReceiver, syncModelPayload);
				
				if (log.isDebugEnabled()) {
					log.debug("Entity sent!");
				}
				
				message.markAsSent();
				
				if (log.isDebugEnabled()) {
					log.debug("Updating sender sync message with identifier {} to {}", message.getIdentifier(),
					    message.getStatus());
				}
				
				senderSyncMessageRepository.save(message);
				log.debug("Successfully updated sender sync message with identifier {} to {}", message.getIdentifier(),
				    message.getStatus());
				
			} else {
				log.info("No entity found in the database matching identifier {} in table {}", message.getIdentifier(),
				    message.getTableName());
				
				if (log.isDebugEnabled()) {
					log.debug("Removing sender sync message with identifier {}", message.getIdentifier(),
					    message.getTableName());
				}
				
				senderSyncMessageRepository.delete(message);
				
				if (log.isDebugEnabled()) {
					log.debug("Successfully removed sender sync message with identifier {}", message.getIdentifier(),
					    message.getTableName());
				}
			}
		}
	}
	
	private SyncModel loadSyncModel(SenderSyncMessage message)
	        throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		SyncModel syncModel;
		if (DatabaseOperation.d.name().equals(message.getOperation())) {
			syncModel = new SyncModel();
			Class<? extends BaseModel> modelClass = TableToSyncEnum.getTableToSyncEnum(message.getTableName())
			        .getModelClass();
			
			syncModel.setTableToSyncModelClass(modelClass);
			syncModel.setModel(modelClass.getConstructor().newInstance());
			syncModel.getModel().setUuid(message.getIdentifier());
			syncModel.setMetadata(new SyncMetadata());
			
			log.info("Deleted entity payload: {}", syncModel);
		} else {
			if (log.isDebugEnabled()) {
				log.debug("Loading entity from DB with identifier {}", message.getIdentifier());
			}
			
			List<SyncModel> syncModels = producerTemplate.requestBody(MessageFormat.format(OPENMRS_EXTRACT_ENDPOINT,
			    message.getTableName().toUpperCase(), message.getIdentifier()), null, List.class);
			
			if (log.isDebugEnabled()) {
				log.debug("Loaded entity: {}", syncModels);
			}
			
			syncModel = syncModels.size() == 1 ? syncModels.get(0) : null;
		}
		
		return syncModel;
	}
	
}
