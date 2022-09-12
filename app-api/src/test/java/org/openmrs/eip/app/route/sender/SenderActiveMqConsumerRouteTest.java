package org.openmrs.eip.app.route.sender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.openmrs.eip.app.route.sender.BaseSenderRouteTest.URI_ACTIVEMQ_SYNC;
import static org.openmrs.eip.app.sender.SenderConstants.PROP_CAMEL_OUTPUT_ENDPOINT;
import static org.testcontainers.utility.MountableFile.forClasspathResource;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openmrs.eip.app.CustomMessageListenerContainer;
import org.openmrs.eip.app.config.JMSApplicationContextInitializer;
import org.openmrs.eip.app.management.entity.JMSBroker;
import org.openmrs.eip.app.management.entity.SenderSyncRequest;
import org.openmrs.eip.app.management.entity.SenderSyncRequest.SenderRequestStatus;
import org.openmrs.eip.app.management.entity.SenderSyncResponse;
import org.openmrs.eip.app.management.entity.SyncRequestModel;
import org.openmrs.eip.app.management.entity.SyncResponseModel;
import org.openmrs.eip.app.management.repository.SenderSyncRequestRepository;
import org.openmrs.eip.app.management.repository.SenderSyncResponseRepository;
import org.openmrs.eip.app.route.TestUtils;
import org.openmrs.eip.app.sender.ActiveMqConsumerAcknowledgementProcessor;
import org.openmrs.eip.app.sender.SenderActiveMqConsumerRouteBuilder;
import org.openmrs.eip.component.utils.JsonUtils;
import org.powermock.reflect.Whitebox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.lifecycle.Startables;

import ch.qos.logback.classic.Level;

@TestPropertySource(properties = PROP_CAMEL_OUTPUT_ENDPOINT + "=" + URI_ACTIVEMQ_SYNC)
public class SenderActiveMqConsumerRouteTest extends BaseSenderRouteTest {
	
	private static final GenericContainer<?> activeMq = new GenericContainer<>(ACTIVEMQ_IMAGE)
	        .withExposedPorts(ACTIVEMQ_PORT);
	
	public static final String URI_ACTIVEMQ_SYNC = "activemq:openmrs.sync." + SENDER_ID + "?connectionFactory=%s";
	
	@Autowired
	private SenderSyncRequestRepository senderSyncRequestRepository;
	
	@Autowired
	private SenderSyncResponseRepository senderSyncResponseRepository;
	
	@Autowired
	private JMSApplicationContextInitializer jmsApplicationContextInitializer;
	
	private static boolean initialStaticSetupDone = false;
	
	@BeforeClass
	public static void startActivemq() throws Exception {
		for (String fileName : Arrays.asList("artemis-roles.properties", "artemis-users.properties")) {
			activeMq.withCopyFileToContainer(forClasspathResource(fileName), "/var/lib/artemis/etc/" + fileName);
		}
		
		Startables.deepStart(activeMq).join();
	}
	
	@AfterClass
	public static void stopActivemq() {
		activeMq.stop();
	}
	
	@Before
	public void setup() throws Exception {
		createJMSBroker("default", activeMq.getContainerIpAddress(), activeMq.getFirstMappedPort(), "admin", "admin");
		
		// we need to execute this once, but it cannot be static code because it depends on non static resources
		if (!initialStaticSetupDone) {
			jmsApplicationContextInitializer.initialize();
			
			loadRoute(new SenderActiveMqConsumerRouteBuilder(jmsBrokerRepository, env));
			
			initialStaticSetupDone = true;
		}
	}
	
	@Override
	public String getTestRouteFilename() {
		return null;
	}
	
	@Test
	public void shouldProcessAndSaveASyncRequestMessage() throws Exception {
		String camelOutputEndpoint = getOutputEndpoint();
		
		assertEquals(0, senderSyncRequestRepository.count());
		
		final String table = "visit";
		final String uuid = "entity-uuid";
		final String requestUuid = "sync-request-uuid";
		
		SyncRequestModel requestData = new SyncRequestModel();
		requestData.setTableName(table);
		requestData.setIdentifier(uuid);
		requestData.setRequestUuid(requestUuid);
		
		producerTemplate.sendBody(camelOutputEndpoint, JsonUtils.marshall(requestData));
		
		await().atMost(3, TimeUnit.SECONDS).until(() -> senderSyncRequestRepository.count() == 1);
		
		List<SenderSyncRequest> requests = TestUtils.getEntities(SenderSyncRequest.class);
		assertEquals(1, requests.size());
		SenderSyncRequest savedRequest = requests.get(0);
		assertEquals(table, savedRequest.getTableName());
		assertEquals(uuid, savedRequest.getIdentifier());
		assertEquals(requestUuid, savedRequest.getRequestUuid());
		assertEquals(SenderRequestStatus.NEW, savedRequest.getStatus());
		assertFalse(savedRequest.getFound());
		assertNotNull(savedRequest.getDateCreated());
		assertNull(savedRequest.getDateProcessed());
		
		assertListenerCommitStatusTrue();
	}
	
	@Test
	public void shouldProcessAndSaveASyncResponseMessage() {
		String camelOutputEndpoint = getOutputEndpoint();
		
		final String messageUuid = "message-uuid";
		final LocalDateTime dateSent = LocalDateTime.now();
		SyncResponseModel responseData = new SyncResponseModel();
		responseData.setMessageUuid(messageUuid);
		responseData.setDateSentByReceiver(dateSent);
		assertEquals(0, senderSyncRequestRepository.count());
		
		producerTemplate.sendBody(camelOutputEndpoint, JsonUtils.marshall(responseData));
		
		await().atMost(3, TimeUnit.SECONDS).until(() -> senderSyncResponseRepository.count() == 1);
		
		List<SenderSyncResponse> responses = TestUtils.getEntities(SenderSyncResponse.class);
		SenderSyncResponse savedResponse = responses.get(0);
		assertEquals(messageUuid, savedResponse.getMessageUuid());
		assertEquals(dateSent, savedResponse.getDateSentByReceiver());
		assertNotNull(savedResponse.getDateCreated());
		
		assertListenerCommitStatusTrue();
	}
	
	@Test
	public void shouldSkipAMessageThatIsNotASyncRequestOrResponse() {
		String camelOutputEndpoint = getOutputEndpoint();
		
		assertEquals(0, senderSyncRequestRepository.count());
		
		final String testMsg = "{}";
		producerTemplate.sendBody(camelOutputEndpoint, JsonUtils.marshall(testMsg));
		
		await().atMost(3, TimeUnit.SECONDS)
		        .until(() -> getMessageLoggedCount(Level.WARN, "Unknown message was received: \"" + testMsg + "\"") == 1);
		
		assertListenerCommitStatusTrue();
	}
	
	private void assertListenerCommitStatusTrue() {
		Map<Integer, CustomMessageListenerContainer> listeners = Whitebox
		        .getInternalState(ActiveMqConsumerAcknowledgementProcessor.class, "listeners");
		assertEquals(1, listeners.values().size());
		assertTrue(Whitebox.getInternalState(listeners.values().iterator().next(), "commit"));
	}
	
	private String getOutputEndpoint() {
		List<JMSBroker> brokers = jmsBrokerRepository.findByDisabledFalse();
		assertThat(brokers).hasSize(1);
		
		JMSBroker broker = brokers.get(0);
		
		return String.format(URI_ACTIVEMQ_SYNC, JMSApplicationContextInitializer.getConnectionFactoryId(broker));
	}
	
}
