package org.openmrs.eip.app.route.sender;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.openmrs.eip.app.route.sender.BaseSenderRouteTest.SENDER_ID;
import static org.openmrs.eip.app.sender.SenderConstants.EX_PROP_EVENT;
import static org.openmrs.eip.app.sender.SenderConstants.PROP_SENDER_ID;
import static org.openmrs.eip.app.sender.SenderConstants.ROUTE_ID_ACTIVEMQ_PUBLISHER;
import static org.openmrs.eip.app.sender.SenderConstants.URI_DBSYNC;
import static org.openmrs.eip.app.sender.SenderConstants.URI_RESPONSE_PROCESSOR;
import static org.testcontainers.utility.MountableFile.forClasspathResource;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.support.DefaultExchange;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openmrs.eip.app.config.JMSApplicationContextInitializer;
import org.openmrs.eip.app.management.entity.SenderSyncMessage;
import org.openmrs.eip.app.management.entity.SenderSyncMessage.SenderSyncMessageStatus;
import org.openmrs.eip.app.management.entity.SyncResponseModel;
import org.openmrs.eip.app.management.repository.SenderSyncMessageRepository;
import org.openmrs.eip.app.management.repository.SenderSyncResponseRepository;
import org.openmrs.eip.app.sender.SenderActiveMqConsumerRouteBuilder;
import org.openmrs.eip.app.sender.SenderConstants;
import org.openmrs.eip.component.DatabaseOperation;
import org.openmrs.eip.component.SyncContext;
import org.openmrs.eip.component.entity.Event;
import org.openmrs.eip.component.utils.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.lifecycle.Startables;

@TestPropertySource(properties = PROP_SENDER_ID + "=" + SENDER_ID)
@TestPropertySource(properties = "camel.output.endpoint=activemq:openmrs.sync?connectionFactory=%s")
@TestPropertySource(properties = "logging.level." + ROUTE_ID_ACTIVEMQ_PUBLISHER + "=DEBUG")
public class SenderActiveMqPublisherRouteMultipleDestinationsTest extends BaseSenderRouteTest {
	
	public static final String URI_ACTIVEMQ_IN = "activemq:openmrs.sync." + SENDER_ID + "?connectionFactory=%s";
	
	private static final String TEST_LISTENER = "mock:listener";
	
	private static final GenericContainer<?> activemqServer1 = new GenericContainer<>(ACTIVEMQ_IMAGE)
	        .withExposedPorts(ACTIVEMQ_PORT);
	
	private static final GenericContainer<?> activemqServer2 = new GenericContainer<>(ACTIVEMQ_IMAGE)
	        .withExposedPorts(ACTIVEMQ_PORT);
	
	private static final String URI_ACTIVEMQ_PUBLISHER = "direct:sender-activemq-publisher";
	
	private static boolean initialStaticSetupDone = false;
	
	@EndpointInject(TEST_LISTENER)
	private MockEndpoint mockListener;
	
	@Autowired
	private SenderSyncMessageRepository senderSyncMessageRepository;
	
	@Autowired
	private SenderSyncResponseRepository senderSyncResponseRepository;
	
	@Autowired
	private JMSApplicationContextInitializer jmsApplicationContextInitializer;
	
	@Override
	public String getTestRouteFilename() {
		return ROUTE_ID_ACTIVEMQ_PUBLISHER;
	}
	
	// TODO: purge all activemq messages
	//	@After
	//	public void cleanup() {
	//	}
	
	@BeforeClass
	public static void startActivemq() {
		for (String fileName : Arrays.asList("artemis-roles.properties", "artemis-users.properties")) {
			activemqServer1.withCopyFileToContainer(forClasspathResource(fileName), "/var/lib/artemis/etc/" + fileName);
			activemqServer2.withCopyFileToContainer(forClasspathResource(fileName), "/var/lib/artemis/etc/" + fileName);
		}
		
		Startables.deepStart(Stream.of(activemqServer1, activemqServer2)).join();
	}
	
