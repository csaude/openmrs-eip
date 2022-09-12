package org.openmrs.eip.app.sender;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.language.JsonPathExpression;
import org.apache.camel.model.language.SpELExpression;
import org.openmrs.eip.app.SyncConstants;
import org.openmrs.eip.app.config.JMSApplicationContextInitializer;
import org.openmrs.eip.app.management.entity.JMSBroker;
import org.openmrs.eip.app.management.repository.JMSBrokerRepository;
import org.openmrs.eip.component.utils.JsonUtils;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@DependsOn(SyncConstants.JMS_INITIALIZER_BEAN_NAME)
public class SenderActiveMqConsumerRouteBuilder extends RouteBuilder {
	
	private JMSBrokerRepository jmsBrokerRepository;
	
	private Environment environment;
	
	public SenderActiveMqConsumerRouteBuilder(JMSBrokerRepository jmsBrokerRepository, Environment environment) {
		super();
		this.jmsBrokerRepository = jmsBrokerRepository;
		this.environment = environment;
	}
	
	@Override
	public void configure() throws Exception {
		jmsBrokerRepository.findByDisabledFalse().forEach(this::buildRoute);
	}
	
	private void buildRoute(JMSBroker broker) {
		String fromUri = String.format(environment.getProperty(SenderConstants.PROP_ACTIVEMQ_IN_ENDPOINT),
		    JMSApplicationContextInitializer.getConnectionFactoryId(broker));
		
		RouteDefinition routeDefinition = from(fromUri);
		routeDefinition.setErrorHandlerRef("shutdownErrorHandler");
		routeDefinition.setId("sender-activemq-consumer_" + broker.getIdentifier());
		
		JsonPathExpression requestUuidJsonPathEx = new JsonPathExpression("$.requestUuid");
		requestUuidJsonPathEx.setSuppressExceptions(Boolean.TRUE.toString());
		
		JsonPathExpression messageUuidJsonPathEx = new JsonPathExpression("$.messageUuid");
		messageUuidJsonPathEx.setSuppressExceptions(Boolean.TRUE.toString());
		
		routeDefinition//
		        .log("Received message -> ${body}") //
		        .setProperty("requestUuid", requestUuidJsonPathEx) //
		        .setProperty("messageUuid", messageUuidJsonPathEx) //
		        .choice()
		        //
		        .when().simple("${exchangeProperty.requestUuid} != null")
		        .setProperty("requestTableName", jsonpath("$.tableName"))
		        .setProperty("requestIdentifier", jsonpath("$.identifier"))
		        .setProperty("requestToSave",
		            new SpELExpression("#{new org.openmrs.eip.app.management.entity.SenderSyncRequest()}"))
		        //
		        .script(new SpELExpression("#{getProperty('requestToSave').setTableName(getProperty('requestTableName'))}\n"
		                + "#{getProperty('requestToSave').setIdentifier(getProperty('requestIdentifier'))}\n"
		                + "#{getProperty('requestToSave').setRequestUuid(getProperty('requestUuid'))}\n"
		                + "#{getProperty('requestToSave').setDateCreated(new java.util.Date())}"))
		        .setBody(simple("${exchangeProperty.requestToSave}")) //
		        //
		        .log(LoggingLevel.DEBUG, "Saving sync request -> ${body}") //
		        .to("jpa:SenderSyncRequest") //
		        .log("Successfully saved sync request") //
		        .endChoice()
		        //
		        .when().simple("${exchangeProperty.messageUuid} != null")
		        .setProperty("syncResponseModelClass",
		            new SpELExpression("#{T(org.openmrs.eip.app.management.entity.SyncResponseModel)}"))
		        .setProperty("syncResponseModel",
		            method(JsonUtils.class, "unmarshal(${body}, ${exchangeProperty.syncResponseModelClass})"))
		        //
		        .setBody(new SpELExpression("#{new org.openmrs.eip.app.management.entity.SenderSyncResponse()}"))
		        .script(new SpELExpression("#{body.setDateCreated(new java.util.Date())}\n"
		                + "#{body.setMessageUuid(getProperty('syncResponseModel').messageUuid)}\n"
		                + "#{body.setDateSentByReceiver(getProperty('syncResponseModel').dateSentByReceiver)}"))
		        //
		        .log(LoggingLevel.DEBUG, "Saving sync response -> ${body}") //
		        .to("jpa:SenderSyncResponse") //
		        .log("Successfully saved sync response") //
		        .endChoice()
		        //
		        // <!-- TODO Log to a special failures log file or DB -->
		        .otherwise().log(LoggingLevel.WARN, "Unknown message was received: ${body}") //
		        .end()
		        //
		        .log(LoggingLevel.DEBUG, "Enabling message acknowledgement") //
		        .process("activeMqConsumerAcknowledgementProcessor");
		
	}
	
}
