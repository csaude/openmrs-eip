package org.openmrs.eip.app.route.sender;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.openmrs.eip.app.route.TestUtils.getEntities;
import static org.openmrs.eip.app.sender.SenderConstants.EX_PROP_DBZM_EVENT;
import static org.openmrs.eip.app.sender.SenderConstants.EX_PROP_EVENT;
import static org.openmrs.eip.app.sender.SenderConstants.EX_PROP_RETRY_ITEM;
import static org.openmrs.eip.app.sender.SenderConstants.ROUTE_ID_DBSYNC;
import static org.openmrs.eip.app.sender.SenderConstants.URI_DBSYNC;

import java.util.Date;
import java.util.List;

import org.apache.camel.support.DefaultExchange;
import org.junit.Test;
import org.openmrs.eip.app.management.entity.sender.DebeziumEvent;
import org.openmrs.eip.app.management.entity.sender.SenderRetryQueueItem;
import org.openmrs.eip.app.management.entity.sender.SenderSyncMessage;
import org.openmrs.eip.app.management.entity.sender.SenderSyncMessage.SenderSyncMessageStatus;
import org.openmrs.eip.app.management.repository.DebeziumEventRepository;
import org.openmrs.eip.app.management.repository.SenderRetryRepository;
import org.openmrs.eip.app.route.TestUtils;
import org.openmrs.eip.component.exception.EIPException;
import org.openmrs.eip.component.model.PatientModel;
import org.openmrs.eip.component.model.PersonModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;

import com.jayway.jsonpath.JsonPath;

import ch.qos.logback.classic.Level;

@TestPropertySource(properties = "logging.level." + ROUTE_ID_DBSYNC + "=DEBUG")
@Sql(scripts = { "classpath:openmrs_core_data.sql", "classpath:openmrs_patient.sql" })
public class DbSyncRouteTest extends BaseSenderRouteTest {
	
	@Autowired
	private DebeziumEventRepository eventRepo;
	
	@Autowired
	private SenderRetryRepository retryRepo;
	
	@Override
	public String getTestRouteFilename() {
		return "db-sync-route";
	}
	
	@Test
	public void shouldProcessAnInsertEventAndDeleteIt() {
		assertTrue(TestUtils.getEntities(SenderSyncMessage.class).isEmpty());
		DefaultExchange exchange = new DefaultExchange(camelContext);
		final String table = "patient";
		final String uuid = "abfd940e-32dc-491f-8038-a8f3afe3e35b";
		final String op = "c";
		final boolean snapshot = true;
		DebeziumEvent debeziumEvent = createDebeziumEvent(table, "1", uuid, op);
		debeziumEvent.setDateCreated(new Date());
		eventRepo.save(debeziumEvent);
		debeziumEvent.getEvent().setSnapshot(snapshot);
		exchange.setProperty(EX_PROP_EVENT, debeziumEvent.getEvent());
		exchange.setProperty(EX_PROP_DBZM_EVENT, debeziumEvent);
		
		producerTemplate.send(URI_DBSYNC, exchange);
		
		assertFalse(eventRepo.existsById(debeziumEvent.getId()));
		List<SenderSyncMessage> syncMsgs = TestUtils.getEntities(SenderSyncMessage.class);
		assertEquals(1, syncMsgs.size());
		SenderSyncMessage msg = syncMsgs.get(0);
		assertEquals(table, msg.getTableName());
		assertEquals(uuid, msg.getIdentifier());
		assertEquals(op, msg.getOperation());
		assertEquals(snapshot, msg.getSnapshot());
		assertEquals(SenderSyncMessageStatus.NEW, msg.getStatus());
		assertNotNull(msg.getMessageUuid());
		assertNotNull(msg.getDateCreated());
		assertEquals(msg.getEventDate().getTime(), debeziumEvent.getDateCreated().getTime());
		assertNull(msg.getRequestUuid());
		assertNull(msg.getDateSent());
		assertEquals(PatientModel.class.getName(), JsonPath.read(msg.getData(), "tableToSyncModelClass"));
		assertEquals(uuid, JsonPath.read(msg.getData(), "model.uuid"));
		assertEquals(op, JsonPath.read(msg.getData(), "metadata.operation"));
		assertEquals(msg.getMessageUuid(), JsonPath.read(msg.getData(), "metadata.messageUuid"));
		assertTrue(JsonPath.read(msg.getData(), "metadata.snapshot"));
		assertNull(JsonPath.read(msg.getData(), "metadata.sourceIdentifier"));
		assertNull(JsonPath.read(msg.getData(), "metadata.dateSent"));
		assertNull(JsonPath.read(msg.getData(), "metadata.requestUuid"));
	}
	
