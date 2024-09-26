package org.openmrs.eip.app.sender.reconcile;

import java.util.UUID;

import org.openmrs.eip.app.AppUtils;
import org.openmrs.eip.app.SyncConstants;
import org.openmrs.eip.app.management.entity.receiver.JmsMessage.MessageType;
import org.springframework.jms.core.MessageCreator;

import jakarta.jms.BytesMessage;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Session;
import lombok.Getter;

/**
 * {@link MessageCreator} implementation for reconciliation responses.
 */
public class ReconcileResponseCreator implements MessageCreator {
	
	@Getter
	private byte[] body;
	
	@Getter
	private String siteId;
	
	ReconcileResponseCreator(byte[] body, String siteId) {
		this.body = body;
		this.siteId = siteId;
	}
	
	@Override
	public Message createMessage(Session session) throws JMSException {
		//TODO First compress payload if necessary
		BytesMessage message = session.createBytesMessage();
		message.writeBytes(body);
		message.setStringProperty(SyncConstants.JMS_HEADER_MSG_ID, UUID.randomUUID().toString());
		message.setStringProperty(SyncConstants.JMS_HEADER_SITE, siteId);
		message.setStringProperty(SyncConstants.JMS_HEADER_TYPE, MessageType.RECONCILE.name());
		message.setStringProperty(SyncConstants.JMS_HEADER_VERSION, AppUtils.getVersion());
		return message;
	}
}
