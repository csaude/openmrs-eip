package org.openmrs.eip.app.sender;

import org.openmrs.eip.app.AppUtils;
import org.openmrs.eip.app.SyncConstants;
import org.openmrs.eip.app.management.entity.receiver.JmsMessage.MessageType;
import org.springframework.jms.core.MessageCreator;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import lombok.Getter;

/**
 * {@link MessageCreator} implementation for sync messages.
 */
public class SyncMessageCreator implements MessageCreator {
	
	@Getter
	private String body;
	
	@Getter
	private String messageUuid;
	
	@Getter
	private String siteId;
	
	SyncMessageCreator(String body, String messageUuid, String siteId) {
		this.body = body;
		this.messageUuid = messageUuid;
		this.siteId = siteId;
	}
	
	@Override
	public Message createMessage(Session session) throws JMSException {
		TextMessage message = session.createTextMessage(body);
		message.setStringProperty(SyncConstants.JMS_HEADER_MSG_ID, messageUuid);
		message.setStringProperty(SyncConstants.JMS_HEADER_VERSION, AppUtils.getVersion());
		message.setStringProperty(SyncConstants.JMS_HEADER_SITE, siteId);
		message.setStringProperty(SyncConstants.JMS_HEADER_TYPE, MessageType.SYNC.name());
		return message;
	}
	
}
