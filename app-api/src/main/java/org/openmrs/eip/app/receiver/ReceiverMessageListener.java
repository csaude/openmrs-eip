package org.openmrs.eip.app.receiver;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.eip.app.SyncConstants;
import org.openmrs.eip.app.management.entity.receiver.JmsMessage;
import org.openmrs.eip.app.management.entity.receiver.JmsMessage.MessageType;
import org.openmrs.eip.app.management.repository.JmsMessageRepository;
import org.openmrs.eip.app.management.service.ReceiverService;
import org.openmrs.eip.component.SyncProfiles;
import org.openmrs.eip.component.exception.EIPException;
import org.openmrs.eip.component.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.TextMessage;

@Component
@Profile(SyncProfiles.RECEIVER)
public class ReceiverMessageListener implements MessageListener {
	
	private static final Logger LOG = LoggerFactory.getLogger(ReceiverMessageListener.class);
	
	private JmsMessageRepository repo;
	
	private ReceiverService service;
	
	public ReceiverMessageListener(JmsMessageRepository repo, ReceiverService service) {
		this.repo = repo;
		this.service = service;
	}
	
	@Override
	public void onMessage(Message message) {
		try {
			final String msgId = message.getStringProperty(SyncConstants.JMS_HEADER_MSG_ID);
			if (StringUtils.isNotBlank(msgId) && repo.existsByMessageId(msgId)) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("Skipping duplicate incoming JMS message with the message id: " + msgId);
				}
				
				return;
			}
			
			byte[] body;
			if (message instanceof TextMessage) {
				body = message.getBody(String.class).getBytes(StandardCharsets.UTF_8);
			} else {
				body = message.getBody(byte[].class);
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
				JmsMessage jmsMsg = createJmsMessage(msgId, body, siteId, version, type);
				service.saveJmsMessage(jmsMsg);
			} else {
				final int batchSize = Integer.valueOf(batchSizeStr);
				List<Object> items = JsonUtils.unmarshalBytes(body, List.class);
				if (batchSize != items.size()) {
					throw new EIPException("Item count " + items.size() + " doesn't match the batch size " + batchSize);
				}
				
				if (LOG.isDebugEnabled()) {
					LOG.debug("Processing sync batch of {} items", batchSize);
				}
				
				List<JmsMessage> msgs = new ArrayList<>(items.size());
				for (Object entry : items) {
					msgs.add(createJmsMessage(msgId, JsonUtils.marshalToBytes(entry), siteId, version, type));
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
