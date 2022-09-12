package org.openmrs.eip.app.route.sender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openmrs.eip.app.route.sender.BaseSenderRouteTest.SENDER_ID;
import static org.openmrs.eip.app.route.sender.SenderActiveMqPublisherRouteSingleDestinationTest.URI_ACTIVEMQ_SYNC;
import static org.openmrs.eip.app.sender.SenderConstants.PROP_SENDER_ID;
import static org.openmrs.eip.app.sender.SenderConstants.ROUTE_ID_ACTIVEMQ_PUBLISHER;
import static org.testcontainers.utility.MountableFile.forClasspathResource;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.camel.EndpointInject;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.ScriptDefinition;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openmrs.eip.app.config.JMSApplicationContextInitializer;
import org.openmrs.eip.app.management.entity.JMSBroker;
import org.openmrs.eip.app.management.entity.SyncResponseModel;
import org.openmrs.eip.app.management.repository.JMSBrokerRepository;
import org.openmrs.eip.app.sender.SenderActiveMqConsumerRouteBuilder;
import org.openmrs.eip.component.utils.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.lifecycle.Startables;

@TestPropertySource(properties = PROP_SENDER_ID + "=" + SENDER_ID)
@TestPropertySource(properties = "camel.output.endpoint=" + URI_ACTIVEMQ_SYNC)
@TestPropertySource(properties = "logging.level." + ROUTE_ID_ACTIVEMQ_PUBLISHER + "=DEBUG")
public class SenderActiveMqPublisherRouteSingleDestinationTest extends BaseSenderRouteTest {
	
	private static final String RECEIVER_ID = "default";
	
	public static final String URI_ACTIVEMQ_SYNC = "activemq:openmrs.sync." + SENDER_ID + "?connectionFactory=%s";
	
	private static final String TEST_LISTENER = "mock:listener";
	
	private static final GenericContainer<?> activemq = new GenericContainer<>(ACTIVEMQ_IMAGE)
	        .withExposedPorts(ACTIVEMQ_PORT);
	
	private static boolean initialStaticSetupDone = false;
	
	@EndpointInject(TEST_LISTENER)
	private MockEndpoint mockListener;
	
	@Autowired
	private JMSBrokerRepository jmsBrokerRepository;
	
	@Autowired
	private JMSApplicationContextInitializer jmsApplicationContextInitializer;
	
	@Override
	public String getTestRouteFilename() {
		return ROUTE_ID_ACTIVEMQ_PUBLISHER;
	}
	
	// TODO: purge all activemq messages after each test
	//	@After
	//	public void cleanup() {
	//	}
	
	@BeforeClass
	public static void startActivemq() throws Exception {
		for (String fileName : Arrays.asList("artemis-roles.properties", "artemis-users.properties")) {
			activemq.withCopyFileToContainer(forClasspathResource(fileName), "/var/lib/artemis/etc/" + fileName);
		}
		
		Startables.deepStart(activemq).join();
	}
	
	@Before
	public void setup() throws Exception {
		createJMSBroker(RECEIVER_ID, activemq.getContainerIpAddress(), activemq.getFirstMappedPort(), "admin",
		    "admin");
		
		// we need to execute this once, but it cannot be static code because it depends on non static resources
		if (!initialStaticSetupDone) {
			jmsApplicationContextInitializer.initialize();
			
			loadRoute(new SenderActiveMqConsumerRouteBuilder(jmsBrokerRepository, env));
			
			initialStaticSetupDone = true;
		}
	}
	
	@Test
	public void shouldSendAndConsumeInOrder() throws Exception {
		List<JMSBroker> brokers = jmsBrokerRepository.findByDisabledFalse();
		assertThat(brokers).hasSize(1);
		
		JMSBroker broker = brokers.get(0);
		
		String camelOutputEndpointForReceiver = String.format(URI_ACTIVEMQ_SYNC,
		    JMSApplicationContextInitializer.getConnectionFactoryId(broker));
		
		advise("sender-activemq-consumer_" + RECEIVER_ID, new AdviceWithRouteBuilder() {
			
			@Override
			public void configure() {
				weaveByType(ScriptDefinition.class).selectLast().after().to(TEST_LISTENER);
			}
			
		});
		
		List<String> messageUuids = new ArrayList<>();
		mockListener.whenAnyExchangeReceived(e -> {
			messageUuids.add(e.getProperty("messageUuid", String.class));
		});
		mockListener.setAssertPeriod(1000);
		mockListener.setExpectedCount(2);
		
		SyncResponseModel syncResponse1 = new SyncResponseModel();
		syncResponse1.setDateSentByReceiver(LocalDateTime.now());
		syncResponse1.setMessageUuid("uuid-1");
		SyncResponseModel syncResponse2 = new SyncResponseModel();
		syncResponse2.setDateSentByReceiver(LocalDateTime.now());
		syncResponse2.setMessageUuid("uuid-2");
		
		producerTemplate.sendBody(camelOutputEndpointForReceiver, JsonUtils.marshall(syncResponse1));
		producerTemplate.sendBody(camelOutputEndpointForReceiver, JsonUtils.marshall(syncResponse2));
		
		mockListener.assertIsSatisfied();
		
		assertThat(messageUuids).hasSize(2);
		assertThat(messageUuids.get(0)).isEqualTo(syncResponse1.getMessageUuid());
		assertThat(messageUuids.get(1)).isEqualTo(syncResponse2.getMessageUuid());
	}
	
}
