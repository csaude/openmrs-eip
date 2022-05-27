package org.openmrs.eip.component.camel;

import java.io.File;

import javax.activation.FileDataSource;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.attachment.AttachmentMessage;
import org.apache.camel.attachment.DefaultAttachment;
import org.openmrs.eip.component.SyncProfiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("sendShutdownNotificationEmailProcessor")
public class SendShutdownNotificationEmailProcessor implements Processor {
	
	private static Logger log = LoggerFactory.getLogger(SendShutdownNotificationEmailProcessor.class);
	
	@Value("${spring.email.attachment.application-log-file}")
	private String logsURI;
	
	@Value("${smtp.host.name}")
	private String smtpHostName;
	
	@Value("${smtp.host.port}")
	private String smtpHostPort;
	
	@Value("${smtp.auth.user}")
	private String smtpAuthUser;
	
	@Value("${smtp.auth.pass}")
	private String smtpAuthPass;
	
	@Value("${spring.email.send-to}")
	private String springEmailSendTo;
	
	@Value("${spring.profiles.active}")
	private String activeProfile;
	
	@Value("${db-sync.senderId:#{null}}")
	private String dbSyncSenderId;
	
	@Value("${db-sync.receiverId:#{null}}")
	private String dbSyncReceiverId;
	
	@Override
	public void process(Exchange exchange) throws Exception {
		log.info("Attaching the log file to email");
		
		String siteName = activeProfile.equals(SyncProfiles.SENDER) ? dbSyncSenderId : dbSyncReceiverId;
		
		AttachmentMessage in = exchange.getMessage(AttachmentMessage.class);
		
		in.setHeader("subject", "EIP Notification - Application Has Stopped");
		in.setHeader("to", springEmailSendTo);
		in.setHeader("from", smtpAuthUser);
		in.setHeader("mail.smtp.auth", true);
		in.setHeader("mail.smtp.starttls.enable", true);
		
		in.setBody("The application on Remote Site " + siteName
		        + " has just stopped due exception. Please refere to the attached log file");
		
		File file = new File(logsURI);
		
		if (file.exists()) {
			DefaultAttachment att = new DefaultAttachment(new FileDataSource(logsURI));
			att.addHeader("Content-Description", "Applicatio Logs file");
			in.addAttachmentObject("Dbsync-logs", att);
		}
		
		Endpoint endpoint = exchange.getContext().getEndpoint(
		    "smtp://" + smtpHostName + ":" + smtpHostPort + "?username=" + smtpAuthUser + "&password=" + smtpAuthPass);
		
		Producer producer = endpoint.createProducer();
		producer.start();
		producer.process(exchange);
	}
	
}
