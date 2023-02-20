package org.openmrs.eip.app.route.receiver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.openmrs.eip.app.SyncConstants.MGT_DATASOURCE_NAME;
import static org.openmrs.eip.app.SyncConstants.MGT_TX_MGR;
import static org.openmrs.eip.app.receiver.ReceiverConstants.EX_PROP_ENTITY_ID;
import static org.openmrs.eip.app.receiver.ReceiverConstants.EX_PROP_MODEL_CLASS;
import static org.openmrs.eip.app.receiver.ReceiverConstants.EX_PROP_MOVED_TO_CONFLICT_QUEUE;
import static org.openmrs.eip.app.receiver.ReceiverConstants.EX_PROP_MOVED_TO_ERROR_QUEUE;
import static org.openmrs.eip.app.receiver.ReceiverConstants.EX_PROP_MSG_PROCESSED;
import static org.openmrs.eip.app.receiver.ReceiverConstants.EX_PROP_PAYLOAD;
import static org.openmrs.eip.app.receiver.ReceiverConstants.EX_PROP_RETRY_ITEM;
import static org.openmrs.eip.app.receiver.ReceiverConstants.EX_PROP_SYNC_MESSAGE;
import static org.openmrs.eip.app.receiver.ReceiverConstants.ROUTE_ID_CLEAR_CACHE;
import static org.openmrs.eip.app.receiver.ReceiverConstants.ROUTE_ID_DBSYNC;
import static org.openmrs.eip.app.receiver.ReceiverConstants.ROUTE_ID_UPDATE_SEARCH_INDEX;
import static org.openmrs.eip.app.receiver.ReceiverConstants.URI_DBSYNC;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.support.DefaultExchange;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openmrs.eip.app.management.entity.ConflictQueueItem;
import org.openmrs.eip.app.management.entity.ReceiverRetryQueueItem;
import org.openmrs.eip.app.management.entity.SiteInfo;
import org.openmrs.eip.app.management.entity.SyncMessage;
import org.openmrs.eip.app.route.TestUtils;
import org.openmrs.eip.component.SyncOperation;
import org.openmrs.eip.component.entity.light.PatientLight;
import org.openmrs.eip.component.entity.light.VisitTypeLight;
import org.openmrs.eip.component.exception.ConflictsFoundException;
import org.openmrs.eip.component.model.BaseModel;
import org.openmrs.eip.component.model.PatientIdentifierModel;
import org.openmrs.eip.component.model.PatientModel;
import org.openmrs.eip.component.model.PersonAddressModel;
import org.openmrs.eip.component.model.PersonAttributeModel;
import org.openmrs.eip.component.model.PersonModel;
import org.openmrs.eip.component.model.PersonNameModel;
import org.openmrs.eip.component.model.SyncMetadata;
import org.openmrs.eip.component.model.SyncModel;
import org.openmrs.eip.component.model.UserModel;
import org.openmrs.eip.component.model.VisitModel;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;

@Sql({ "classpath:openmrs_core_data.sql", "classpath:openmrs_patient.sql" })
@Sql(scripts = "classpath:mgt_site_info.sql", config = @SqlConfig(dataSource = MGT_DATASOURCE_NAME, transactionManager = MGT_TX_MGR))
@TestPropertySource(properties = "logging.level." + ROUTE_ID_DBSYNC + "=DEBUG")
public class DbSyncRouteTest extends BaseReceiverRouteTest {
	
	protected static final String ROUTE_ID_DESTINATION = "msg-processor";
	
	@EndpointInject("mock:" + ROUTE_ID_DESTINATION)
	private MockEndpoint mockLoadEndpoint;
	
	@EndpointInject("mock:" + ROUTE_ID_UPDATE_SEARCH_INDEX)
	private MockEndpoint mockUpdateSearchIndexEndpoint;
	
	@EndpointInject("mock:" + ROUTE_ID_CLEAR_CACHE)
	private MockEndpoint mockClearCacheEndpoint;
	
	@Override
	public String getTestRouteFilename() {
		return "db-sync-route";
	}
	
	@Before
	public void setup() throws Exception {
		mockLoadEndpoint.reset();
		mockUpdateSearchIndexEndpoint.reset();
		mockClearCacheEndpoint.reset();
		
		advise(ROUTE_ID_DBSYNC, new AdviceWithRouteBuilder() {
			
			@Override
			public void configure() {
				weaveByToUri("openmrs:load").replace().to(mockLoadEndpoint);
			}
			
		});
	}
	