	@Test
	public void shouldProcessASnapshotReadEventAndDeleteIt() {
		assertTrue(TestUtils.getEntities(SenderSyncMessage.class).isEmpty());
		DefaultExchange exchange = new DefaultExchange(camelContext);
		final String table = "patient";
		final String uuid = "abfd940e-32dc-491f-8038-a8f3afe3e35b";
		final String op = "s";
		final boolean snapshot = true;
		DebeziumEvent debeziumEvent = createDebeziumEvent(table, "1", uuid, op);
		debeziumEvent.setDateCreated(new Date());
		eventRepo.save(debeziumEvent);
		debeziumEvent.getEvent().setSnapshot(snapshot);
		exchange.setProperty(EX_PROP_EVENT, debeziumEvent.getEvent());
		exchange.setProperty(EX_PROP_DBZM_EVENT, debeziumEvent);
		
		producerTemplate.send(URI_DBSYNC, exchange);
		
		assertFalse(eventRepo.existsById(debeziumEvent.getId()));
		List<SenderSyncMessage> syncMsgs = TestUtils.getEntities(SenderSyncMessage.class);
		assertEquals(1, syncMsgs.size());
		SenderSyncMessage msg = syncMsgs.get(0);
		assertEquals(table, msg.getTableName());
		assertEquals(uuid, msg.getIdentifier());
		assertEquals(op, msg.getOperation());
		assertEquals(snapshot, msg.getSnapshot());
		assertEquals(SenderSyncMessageStatus.NEW, msg.getStatus());
		assertNotNull(msg.getMessageUuid());
		assertNotNull(msg.getDateCreated());
		assertEquals(msg.getEventDate().getTime(), debeziumEvent.getDateCreated().getTime());
		assertNull(msg.getRequestUuid());
		assertNull(msg.getDateSent());
		assertEquals(PatientModel.class.getName(), JsonPath.read(msg.getData(), "tableToSyncModelClass"));
		assertEquals(uuid, JsonPath.read(msg.getData(), "model.uuid"));
		assertEquals(op, JsonPath.read(msg.getData(), "metadata.operation"));
		assertEquals(msg.getMessageUuid(), JsonPath.read(msg.getData(), "metadata.messageUuid"));
		assertTrue(JsonPath.read(msg.getData(), "metadata.snapshot"));
		assertNull(JsonPath.read(msg.getData(), "metadata.sourceIdentifier"));
		assertNull(JsonPath.read(msg.getData(), "metadata.dateSent"));
		assertNull(JsonPath.read(msg.getData(), "metadata.requestUuid"));
	}
	
