package org.openmrs.eip.app.receiver;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.eip.app.SyncConstants;
import org.openmrs.eip.app.management.entity.receiver.JmsMessage;
import org.openmrs.eip.app.management.entity.receiver.JmsMessage.MessageType;
import org.openmrs.eip.app.management.repository.JmsMessageRepository;
import org.openmrs.eip.app.management.service.ReceiverService;
import org.openmrs.eip.component.SyncProfiles;
import org.openmrs.eip.component.exception.EIPException;
import org.openmrs.eip.component.model.SyncModel;
import org.openmrs.eip.component.utils.JsonUtils;
import org.openmrs.eip.component.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import jakarta.jms.BytesMessage;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.StreamMessage;

@Component
@Profile(SyncProfiles.RECEIVER)
public class ReceiverMessageListener implements MessageListener {
	
	private static final Logger LOG = LoggerFactory.getLogger(ReceiverMessageListener.class);
	
	private JmsMessageRepository repo;
	
	private ReceiverService service;
	
	@Value("${" + ReceiverConstants.PROP_JMS_SKIP_DUPLICATES + ":true}")
	private boolean skipDuplicates;
	
	public ReceiverMessageListener(JmsMessageRepository repo, ReceiverService service) {
		this.repo = repo;
		this.service = service;
	}
	
	@Override
	public void onMessage(Message message) {
		try {
			byte[] body;
			if (message instanceof BytesMessage || message instanceof StreamMessage) {
				if (message instanceof BytesMessage) {
					body = message.getBody(byte[].class);
				} else {
					StreamMessage streamMsg = (StreamMessage) message;
					body = new byte[message.getIntProperty(SyncConstants.JMS_HEADER_MSG_LENGTH)];
					streamMsg.readBytes(body);
				}
				
				if (message.getBooleanProperty(SyncConstants.JMS_HEADER_COMPRESSED)) {
					body = Utils.decompress(body);
				}
			} else {
				body = message.getBody(String.class).getBytes(StandardCharsets.UTF_8);
			}
			
			final String siteId = message.getStringProperty(SyncConstants.JMS_HEADER_SITE);
			final String version = message.getStringProperty(SyncConstants.JMS_HEADER_VERSION);
			MessageType type = MessageType.SYNC;
			String typeStr = message.getStringProperty(SyncConstants.JMS_HEADER_TYPE);
			if (StringUtils.isNotBlank(typeStr)) {
				type = MessageType.valueOf(typeStr);
			}
			
			String batchSizeStr = null;
			if (type == MessageType.SYNC) {
				batchSizeStr = message.getStringProperty(SyncConstants.JMS_HEADER_BATCH_SIZE);
			}
			
			if (batchSizeStr == null) {
				final String msgUid = JsonUtils.unmarshalBytes(body, SyncModel.class).getMetadata().getMessageUuid();
				JmsMessage jmsMsg = createJmsMessage(msgUid, body, siteId, version, type);
				if (jmsMsg != null) {
					service.saveJmsMessage(jmsMsg);
				}
			} else {
				final int batchSize = Integer.valueOf(batchSizeStr);
				List<Map> items = JsonUtils.unmarshalBytes(body, List.class);
				if (batchSize != items.size()) {
					throw new EIPException("Item count " + items.size() + " doesn't match the batch size " + batchSize);
				}
				
				if (LOG.isDebugEnabled()) {
					LOG.debug("Processing sync batch of {} items", batchSize);
				}
				
				List<JmsMessage> msgs = new ArrayList<>(items.size());
				for (Map entry : items) {
					String msgUid = ((Map) (entry).get("metadata")).get("messageUuid").toString();
					JmsMessage msg = createJmsMessage(msgUid, JsonUtils.marshalToBytes(entry), siteId, version, type);
					if (msg != null) {
						msgs.add(msg);
					}
				}
				
				service.saveJmsMessages(msgs);
			}
			
			ReceiverMessageListenerContainer.enableAcknowledgement();
		}
		catch (Throwable t) {
			throw new EIPException("Failed to process incoming JMS message", t);
		}
	}
	
	private JmsMessage createJmsMessage(String msgId, byte[] body, String siteId, String version, MessageType type) {
		//TODO Add global property to disable this check for performance
		if (skipDuplicates && StringUtils.isNotBlank(msgId) && repo.existsByMessageId(msgId)) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Skipping duplicate incoming JMS message with the message id: " + msgId);
			}
			
			return null;
		}
		
		JmsMessage jmsMsg = new JmsMessage();
		jmsMsg.setMessageId(msgId);
		jmsMsg.setBody(body);
		jmsMsg.setSiteId(siteId);
		jmsMsg.setSyncVersion(version);
		jmsMsg.setType(type);
		jmsMsg.setDateCreated(new Date());
		return jmsMsg;
	}
	
}