	@Test
	public void shouldCallTheLoadProducer() throws Exception {
		final Class<? extends BaseModel> modelClass = VisitModel.class;
		final String uuid = "visit-uuid";
		VisitModel model = new VisitModel();
		model.setUuid(uuid);
		model.setPatientUuid(PatientLight.class.getName() + "(some-patient-uuid)");
		model.setVisitTypeUuid(VisitTypeLight.class.getName() + "(some-visit-type-uuid)");
		SyncModel syncModel = new SyncModel();
		syncModel.setTableToSyncModelClass(modelClass);
		syncModel.setModel(model);
		syncModel.setMetadata(new SyncMetadata());
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_MODEL_CLASS, modelClass.getName());
		exchange.setProperty(EX_PROP_ENTITY_ID, uuid);
		exchange.getIn().setBody(syncModel);
		mockLoadEndpoint.expectedBodiesReceived(syncModel);
		mockClearCacheEndpoint.expectedMessageCount(0);
		mockUpdateSearchIndexEndpoint.expectedMessageCount(0);
		
		producerTemplate.send(URI_DBSYNC, exchange);
		
		mockLoadEndpoint.assertIsSatisfied();
		mockClearCacheEndpoint.assertIsSatisfied();
		mockUpdateSearchIndexEndpoint.assertIsSatisfied();
		assertTrue(exchange.getProperty(EX_PROP_MSG_PROCESSED, Boolean.class));
		assertNull(exchange.getProperty(EX_PROP_MOVED_TO_CONFLICT_QUEUE));
		assertNull(exchange.getProperty(EX_PROP_MOVED_TO_ERROR_QUEUE));
	}
	
	@Test
	@Sql(scripts = { "classpath:mgt_site_info.sql",
	        "classpath:mgt_receiver_conflict_queue.sql" }, config = @SqlConfig(dataSource = MGT_DATASOURCE_NAME, transactionManager = MGT_TX_MGR))
	public void shouldFailIfTheEntityHasItemsInTheConflictQueue() throws Exception {
		final Class<? extends BaseModel> modelClass = PersonModel.class;
		final String uuid = "uuid-1";
		assertTrue(ReceiverTestUtils.hasConflict(modelClass, uuid));
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_MODEL_CLASS, modelClass.getName());
		exchange.setProperty(EX_PROP_ENTITY_ID, uuid);
		mockLoadEndpoint.expectedMessageCount(0);
		mockUpdateSearchIndexEndpoint.expectedMessageCount(0);
		mockClearCacheEndpoint.expectedMessageCount(0);
		
		producerTemplate.send(URI_DBSYNC, exchange);
		
		assertEquals("Cannot process the message because the entity has 3 message(s) in the DB sync conflict queue",
		    getErrorMessage(exchange));
		mockLoadEndpoint.assertIsSatisfied();
		mockUpdateSearchIndexEndpoint.assertIsSatisfied();
		mockClearCacheEndpoint.assertIsSatisfied();
		assertNull(exchange.getProperty(EX_PROP_MSG_PROCESSED));
		assertNull(exchange.getProperty(EX_PROP_MOVED_TO_CONFLICT_QUEUE));
		assertNull(exchange.getProperty(EX_PROP_MOVED_TO_ERROR_QUEUE));
	}
	
	@Test
	@Sql(scripts = { "classpath:mgt_site_info.sql",
	        "classpath:mgt_receiver_conflict_queue.sql" }, config = @SqlConfig(dataSource = MGT_DATASOURCE_NAME, transactionManager = MGT_TX_MGR))
	public void shouldPassIfTheEntityHasResolvedItemsInTheConflictQueue() throws Exception {
		final Class<? extends BaseModel> modelClass = PersonModel.class;
		final String uuid = "uuid-2";
		ConflictQueueItem conflict = TestUtils.getEntity(ConflictQueueItem.class, 4L);
		assertEquals(modelClass.getName(), conflict.getModelClassName());
		assertEquals(uuid, conflict.getIdentifier());
		assertTrue(conflict.getResolved());
		PersonModel model = new PersonModel();
		model.setUuid(uuid);
		SyncModel syncModel = new SyncModel();
		syncModel.setTableToSyncModelClass(modelClass);
		syncModel.setModel(model);
		syncModel.setMetadata(new SyncMetadata());
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_MODEL_CLASS, modelClass.getName());
		exchange.setProperty(EX_PROP_ENTITY_ID, uuid);
		exchange.getIn().setBody(syncModel);
		mockLoadEndpoint.expectedBodiesReceived(syncModel);
		mockClearCacheEndpoint.expectedBodiesReceived("{\"resource\": \"person\", \"uuid\": \"" + uuid + "\"}");
		mockUpdateSearchIndexEndpoint.expectedMessageCount(0);
		
		producerTemplate.send(URI_DBSYNC, exchange);
		
		mockLoadEndpoint.assertIsSatisfied();
	}
	
	@Test
	@Ignore
	public void shouldLoadPersonEntityAndClearDbCacheAndUpdateTheSearchIndex() throws Exception {
		final Class<? extends BaseModel> modelClass = PersonModel.class;
		final String uuid = "abfd940e-32dc-491f-8038-a8f3afe3e35b";
		PersonModel model = new PersonModel();
		model.setUuid(uuid);
		SyncModel syncModel = new SyncModel();
		syncModel.setTableToSyncModelClass(modelClass);
		syncModel.setModel(model);
		syncModel.setMetadata(new SyncMetadata());
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_MODEL_CLASS, modelClass.getName());
		exchange.setProperty(EX_PROP_ENTITY_ID, uuid);
		exchange.getIn().setBody(syncModel);
		mockLoadEndpoint.expectedBodiesReceived(syncModel);
		mockClearCacheEndpoint.expectedBodiesReceived("{\"resource\": \"person\", \"uuid\": \"" + uuid + "\"}");
		List<String> bodies = new ArrayList();
		mockUpdateSearchIndexEndpoint.whenAnyExchangeReceived(e -> bodies.add(e.getIn().getBody(String.class)));
		
		producerTemplate.send(URI_DBSYNC, exchange);
		
		assertEquals(4, bodies.size());
		assertTrue(bodies.contains(
		    "{\"resource\": \"person\", \"subResource\": \"name\", \"uuid\": \"1bfd940e-32dc-491f-8038-a8f3afe3e35a\"}"));
		assertTrue(bodies.contains(
		    "{\"resource\": \"person\", \"subResource\": \"name\", \"uuid\": \"2bfd940e-32dc-491f-8038-a8f3afe3e35a\"}"));
		assertTrue(bodies.contains(
		    "{\"resource\": \"patient\", \"subResource\": \"identifier\", \"uuid\": \"1cfd940e-32dc-491f-8038-a8f3afe3e35c\"}"));
		assertTrue(bodies.contains(
		    "{\"resource\": \"patient\", \"subResource\": \"identifier\", \"uuid\": \"2cfd940e-32dc-491f-8038-a8f3afe3e35c\"}"));
		mockLoadEndpoint.assertIsSatisfied();
		mockClearCacheEndpoint.assertIsSatisfied();
		mockUpdateSearchIndexEndpoint.assertIsSatisfied();
	}
	
	@Test
	@Ignore
	public void shouldLoadPersonEntityAndUpdateTheSearchIndexForADeleteMessage() throws Exception {
		final Class<? extends BaseModel> modelClass = PersonModel.class;
		final String uuid = "abfd940e-32dc-491f-8038-a8f3afe3e35b";
		PersonModel model = new PersonModel();
		model.setUuid(uuid);
		SyncModel syncModel = new SyncModel();
		syncModel.setTableToSyncModelClass(modelClass);
		syncModel.setModel(model);
		SyncMetadata metadata = new SyncMetadata();
		metadata.setOperation("d");
		syncModel.setMetadata(metadata);
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_MODEL_CLASS, modelClass.getName());
		exchange.setProperty(EX_PROP_ENTITY_ID, uuid);
		exchange.getIn().setBody(syncModel);
		mockLoadEndpoint.expectedBodiesReceived(syncModel);
		mockClearCacheEndpoint.expectedBodiesReceived("{\"resource\": \"person\"}");
		List<String> bodies = new ArrayList();
		mockUpdateSearchIndexEndpoint.whenAnyExchangeReceived(e -> bodies.add(e.getIn().getBody(String.class)));
		
		producerTemplate.send(URI_DBSYNC, exchange);
		
		assertEquals(4, bodies.size());
		assertTrue(bodies.contains(
		    "{\"resource\": \"person\", \"subResource\": \"name\", \"uuid\": \"1bfd940e-32dc-491f-8038-a8f3afe3e35a\"}"));
		assertTrue(bodies.contains(
		    "{\"resource\": \"person\", \"subResource\": \"name\", \"uuid\": \"2bfd940e-32dc-491f-8038-a8f3afe3e35a\"}"));
		assertTrue(bodies.contains(
		    "{\"resource\": \"patient\", \"subResource\": \"identifier\", \"uuid\": \"1cfd940e-32dc-491f-8038-a8f3afe3e35c\"}"));
		assertTrue(bodies.contains(
		    "{\"resource\": \"patient\", \"subResource\": \"identifier\", \"uuid\": \"2cfd940e-32dc-491f-8038-a8f3afe3e35c\"}"));
		mockLoadEndpoint.assertIsSatisfied();
		mockClearCacheEndpoint.assertIsSatisfied();
		mockUpdateSearchIndexEndpoint.assertIsSatisfied();
	}
	
	@Test
	@Ignore
	public void shouldLoadPatientEntityAndClearDbCacheAndUpdateTheSearchIndex() throws Exception {
		final Class<? extends BaseModel> modelClass = PatientModel.class;
		final String uuid = "abfd940e-32dc-491f-8038-a8f3afe3e35b";
		PatientModel model = new PatientModel();
		model.setUuid(uuid);
		SyncModel syncModel = new SyncModel();
		syncModel.setTableToSyncModelClass(modelClass);
		syncModel.setModel(model);
		syncModel.setMetadata(new SyncMetadata());
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_MODEL_CLASS, modelClass.getName());
		exchange.setProperty(EX_PROP_ENTITY_ID, uuid);
		exchange.getIn().setBody(syncModel);
		mockLoadEndpoint.expectedBodiesReceived(syncModel);
		mockClearCacheEndpoint.expectedBodiesReceived("{\"resource\": \"person\", \"uuid\": \"" + uuid + "\"}");
		mockUpdateSearchIndexEndpoint.expectedMessageCount(4);
		List<String> bodies = new ArrayList();
		mockUpdateSearchIndexEndpoint.whenAnyExchangeReceived(e -> bodies.add(e.getIn().getBody(String.class)));
		
		producerTemplate.send(URI_DBSYNC, exchange);
		
		assertEquals(4, bodies.size());
		assertTrue(bodies.contains(
		    "{\"resource\": \"person\", \"subResource\": \"name\", \"uuid\": \"1bfd940e-32dc-491f-8038-a8f3afe3e35a\"}"));
		assertTrue(bodies.contains(
		    "{\"resource\": \"person\", \"subResource\": \"name\", \"uuid\": \"2bfd940e-32dc-491f-8038-a8f3afe3e35a\"}"));
		assertTrue(bodies.contains(
		    "{\"resource\": \"patient\", \"subResource\": \"identifier\", \"uuid\": \"1cfd940e-32dc-491f-8038-a8f3afe3e35c\"}"));
		assertTrue(bodies.contains(
		    "{\"resource\": \"patient\", \"subResource\": \"identifier\", \"uuid\": \"2cfd940e-32dc-491f-8038-a8f3afe3e35c\"}"));
		mockLoadEndpoint.assertIsSatisfied();
		mockClearCacheEndpoint.assertIsSatisfied();
		mockUpdateSearchIndexEndpoint.assertIsSatisfied();
	}
	
	@Test
	@Ignore
	public void shouldLoadPatientEntityAndClearDbCacheAndUpdateTheSearchIndexForADeleteMessage() throws Exception {
		final Class<? extends BaseModel> modelClass = PatientModel.class;
		final String uuid = "abfd940e-32dc-491f-8038-a8f3afe3e35b";
		PatientModel model = new PatientModel();
		model.setUuid(uuid);
		SyncModel syncModel = new SyncModel();
		syncModel.setTableToSyncModelClass(modelClass);
		syncModel.setModel(model);
		SyncMetadata metadata = new SyncMetadata();
		metadata.setOperation("d");
		syncModel.setMetadata(metadata);
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_MODEL_CLASS, modelClass.getName());
		exchange.setProperty(EX_PROP_ENTITY_ID, uuid);
		exchange.getIn().setBody(syncModel);
		mockLoadEndpoint.expectedBodiesReceived(syncModel);
		mockClearCacheEndpoint.expectedBodiesReceived("{\"resource\": \"person\"}");
		mockUpdateSearchIndexEndpoint.expectedMessageCount(4);
		List<String> bodies = new ArrayList();
		mockUpdateSearchIndexEndpoint.whenAnyExchangeReceived(e -> bodies.add(e.getIn().getBody(String.class)));
		
		producerTemplate.send(URI_DBSYNC, exchange);
		
		assertEquals(4, bodies.size());
		assertTrue(bodies.contains(
		    "{\"resource\": \"person\", \"subResource\": \"name\", \"uuid\": \"1bfd940e-32dc-491f-8038-a8f3afe3e35a\"}"));
		assertTrue(bodies.contains(
		    "{\"resource\": \"person\", \"subResource\": \"name\", \"uuid\": \"2bfd940e-32dc-491f-8038-a8f3afe3e35a\"}"));
		assertTrue(bodies.contains(
		    "{\"resource\": \"patient\", \"subResource\": \"identifier\", \"uuid\": \"1cfd940e-32dc-491f-8038-a8f3afe3e35c\"}"));
		assertTrue(bodies.contains(
		    "{\"resource\": \"patient\", \"subResource\": \"identifier\", \"uuid\": \"2cfd940e-32dc-491f-8038-a8f3afe3e35c\"}"));
		mockLoadEndpoint.assertIsSatisfied();
		mockClearCacheEndpoint.assertIsSatisfied();
		mockUpdateSearchIndexEndpoint.assertIsSatisfied();
	}
	
	@Test
	@Ignore
	public void shouldLoadAPersonNameAndClearDbCacheAndUpdateTheSearchIndex() throws Exception {
		final Class<? extends BaseModel> modelClass = PersonNameModel.class;
		final String uuid = "name-uuid";
		PersonNameModel model = new PersonNameModel();
		model.setUuid(uuid);
		SyncModel syncModel = new SyncModel();
		syncModel.setTableToSyncModelClass(modelClass);
		syncModel.setModel(model);
		syncModel.setMetadata(new SyncMetadata());
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_MODEL_CLASS, modelClass.getName());
		exchange.setProperty(EX_PROP_ENTITY_ID, uuid);
		exchange.getIn().setBody(syncModel);
		mockLoadEndpoint.expectedBodiesReceived(syncModel);
		mockClearCacheEndpoint
		        .expectedBodiesReceived("{\"resource\": \"person\", \"subResource\": \"name\", \"uuid\": \"" + uuid + "\"}");
		mockUpdateSearchIndexEndpoint
		        .expectedBodiesReceived("{\"resource\": \"person\", \"subResource\": \"name\", \"uuid\": \"" + uuid + "\"}");
		
		producerTemplate.send(URI_DBSYNC, exchange);
		
		mockLoadEndpoint.assertIsSatisfied();
		mockClearCacheEndpoint.assertIsSatisfied();
		mockUpdateSearchIndexEndpoint.assertIsSatisfied();
	}
	
	@Test
	@Ignore
	public void shouldLoadAPersonNameAndClearDbCacheAndUpdateTheSearchIndexForADeleteMessage() throws Exception {
		final Class<? extends BaseModel> modelClass = PersonNameModel.class;
		final String uuid = "name-uuid";
		PersonNameModel model = new PersonNameModel();
		model.setUuid(uuid);
		SyncModel syncModel = new SyncModel();
		syncModel.setTableToSyncModelClass(modelClass);
		syncModel.setModel(model);
		SyncMetadata metadata = new SyncMetadata();
		metadata.setOperation("d");
		syncModel.setMetadata(metadata);
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_MODEL_CLASS, modelClass.getName());
		exchange.setProperty(EX_PROP_ENTITY_ID, uuid);
		exchange.getIn().setBody(syncModel);
		mockLoadEndpoint.expectedBodiesReceived(syncModel);
		mockClearCacheEndpoint.expectedBodiesReceived("{\"resource\": \"person\", \"subResource\": \"name\"}");
		mockUpdateSearchIndexEndpoint.expectedBodiesReceived("{\"resource\": \"person\", \"subResource\": \"name\"}");
		
		producerTemplate.send(URI_DBSYNC, exchange);
		
		mockLoadEndpoint.assertIsSatisfied();
		mockClearCacheEndpoint.assertIsSatisfied();
		mockUpdateSearchIndexEndpoint.assertIsSatisfied();
	}
	
	@Test
	@Ignore
	public void shouldLoadAPersonAttributeAndClearDbCacheAndUpdateTheSearchIndex() throws Exception {
		final Class<? extends BaseModel> modelClass = PersonAttributeModel.class;
		final String uuid = "attrib-uuid";
		PersonAttributeModel model = new PersonAttributeModel();
		model.setUuid(uuid);
		SyncModel syncModel = new SyncModel();
		syncModel.setTableToSyncModelClass(modelClass);
		syncModel.setModel(model);
		syncModel.setMetadata(new SyncMetadata());
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_MODEL_CLASS, modelClass.getName());
		exchange.setProperty(EX_PROP_ENTITY_ID, uuid);
		exchange.getIn().setBody(syncModel);
		mockLoadEndpoint.expectedBodiesReceived(syncModel);
		mockClearCacheEndpoint.expectedBodiesReceived(
		    "{\"resource\": \"person\", \"subResource\": \"attribute\", \"uuid\": \"" + uuid + "\"}");
		mockUpdateSearchIndexEndpoint.expectedBodiesReceived(
		    "{\"resource\": \"person\", \"subResource\": \"attribute\", \"uuid\": \"" + uuid + "\"}");
		
		producerTemplate.send(URI_DBSYNC, exchange);
		
		mockLoadEndpoint.assertIsSatisfied();
		mockClearCacheEndpoint.assertIsSatisfied();
		mockUpdateSearchIndexEndpoint.assertIsSatisfied();
	}
	
	@Test
	@Ignore
	public void shouldLoadAPersonAttributeAndClearDbCacheAndUpdateTheSearchIndexForADeleteMessage() throws Exception {
		final Class<? extends BaseModel> modelClass = PersonAttributeModel.class;
		final String uuid = "attrib-uuid";
		PersonAttributeModel model = new PersonAttributeModel();
		model.setUuid(uuid);
		SyncModel syncModel = new SyncModel();
		syncModel.setTableToSyncModelClass(modelClass);
		syncModel.setModel(model);
		SyncMetadata metadata = new SyncMetadata();
		metadata.setOperation("d");
		syncModel.setMetadata(metadata);
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_MODEL_CLASS, modelClass.getName());
		exchange.setProperty(EX_PROP_ENTITY_ID, uuid);
		exchange.getIn().setBody(syncModel);
		mockLoadEndpoint.expectedBodiesReceived(syncModel);
		mockClearCacheEndpoint.expectedBodiesReceived("{\"resource\": \"person\", \"subResource\": \"attribute\"}");
		mockUpdateSearchIndexEndpoint.expectedBodiesReceived("{\"resource\": \"person\", \"subResource\": \"attribute\"}");
		
		producerTemplate.send(URI_DBSYNC, exchange);
		
		mockLoadEndpoint.assertIsSatisfied();
		mockClearCacheEndpoint.assertIsSatisfied();
		mockUpdateSearchIndexEndpoint.assertIsSatisfied();
	}
	
	@Test
	@Ignore
	public void shouldLoadAPatientIdentifierAndUpdateTheSearchIndex() throws Exception {
		final Class<? extends BaseModel> modelClass = PatientIdentifierModel.class;
		final String uuid = "id-uuid";
		PatientIdentifierModel model = new PatientIdentifierModel();
		model.setUuid(uuid);
		SyncModel syncModel = new SyncModel();
		syncModel.setTableToSyncModelClass(modelClass);
		syncModel.setModel(model);
		syncModel.setMetadata(new SyncMetadata());
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_MODEL_CLASS, modelClass.getName());
		exchange.setProperty(EX_PROP_ENTITY_ID, uuid);
		exchange.getIn().setBody(syncModel);
		mockLoadEndpoint.expectedBodiesReceived(syncModel);
		mockClearCacheEndpoint.expectedMessageCount(0);
		mockUpdateSearchIndexEndpoint.expectedBodiesReceived(
		    "{\"resource\": \"patient\", \"subResource\": \"identifier\", \"uuid\": \"" + uuid + "\"}");
		
		producerTemplate.send(URI_DBSYNC, exchange);
		
		mockLoadEndpoint.assertIsSatisfied();
		mockClearCacheEndpoint.assertIsSatisfied();
		mockUpdateSearchIndexEndpoint.assertIsSatisfied();
	}
	
	@Test
	@Ignore
	public void shouldLoadAPatientIdentifierAndUpdateTheSearchIndexForADeleteMessage() throws Exception {
		final Class<? extends BaseModel> modelClass = PatientIdentifierModel.class;
		final String uuid = "id-uuid";
		PatientIdentifierModel model = new PatientIdentifierModel();
		model.setUuid(uuid);
		SyncModel syncModel = new SyncModel();
		syncModel.setTableToSyncModelClass(modelClass);
		syncModel.setModel(model);
		SyncMetadata metadata = new SyncMetadata();
		metadata.setOperation("d");
		syncModel.setMetadata(metadata);
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_MODEL_CLASS, modelClass.getName());
		exchange.setProperty(EX_PROP_ENTITY_ID, uuid);
		exchange.getIn().setBody(syncModel);
		mockLoadEndpoint.expectedBodiesReceived(syncModel);
		mockClearCacheEndpoint.expectedMessageCount(0);
		mockUpdateSearchIndexEndpoint.expectedBodiesReceived("{\"resource\": \"patient\", \"subResource\": \"identifier\"}");
		
		producerTemplate.send(URI_DBSYNC, exchange);
		
		mockLoadEndpoint.assertIsSatisfied();
		mockClearCacheEndpoint.assertIsSatisfied();
		mockUpdateSearchIndexEndpoint.assertIsSatisfied();
	}
	
	@Test
	@Ignore
	public void shouldLoadAPersonAddressAndUpdateTheSearchIndex() throws Exception {
		final Class<? extends BaseModel> modelClass = PersonAddressModel.class;
		final String uuid = "address-uuid";
		PersonAddressModel model = new PersonAddressModel();
		model.setUuid(uuid);
		SyncModel syncModel = new SyncModel();
		syncModel.setTableToSyncModelClass(modelClass);
		syncModel.setModel(model);
		syncModel.setMetadata(new SyncMetadata());
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_MODEL_CLASS, modelClass.getName());
		exchange.setProperty(EX_PROP_ENTITY_ID, uuid);
		exchange.getIn().setBody(syncModel);
		mockLoadEndpoint.expectedBodiesReceived(syncModel);
		mockUpdateSearchIndexEndpoint.expectedMessageCount(0);
		mockClearCacheEndpoint.expectedBodiesReceived(
		    "{\"resource\": \"person\", \"subResource\": \"address\", \"uuid\": \"" + uuid + "\"}");
		
		producerTemplate.send(URI_DBSYNC, exchange);
		
		mockLoadEndpoint.assertIsSatisfied();
		mockClearCacheEndpoint.assertIsSatisfied();
		mockUpdateSearchIndexEndpoint.assertIsSatisfied();
	}
	
	@Test
	@Ignore
	public void shouldLoadAPersonAddressAndUpdateTheSearchIndexForADeleteMessage() throws Exception {
		final Class<? extends BaseModel> modelClass = PersonAddressModel.class;
		final String uuid = "address-uuid";
		PersonAddressModel model = new PersonAddressModel();
		model.setUuid(uuid);
		SyncModel syncModel = new SyncModel();
		syncModel.setTableToSyncModelClass(modelClass);
		syncModel.setModel(model);
		SyncMetadata metadata = new SyncMetadata();
		metadata.setOperation("d");
		syncModel.setMetadata(metadata);
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_MODEL_CLASS, modelClass.getName());
		exchange.setProperty(EX_PROP_ENTITY_ID, uuid);
		exchange.getIn().setBody(syncModel);
		mockLoadEndpoint.expectedBodiesReceived(syncModel);
		mockUpdateSearchIndexEndpoint.expectedMessageCount(0);
		mockClearCacheEndpoint.expectedBodiesReceived("{\"resource\": \"person\", \"subResource\": \"address\"}");
		
		producerTemplate.send(URI_DBSYNC, exchange);
		
		mockLoadEndpoint.assertIsSatisfied();
		mockClearCacheEndpoint.assertIsSatisfied();
		mockUpdateSearchIndexEndpoint.assertIsSatisfied();
	}
	
	@Test
	@Ignore
	public void shouldLoadAUserAndClearDbCache() throws Exception {
		final Class<? extends BaseModel> modelClass = UserModel.class;
		final String uuid = "user-uuid";
		UserModel model = new UserModel();
		model.setUuid(uuid);
		SyncModel syncModel = new SyncModel();
		syncModel.setTableToSyncModelClass(modelClass);
		syncModel.setModel(model);
		syncModel.setMetadata(new SyncMetadata());
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_MODEL_CLASS, modelClass.getName());
		exchange.setProperty(EX_PROP_ENTITY_ID, uuid);
		exchange.getIn().setBody(syncModel);
		mockLoadEndpoint.expectedBodiesReceived(syncModel);
		mockUpdateSearchIndexEndpoint.expectedMessageCount(0);
		mockClearCacheEndpoint.expectedBodiesReceived("{\"resource\": \"user\", \"uuid\": \"" + uuid + "\"}");
		
		producerTemplate.send(URI_DBSYNC, exchange);
		
		mockLoadEndpoint.assertIsSatisfied();
		mockClearCacheEndpoint.assertIsSatisfied();
		mockUpdateSearchIndexEndpoint.assertIsSatisfied();
	}
	
	@Test
	public void shouldAddTheMessageToTheConflictQueueIfAConflictIsDetected() throws Exception {
		assertTrue(TestUtils.getEntities(ConflictQueueItem.class).isEmpty());
		final Class<? extends BaseModel> modelClass = UserModel.class;
		final String uuid = "user-uuid";
		final String payLoad = "{}";
		UserModel model = new UserModel();
		model.setUuid(uuid);
		SyncModel syncModel = new SyncModel();
		syncModel.setTableToSyncModelClass(modelClass);
		syncModel.setModel(model);
		syncModel.setMetadata(new SyncMetadata());
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_MODEL_CLASS, modelClass.getName());
		exchange.setProperty(EX_PROP_ENTITY_ID, uuid);
		exchange.setProperty(EX_PROP_PAYLOAD, payLoad);
		SyncMessage syncMessage = new SyncMessage();
		syncMessage.setOperation(SyncOperation.c);
		syncMessage.setSnapshot(true);
		syncMessage.setMessageUuid("message-uuid");
		syncMessage.setDateCreated(new Date());
		syncMessage.setSite(TestUtils.getEntity(SiteInfo.class, 1L));
		syncMessage.setDateSentBySender(LocalDateTime.now());
		exchange.setProperty(EX_PROP_SYNC_MESSAGE, syncMessage);
		exchange.getIn().setBody(syncModel);
		mockLoadEndpoint.expectedBodiesReceived(syncModel);
		mockUpdateSearchIndexEndpoint.expectedMessageCount(0);
		mockClearCacheEndpoint.expectedMessageCount(0);
		mockLoadEndpoint.whenAnyExchangeReceived(e -> {
			throw new ConflictsFoundException();
		});
		
		producerTemplate.send(URI_DBSYNC, exchange);
		
		mockLoadEndpoint.assertIsSatisfied();
		mockClearCacheEndpoint.assertIsSatisfied();
		mockUpdateSearchIndexEndpoint.assertIsSatisfied();
		List<ConflictQueueItem> conflicts = TestUtils.getEntities(ConflictQueueItem.class);
		assertEquals(1, conflicts.size());
		ConflictQueueItem conflict = conflicts.get(0);
		assertEquals(modelClass.getName(), conflict.getModelClassName());
		assertEquals(uuid, conflict.getIdentifier());
		assertEquals(syncMessage.getOperation(), conflict.getOperation());
		assertEquals(payLoad, conflict.getEntityPayload());
		assertEquals(syncMessage.getSite(), conflict.getSite());
		assertEquals(syncMessage.getDateSentBySender(), conflict.getDateSentBySender());
		assertEquals(syncMessage.getMessageUuid(), conflict.getMessageUuid());
		assertEquals(syncMessage.getSnapshot(), conflict.getSnapshot());
		assertEquals(syncMessage.getDateCreated(), conflict.getDateReceived());
		assertFalse(conflict.getResolved());
		assertNotNull(conflict.getDateCreated());
		assertTrue(exchange.getProperty(EX_PROP_MOVED_TO_CONFLICT_QUEUE, Boolean.class));
		assertNull(exchange.getProperty(EX_PROP_MSG_PROCESSED));
	}
	
	@Test
	public void shouldAddTheMessageToTheConflictQueueIfAConflictIsDetectedForARetryItem() throws Exception {
		assertTrue(TestUtils.getEntities(ConflictQueueItem.class).isEmpty());
		final Class<? extends BaseModel> modelClass = UserModel.class;
		final String uuid = "user-uuid";
		final String payLoad = "{}";
		UserModel model = new UserModel();
		model.setUuid(uuid);
		SyncModel syncModel = new SyncModel();
		syncModel.setTableToSyncModelClass(modelClass);
		syncModel.setModel(model);
		syncModel.setMetadata(new SyncMetadata());
		ReceiverRetryQueueItem retry = new ReceiverRetryQueueItem();
		retry.setModelClassName(modelClass.getName());
		retry.setIdentifier(uuid);
		retry.setOperation(SyncOperation.u);
		retry.setEntityPayload(payLoad);
		retry.setSnapshot(true);
		retry.setSite(TestUtils.getEntity(SiteInfo.class, 1L));
		retry.setDateSentBySender(LocalDateTime.now());
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_MODEL_CLASS, modelClass.getName());
		exchange.setProperty(EX_PROP_ENTITY_ID, uuid);
		exchange.setProperty(EX_PROP_RETRY_ITEM, retry);
		exchange.getIn().setBody(syncModel);
		mockLoadEndpoint.expectedBodiesReceived(syncModel);
		mockUpdateSearchIndexEndpoint.expectedMessageCount(0);
		mockClearCacheEndpoint.expectedMessageCount(0);
		mockLoadEndpoint.whenAnyExchangeReceived(e -> {
			throw new ConflictsFoundException();
		});
		
		producerTemplate.send(URI_DBSYNC, exchange);
		
		mockLoadEndpoint.assertIsSatisfied();
		mockClearCacheEndpoint.assertIsSatisfied();
		mockUpdateSearchIndexEndpoint.assertIsSatisfied();
		List<ConflictQueueItem> conflicts = TestUtils.getEntities(ConflictQueueItem.class);
		assertEquals(1, conflicts.size());
		ConflictQueueItem conflict = conflicts.get(0);
		assertEquals(modelClass.getName(), conflict.getModelClassName());
		assertEquals(uuid, conflict.getIdentifier());
		assertEquals(retry.getOperation(), conflict.getOperation());
		assertEquals(payLoad, conflict.getEntityPayload());
		assertEquals(retry.getDateSentBySender(), conflict.getDateSentBySender());
		assertEquals(retry.getMessageUuid(), conflict.getMessageUuid());
		assertEquals(retry.getSnapshot(), conflict.getSnapshot());
		assertEquals(retry.getDateReceived(), conflict.getDateReceived());
		assertEquals(retry.getSite(), conflict.getSite());
		assertFalse(conflict.getResolved());
		assertNotNull(conflict.getDateCreated());
		assertTrue(exchange.getProperty(EX_PROP_MOVED_TO_CONFLICT_QUEUE, Boolean.class));
		assertNull(exchange.getProperty(EX_PROP_MSG_PROCESSED));
	}
	
}
