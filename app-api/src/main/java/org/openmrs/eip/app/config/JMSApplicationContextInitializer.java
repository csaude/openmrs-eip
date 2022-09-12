package org.openmrs.eip.app.config;

import java.util.function.Supplier;

import javax.annotation.PostConstruct;
import javax.jms.ConnectionFactory;

import org.apache.activemq.RedeliveryPolicy;
import org.apache.activemq.spring.ActiveMQConnectionFactory;
import org.apache.camel.component.jms.JmsComponent;
import org.openmrs.eip.app.SyncConstants;
import org.openmrs.eip.app.management.entity.JMSBroker;
import org.openmrs.eip.app.management.repository.JMSBrokerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.connection.JmsTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.GenericWebApplicationContext;

@Component(SyncConstants.JMS_INITIALIZER_BEAN_NAME)
@DependsOn(SyncConstants.LIQUIBASE_BEAN_NAME)
public class JMSApplicationContextInitializer {
	
	// TODO: move to system property
	private static final long REDELIVERY_DELAY = 300000;
	
	private static final String JMS_COMPONENT_ID = "%sJmsComponent";
	
	private static final String JMS_TRANSACTION_MANAGER_ID = "%sJmsTransactionManager";
	
	private static final String CONNECTION_FACTORY_ID = "%sActiveMqConnFactory";
	
	@Autowired
	private JMSBrokerRepository jmsBrokerRepository;
	
	@Autowired
	private GenericWebApplicationContext appContext;
	
	@PostConstruct
	public void initialize() {
		jmsBrokerRepository.findByDisabledFalse().forEach(broker -> loadActiveMQBroker(broker));
	}
	
	private void loadActiveMQBroker(JMSBroker jmsBroker) {
		
		Supplier<CachingConnectionFactory> activeMqConnectionFactorySupplier = () -> {
			ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory();
			String url = "tcp://" + jmsBroker.getHost() + ":" + jmsBroker.getPort();
			cf.setBrokerURL(url);
			cf.setUserName(jmsBroker.getUsername());
			cf.setPassword(jmsBroker.getPassword());
			
			RedeliveryPolicy redeliveryPolicy = new RedeliveryPolicy();
			redeliveryPolicy.setMaximumRedeliveries(RedeliveryPolicy.NO_MAXIMUM_REDELIVERIES);
			redeliveryPolicy.setInitialRedeliveryDelay(REDELIVERY_DELAY);
			redeliveryPolicy.setRedeliveryDelay(REDELIVERY_DELAY);
			cf.setRedeliveryPolicy(redeliveryPolicy);
			
			return new CachingConnectionFactory(cf);
		};
		
		String connectionFactoryId = getConnectionFactoryId(jmsBroker);
		String jmsTransactionManagerId = getJmsTransactionManagerId(jmsBroker);
		String jmsComponentId = getJmsComponentId(jmsBroker);
		
		appContext.registerBean(connectionFactoryId, CachingConnectionFactory.class, activeMqConnectionFactorySupplier);
		
		appContext.registerBean(jmsTransactionManagerId, JmsTransactionManager.class, () -> {
			JmsTransactionManager jmsTransactionManager = new JmsTransactionManager();
			jmsTransactionManager.setConnectionFactory(appContext.getBean(connectionFactoryId, ConnectionFactory.class));
			return jmsTransactionManager;
		});
		
		appContext.registerBean(jmsComponentId, JmsComponent.class, () -> {
			return JmsComponent.jmsComponentTransacted(appContext.getBean(connectionFactoryId, ConnectionFactory.class),
			    appContext.getBean(jmsTransactionManagerId, JmsTransactionManager.class));
		});
	}
	
	public static String getJmsComponentId(JMSBroker jmsBroker) {
		return String.format(JMS_COMPONENT_ID, jmsBroker.getIdentifier());
	}
	
	public static String getJmsTransactionManagerId(JMSBroker jmsBroker) {
		return String.format(JMS_TRANSACTION_MANAGER_ID, jmsBroker.getIdentifier());
	}
	
	public static String getConnectionFactoryId(JMSBroker jmsBroker) {
		return String.format(CONNECTION_FACTORY_ID, jmsBroker.getIdentifier());
	}
}
