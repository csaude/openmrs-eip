package org.openmrs.eip.app.route.sender;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.openmrs.eip.app.SyncConstants.MGT_DATASOURCE_NAME;
import static org.openmrs.eip.app.SyncConstants.MGT_TX_MGR;
import static org.openmrs.eip.app.route.sender.SenderActiveMqPublisherRouteTest.SENDER_ID;
import static org.openmrs.eip.app.route.sender.SenderActiveMqPublisherRouteTest.URI_ACTIVEMQ_SYNC;
import static org.openmrs.eip.app.route.sender.SenderTestUtils.getEntities;
import static org.openmrs.eip.app.sender.SenderConstants.PROP_CAMEL_OUTPUT_ENDPOINT;
import static org.openmrs.eip.app.sender.SenderConstants.PROP_SENDER_ID;
import static org.openmrs.eip.app.sender.SenderConstants.ROUTE_ID_RESPONSE_PROCESSOR;
import static org.openmrs.eip.app.sender.SenderConstants.URI_RESPONSE_PROCESSOR;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import org.apache.camel.support.DefaultExchange;
import org.junit.Test;
import org.openmrs.eip.app.management.entity.SenderSyncMessage;
import org.openmrs.eip.app.management.entity.SenderSyncResponse;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;

import ch.qos.logback.classic.Level;

@TestPropertySource(properties = "logging.level." + ROUTE_ID_RESPONSE_PROCESSOR + "=DEBUG")
@TestPropertySource(properties = PROP_SENDER_ID + "=" + SENDER_ID)
@TestPropertySource(properties = PROP_CAMEL_OUTPUT_ENDPOINT + "=" + URI_ACTIVEMQ_SYNC)
public class SenderResponseProcessorRouteTest extends BaseSenderRouteTest {
	
	@Override
	public String getTestRouteFilename() {
		return ROUTE_ID_RESPONSE_PROCESSOR;
	}
	
	private SenderSyncResponse createSyncResponse(String messageUuid) {
		SenderSyncResponse response = new SenderSyncResponse();
		response.setMessageUuid(messageUuid);
		response.setDateSentByReceiver(LocalDateTime.now());
		response.setDateCreated(new Date());
		SenderTestUtils.saveEntity(response);
		return response;
	}
	
	@Test
	public void shouldDoNothingIfNoSyncResponsesAreFound() {
		producerTemplate.send(URI_RESPONSE_PROCESSOR, new DefaultExchange(camelContext));
		
		assertMessageLogged(Level.DEBUG, "No sender sync responses was found");
	}
	
	@Test
	@Sql(scripts = "classpath:mgt_sender_sync_message.sql", config = @SqlConfig(dataSource = MGT_DATASOURCE_NAME, transactionManager = MGT_TX_MGR))
	@Sql(scripts = "classpath:mgt_sender_sync_response.sql", config = @SqlConfig(dataSource = MGT_DATASOURCE_NAME, transactionManager = MGT_TX_MGR))
	public void shouldLoadAndProcessAllNewSyncResponsesSortedByDateCreated() {
		final int messageCount = 4;
		assertFalse(getEntities(SenderSyncMessage.class).isEmpty());
		List<SenderSyncResponse> responses = getEntities(SenderSyncResponse.class);
		assertEquals(messageCount, responses.size());
		assertTrue(responses.get(0).getDateCreated().getTime() > (responses.get(2).getDateCreated().getTime()));
		assertTrue(responses.get(1).getDateCreated().getTime() > (responses.get(2).getDateCreated().getTime()));
		DefaultExchange exchange = new DefaultExchange(camelContext);
		
		producerTemplate.send(URI_RESPONSE_PROCESSOR, exchange);
		
		assertMessageLogged(Level.INFO, "Fetched " + responses.size() + " sender sync response(s)");
		assertEquals(messageCount, exchange.getIn().getBody(List.class).size());
		assertEquals(3, ((SenderSyncResponse) exchange.getIn().getBody(List.class).get(0)).getId().intValue());
		assertEquals(4, ((SenderSyncResponse) exchange.getIn().getBody(List.class).get(1)).getId().intValue());
		assertEquals(1, ((SenderSyncResponse) exchange.getIn().getBody(List.class).get(2)).getId().intValue());
		assertEquals(2, ((SenderSyncResponse) exchange.getIn().getBody(List.class).get(3)).getId().intValue());
		assertTrue(getEntities(SenderSyncResponse.class).isEmpty());
		assertTrue(getEntities(SenderSyncMessage.class).isEmpty());
	}
	
	@Test
	@Sql(scripts = "classpath:mgt_sender_sync_message.sql", config = @SqlConfig(dataSource = MGT_DATASOURCE_NAME, transactionManager = MGT_TX_MGR))
	@Sql(scripts = "classpath:mgt_sender_sync_response.sql", config = @SqlConfig(dataSource = MGT_DATASOURCE_NAME, transactionManager = MGT_TX_MGR))
	public void shouldProcessAResponseForASyncMessageWithAMessageUuidMatches() {
		final String msgUuid = "26beb8bd-287c-47f2-9786-a7b98c933c04";
		SenderSyncMessage msg = SenderTestUtils.getEntity(SenderSyncMessage.class, 2L);
		assertEquals(msgUuid, msg.getMessageUuid());
		SenderSyncResponse response = SenderTestUtils.getEntity(SenderSyncResponse.class, 2L);
		assertEquals(msgUuid, response.getMessageUuid());
		
		producerTemplate.send(URI_RESPONSE_PROCESSOR, new DefaultExchange(camelContext));
		
		assertNull(SenderTestUtils.getEntity(SenderSyncMessage.class, msg.getId()));
		assertNull(SenderTestUtils.getEntity(SenderSyncResponse.class, response.getId()));
	}
	
	@Test
	public void shouldProcessAResponseAndTheSyncMessageIsNotFound() {
		final String msgUuid = "msg-uuid";
		assertTrue(getEntities(SenderSyncResponse.class).isEmpty());
		createSyncResponse(msgUuid);
		assertEquals(1, getEntities(SenderSyncResponse.class).size());
		
		producerTemplate.send(URI_RESPONSE_PROCESSOR, new DefaultExchange(camelContext));
		
		assertMessageLogged(Level.INFO, "No Sender sync message was found with uuid " + msgUuid);
		assertTrue(getEntities(SenderSyncResponse.class).isEmpty());
	}
	
}
