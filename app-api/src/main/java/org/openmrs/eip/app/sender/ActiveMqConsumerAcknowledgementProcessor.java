package org.openmrs.eip.app.sender;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.jms.JmsEndpoint;
import org.openmrs.eip.app.CustomMessageListenerContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("activeMqConsumerAcknowledgementProcessor")
public class ActiveMqConsumerAcknowledgementProcessor implements Processor {
	
	private static final Logger log = LoggerFactory.getLogger(ActiveMqConsumerAcknowledgementProcessor.class);
	
	private static Map<Integer, CustomMessageListenerContainer> listeners = new HashMap<>();
	
	public static void registerListener(JmsEndpoint endpoint, CustomMessageListenerContainer listener) {
		listeners.put(endpoint.hashCode(), listener);
	}
	
	@Override
	public void process(Exchange exchange) throws Exception {
		JmsEndpoint endpoint = (JmsEndpoint) exchange.getFromEndpoint();
		listeners.get(endpoint.hashCode()).enableAcknowledgement();
	}
	
}
