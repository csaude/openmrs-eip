package org.openmrs.eip.component.camel;

import java.io.File;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;

import org.apache.camel.Exchange;
import org.apache.camel.attachment.AttachmentMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

public class OpenmrsAttachmentProducer extends AbstractOpenmrsProducer {
	private static Logger log = LoggerFactory.getLogger(OpenmrsAttachmentProducer.class);
	
	public OpenmrsAttachmentProducer(final OpenmrsEndpoint endpoint, final ApplicationContext applicationContext, final ProducerParams params) {
		super(endpoint, applicationContext, params);
	}
	
	@Override
	public void process(final Exchange exchange) {
		log.info("Attaching the log file to email");
		
		AttachmentMessage in = exchange.getMessage(AttachmentMessage.class); 
		
		String logsURI = appContext.getEnvironment().getProperty("spring.email.attachment.application-log-file");
		
		File file = new File(logsURI);
		
		if (file.exists()) in.addAttachment("Dbsyn-logs", new DataHandler(new FileDataSource(logsURI)));
	}
}