	@Test
	public void shouldProcessAnUpdateEventAndDeleteIt() {
		assertTrue(TestUtils.getEntities(SenderSyncMessage.class).isEmpty());
		DefaultExchange exchange = new DefaultExchange(camelContext);
		final String table = "patient";
		final String uuid = "abfd940e-32dc-491f-8038-a8f3afe3e35b";
		final String op = "u";
		DebeziumEvent debeziumEvent = createDebeziumEvent(table, "1", uuid, op);
		debeziumEvent.setDateCreated(new Date());
		eventRepo.save(debeziumEvent);
		exchange.setProperty(EX_PROP_EVENT, debeziumEvent.getEvent());
		exchange.setProperty(EX_PROP_DBZM_EVENT, debeziumEvent);
		
		producerTemplate.send(URI_DBSYNC, exchange);
		
		assertFalse(eventRepo.existsById(debeziumEvent.getId()));
		List<SenderSyncMessage> syncMsgs = TestUtils.getEntities(SenderSyncMessage.class);
		assertEquals(1, syncMsgs.size());
		SenderSyncMessage msg = syncMsgs.get(0);
		assertEquals(table, msg.getTableName());
		assertEquals(uuid, msg.getIdentifier());
		assertEquals(op, msg.getOperation());
		assertFalse(msg.getSnapshot());
		assertEquals(SenderSyncMessageStatus.NEW, msg.getStatus());
		assertNotNull(msg.getMessageUuid());
		assertNotNull(msg.getDateCreated());
		assertEquals(msg.getEventDate().getTime(), debeziumEvent.getDateCreated().getTime());
		assertNull(msg.getRequestUuid());
		assertNull(msg.getDateSent());
		assertEquals(PatientModel.class.getName(), JsonPath.read(msg.getData(), "tableToSyncModelClass"));
		assertEquals(uuid, JsonPath.read(msg.getData(), "model.uuid"));
		assertEquals(op, JsonPath.read(msg.getData(), "metadata.operation"));
		assertEquals(msg.getMessageUuid(), JsonPath.read(msg.getData(), "metadata.messageUuid"));
		assertFalse(JsonPath.read(msg.getData(), "metadata.snapshot"));
		assertNull(JsonPath.read(msg.getData(), "metadata.sourceIdentifier"));
		assertNull(JsonPath.read(msg.getData(), "metadata.dateSent"));
		assertNull(JsonPath.read(msg.getData(), "metadata.requestUuid"));
	}
	
	@Test
	public void shouldProcessADeleteEventAndDeleteIt() {
		assertTrue(TestUtils.getEntities(SenderSyncMessage.class).isEmpty());
		DefaultExchange exchange = new DefaultExchange(camelContext);
		final String table = "person";
		final String uuid = "person-uuid";
		final String op = "d";
		DebeziumEvent debeziumEvent = createDebeziumEvent(table, "1", uuid, op);
		debeziumEvent.setDateCreated(new Date());
		eventRepo.save(debeziumEvent);
		exchange.setProperty(EX_PROP_EVENT, debeziumEvent.getEvent());
		exchange.setProperty(EX_PROP_DBZM_EVENT, debeziumEvent);
		
		producerTemplate.send(URI_DBSYNC, exchange);
		
		assertFalse(eventRepo.existsById(debeziumEvent.getId()));
		List<SenderSyncMessage> syncMsgs = TestUtils.getEntities(SenderSyncMessage.class);
		assertEquals(1, syncMsgs.size());
		SenderSyncMessage msg = syncMsgs.get(0);
		assertEquals(table, msg.getTableName());
		assertEquals(uuid, msg.getIdentifier());
		assertEquals(op, msg.getOperation());
		assertFalse(msg.getSnapshot());
		assertEquals(SenderSyncMessageStatus.NEW, msg.getStatus());
		assertNotNull(msg.getMessageUuid());
		assertNotNull(msg.getDateCreated());
		assertEquals(msg.getEventDate().getTime(), debeziumEvent.getDateCreated().getTime());
		assertNull(msg.getRequestUuid());
		assertNull(msg.getDateSent());
		assertEquals(PersonModel.class.getName(), JsonPath.read(msg.getData(), "tableToSyncModelClass"));
		assertEquals(uuid, JsonPath.read(msg.getData(), "model.uuid"));
		assertEquals(op, JsonPath.read(msg.getData(), "metadata.operation"));
		assertEquals(msg.getMessageUuid(), JsonPath.read(msg.getData(), "metadata.messageUuid"));
		assertFalse(JsonPath.read(msg.getData(), "metadata.snapshot"));
		assertNull(JsonPath.read(msg.getData(), "metadata.sourceIdentifier"));
		assertNull(JsonPath.read(msg.getData(), "metadata.dateSent"));
		assertNull(JsonPath.read(msg.getData(), "metadata.requestUuid"));
	}
	