	@Before
	public void setup() throws Exception {
		createJMSBroker("server1", activemqServer1.getContainerIpAddress(), activemqServer1.getFirstMappedPort(),
		    "admin", "admin");
		createJMSBroker("server2", activemqServer2.getContainerIpAddress(), activemqServer2.getFirstMappedPort(),
		    "admin", "admin");
		
		// we need to execute this once, but it cannot be static code because it depends on non static resources
		if (!initialStaticSetupDone) {
			jmsApplicationContextInitializer.initialize();
			
			loadRoute(new SenderActiveMqConsumerRouteBuilder(jmsBrokerRepository, env));
			
			loadRoute("db-sync-route.xml");
			
			initialStaticSetupDone = true;
		}
	}
	
	@Test
	public void shouldSendToMultipleReceivers() throws Exception {
		// create one db event
		DefaultExchange exchange = new DefaultExchange(camelContext);
		Event event = createEvent("person", null, "person-uuid-1", DatabaseOperation.d.name());
		exchange.setProperty(EX_PROP_EVENT, event);
		
		// submit for processing
		producerTemplate.send(URI_DBSYNC, exchange);
		
		// assert if two messages was created
		List<SenderSyncMessage> messages = fetchMessages();
		assertEquals(2, messages.size());
		assertEquals(SenderSyncMessageStatus.NEW, messages.get(0).getStatus());
		assertEquals(SenderSyncMessageStatus.NEW, messages.get(1).getStatus());
		
		// call publisher
		producerTemplate.send(URI_ACTIVEMQ_PUBLISHER, new DefaultExchange(camelContext));
		
		// assert that messages was set to SENT
		messages = fetchMessages();
		assertEquals(2, messages.size());
		assertEquals(SenderSyncMessageStatus.SENT, messages.get(0).getStatus());
		assertEquals(SenderSyncMessageStatus.SENT, messages.get(1).getStatus());
		
		// submit hand-made receiver responses for each destinationF
		SyncResponseModel responseServer1 = new SyncResponseModel();
		responseServer1.setDateSentByReceiver(LocalDateTime.now());
		responseServer1.setMessageUuid(messages.get(0).getMessageUuid());
		
		SyncResponseModel responseServer2 = new SyncResponseModel();
		responseServer2.setDateSentByReceiver(LocalDateTime.now());
		responseServer2.setMessageUuid(messages.get(1).getMessageUuid());
		
		// simulate receiver responses to the appropriate activemq servers and queues
		String camelOutputEndpointForReceiver1 = String.format(URI_ACTIVEMQ_IN,
		    JMSApplicationContextInitializer.getConnectionFactoryId(messages.get(0).getBroker()));
		String camelOutputEndpointForReceiver2 = String.format(URI_ACTIVEMQ_IN,
		    JMSApplicationContextInitializer.getConnectionFactoryId(messages.get(1).getBroker()));
		
		// submit 
		producerTemplate.sendBody(camelOutputEndpointForReceiver1, JsonUtils.marshall(responseServer1));
		producerTemplate.sendBody(camelOutputEndpointForReceiver2, JsonUtils.marshall(responseServer2));
		
		await().atMost(3, TimeUnit.SECONDS).until(() -> senderSyncResponseRepository.count() == 2);
		
		// load processor route
		loadRoute(SenderConstants.ROUTE_ID_RESPONSE_PROCESSOR + ".xml");
		producerTemplate.send(URI_RESPONSE_PROCESSOR, new DefaultExchange(camelContext));
		
		// assert that responses was processed and senderSyncMessages removed
		assertEquals(0, senderSyncResponseRepository.count());
		assertEquals(0, senderSyncMessageRepository.count());
	}
	
	@SuppressWarnings("unchecked")
	public List<SenderSyncMessage> fetchMessages() {
		ProducerTemplate t = SyncContext.getBean(ProducerTemplate.class);
		final String classname = SenderSyncMessage.class.getSimpleName();
		
		return t.requestBody(
		    "jpa:" + classname + "?query=SELECT distinct(i) FROM " + classname + " i inner join fetch i.broker",
		    null, List.class);
	}
	
}
