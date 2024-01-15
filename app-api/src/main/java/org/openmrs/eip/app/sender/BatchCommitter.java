package org.openmrs.eip.app.sender;

import static org.openmrs.eip.app.SyncConstants.MGT_DATASOURCE_NAME;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.eip.app.management.entity.sender.SenderSyncMessage;
import org.openmrs.eip.component.camel.utils.CamelUtils;
import org.openmrs.eip.component.model.SyncModel;
import org.openmrs.eip.component.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.MessageProducer;
import jakarta.jms.Queue;
import jakarta.jms.Session;

@Component("batchCommitter")
public class BatchCommitter {
	
	private static final Logger log = LoggerFactory.getLogger(BatchCommitter.class);
	
	@Autowired
	private ConnectionFactory activeMqConnFactory;
	
	@Value("db-sync.senderId")
	private String senderId;
	
	private String queueName = "openmrs.sync";
	
	public void sendBatch(List<SenderSyncMessage> items) throws JMSException {
		if (log.isDebugEnabled()) {
			log.debug("Sending " + items.size() + " sync messages(s)");
		}
		
		List<String> messages = new ArrayList<>(items.size());
		LocalDateTime dateSent = LocalDateTime.now();
		List<Long> ids = new ArrayList<>(items.size());
		for (SenderSyncMessage m : items) {
			SyncModel model = JsonUtils.unmarshalSyncModel(m.getData());
			model.getMetadata().setSourceIdentifier(senderId);
			model.getMetadata().setDateSent(dateSent);
			messages.add(JsonUtils.marshall(model));
			ids.add(m.getId());
		}
		
		try (Connection conn = activeMqConnFactory.createConnection();
		        Session session = conn.createSession(true, Session.AUTO_ACKNOWLEDGE)) {
			
			Queue queue = session.createQueue(queueName);
			try (MessageProducer p = session.createProducer(queue)) {
				for (String responsePayload : messages) {
					p.send(session.createTextMessage(responsePayload));
				}
			}
			
			if (log.isDebugEnabled()) {
				log.debug("Committing " + messages.size() + " messages(s)");
			}
			
			session.commit();
			
			if (log.isDebugEnabled()) {
				log.debug("Successfully committed " + messages.size() + " messages(s)");
			}
		}
		
		log.info("Successfully sent " + messages.size() + " sync messages");
		
		String myIds = StringUtils.join(ids, ",");
		CamelUtils.send("sql:UPDATE sender_sync_message set status = 'SENT', date_sent = now() where id in (" + myIds
		        + ")?dataSource=#" + MGT_DATASOURCE_NAME);
		
		log.info("Successfully updated the statuses of " + messages.size() + " sync messages");
	}
	
}