	@Test
	public void shouldProcessARetryItemAndDeleteIt() {
		assertTrue(TestUtils.getEntities(SenderSyncMessage.class).isEmpty());
		DefaultExchange exchange = new DefaultExchange(camelContext);
		final String table = "patient";
		final String uuid = "abfd940e-32dc-491f-8038-a8f3afe3e35b";
		final String op = "u";
		SenderRetryQueueItem retry = new SenderRetryQueueItem();
		retry.setEvent(createEvent(table, "101", uuid, op));
		retry.setDateCreated(new Date());
		retry.setAttemptCount(1);
		retry.setExceptionType(EIPException.class.getName());
		retry.setEventDate(new Date());
		retryRepo.save(retry);
		assertEquals(1, getEntities(SenderRetryQueueItem.class).size());
		exchange.setProperty(EX_PROP_EVENT, retry.getEvent());
		exchange.setProperty(EX_PROP_RETRY_ITEM, retry);
		
		producerTemplate.send(URI_DBSYNC, exchange);
		
		assertFalse(retryRepo.existsById(retry.getId()));
		List<SenderSyncMessage> syncMsgs = TestUtils.getEntities(SenderSyncMessage.class);
		assertEquals(1, syncMsgs.size());
		SenderSyncMessage msg = syncMsgs.get(0);
		assertEquals(table, msg.getTableName());
		assertEquals(uuid, msg.getIdentifier());
		assertEquals(op, msg.getOperation());
		assertFalse(msg.getSnapshot());
		assertEquals(SenderSyncMessageStatus.NEW, msg.getStatus());
		assertNotNull(msg.getMessageUuid());
		assertNotNull(msg.getDateCreated());
		assertEquals(msg.getEventDate().getTime(), retry.getEventDate().getTime());
		assertNull(msg.getRequestUuid());
		assertNull(msg.getDateSent());
		assertEquals(PatientModel.class.getName(), JsonPath.read(msg.getData(), "tableToSyncModelClass"));
		assertEquals(uuid, JsonPath.read(msg.getData(), "model.uuid"));
		assertEquals(op, JsonPath.read(msg.getData(), "metadata.operation"));
		assertEquals(msg.getMessageUuid(), JsonPath.read(msg.getData(), "metadata.messageUuid"));
		assertFalse(JsonPath.read(msg.getData(), "metadata.snapshot"));
		assertNull(JsonPath.read(msg.getData(), "metadata.sourceIdentifier"));
		assertNull(JsonPath.read(msg.getData(), "metadata.dateSent"));
		assertNull(JsonPath.read(msg.getData(), "metadata.requestUuid"));
	}
	
	@Test
	public void shouldProcessAnEventAndDeleteItIfTheEntityIsNotFound() {
		assertTrue(TestUtils.getEntities(SenderSyncMessage.class).isEmpty());
		DefaultExchange exchange = new DefaultExchange(camelContext);
		final String table = "person";
		final String uuid = "person-uuid";
		final String op = "u";
		final boolean snapshot = true;
		DebeziumEvent debeziumEvent = createDebeziumEvent(table, "1", uuid, op);
		debeziumEvent.setDateCreated(new Date());
		eventRepo.save(debeziumEvent);
		debeziumEvent.getEvent().setSnapshot(snapshot);
		exchange.setProperty(EX_PROP_EVENT, debeziumEvent.getEvent());
		exchange.setProperty(EX_PROP_DBZM_EVENT, debeziumEvent);
		
		producerTemplate.send(URI_DBSYNC, exchange);
		
		assertFalse(eventRepo.existsById(debeziumEvent.getId()));
		assertTrue(TestUtils.getEntities(SenderSyncMessage.class).isEmpty());
		assertMessageLogged(Level.INFO,
		    "No entity found in the database matching identifier " + uuid + " in table " + table);
	}
	
	@Test
	public void shouldProcessARetryAndDeleteItIfTheEntityIsNotFound() {
		assertTrue(TestUtils.getEntities(SenderSyncMessage.class).isEmpty());
		DefaultExchange exchange = new DefaultExchange(camelContext);
		final String table = "person";
		final String uuid = "person-uuid";
		final String op = "u";
		SenderRetryQueueItem retry = new SenderRetryQueueItem();
		retry.setEvent(createEvent(table, "101", uuid, op));
		retry.setDateCreated(new Date());
		retry.setAttemptCount(1);
		retry.setExceptionType(EIPException.class.getName());
		retry.setEventDate(new Date());
		retryRepo.save(retry);
		assertEquals(1, getEntities(SenderRetryQueueItem.class).size());
		exchange.setProperty(EX_PROP_EVENT, retry.getEvent());
		exchange.setProperty(EX_PROP_RETRY_ITEM, retry);
		
		producerTemplate.send(URI_DBSYNC, exchange);
		
		assertFalse(retryRepo.existsById(retry.getId()));
		assertTrue(TestUtils.getEntities(SenderSyncMessage.class).isEmpty());
		assertMessageLogged(Level.INFO,
		    "No entity found in the database matching identifier " + uuid + " in table " + table);
	}
	
	@Test
	public void shouldProcessASyncRequestAndDeleteIt() {
		assertTrue(TestUtils.getEntities(SenderSyncMessage.class).isEmpty());
		DefaultExchange exchange = new DefaultExchange(camelContext);
		final String table = "patient";
		final String uuid = "abfd940e-32dc-491f-8038-a8f3afe3e35b";
		final String op = "r";
		DebeziumEvent debeziumEvent = createDebeziumEvent(table, "1", uuid, op);
		debeziumEvent.setDateCreated(new Date());
		eventRepo.save(debeziumEvent);
		exchange.setProperty(EX_PROP_EVENT, debeziumEvent.getEvent());
		exchange.setProperty(EX_PROP_DBZM_EVENT, debeziumEvent);
		
		producerTemplate.send(URI_DBSYNC, exchange);
		
		assertFalse(eventRepo.existsById(debeziumEvent.getId()));
		List<SenderSyncMessage> syncMsgs = TestUtils.getEntities(SenderSyncMessage.class);
		assertEquals(1, syncMsgs.size());
		SenderSyncMessage msg = syncMsgs.get(0);
		assertEquals(table, msg.getTableName());
		assertEquals(uuid, msg.getIdentifier());
		assertEquals(op, msg.getOperation());
		assertFalse(msg.getSnapshot());
		assertEquals(SenderSyncMessageStatus.NEW, msg.getStatus());
		assertNotNull(msg.getMessageUuid());
		assertNotNull(msg.getDateCreated());
		assertEquals(msg.getEventDate().getTime(), debeziumEvent.getDateCreated().getTime());
		assertNull(msg.getRequestUuid());
		assertNull(msg.getDateSent());
		assertEquals(PatientModel.class.getName(), JsonPath.read(msg.getData(), "tableToSyncModelClass"));
		assertEquals(uuid, JsonPath.read(msg.getData(), "model.uuid"));
		assertEquals(op, JsonPath.read(msg.getData(), "metadata.operation"));
		assertEquals(msg.getMessageUuid(), JsonPath.read(msg.getData(), "metadata.messageUuid"));
		assertFalse(JsonPath.read(msg.getData(), "metadata.snapshot"));
		assertNull(JsonPath.read(msg.getData(), "metadata.sourceIdentifier"));
		assertNull(JsonPath.read(msg.getData(), "metadata.dateSent"));
		assertNull(JsonPath.read(msg.getData(), "metadata.requestUuid"));
	}
	
	@Test
	public void shouldFailIfNoDebeziumEventOrRetryItemIsFoundOnTheExchange() {
		assertTrue(TestUtils.getEntities(SenderSyncMessage.class).isEmpty());
		DefaultExchange exchange = new DefaultExchange(camelContext);
		final String table = "patient";
		final String uuid = "abfd940e-32dc-491f-8038-a8f3afe3e35b";
		exchange.setProperty(EX_PROP_EVENT, createEvent(table, null, uuid, "u"));
		
		producerTemplate.send(URI_DBSYNC, exchange);
		
		assertEquals("No debezium event or retry item found on the exchange", getErrorMessage(exchange));
		assertTrue(TestUtils.getEntities(SenderSyncMessage.class).isEmpty());
	}
	
}
