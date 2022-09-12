package org.openmrs.eip.app.route.sender;

import static org.openmrs.eip.app.route.sender.BaseSenderRouteTest.SENDER_ID;
import static org.openmrs.eip.app.route.sender.BaseSenderRouteTest.URI_ACTIVEMQ_SYNC;
import static org.openmrs.eip.app.sender.SenderConstants.PROP_CAMEL_OUTPUT_ENDPOINT;
import static org.openmrs.eip.app.sender.SenderConstants.PROP_SENDER_ID;
import static org.openmrs.eip.app.sender.SenderConstants.ROUTE_ID_ACTIVEMQ_PUBLISHER;

import java.util.Date;

import org.apache.camel.EndpointInject;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.Before;
import org.openmrs.eip.app.management.entity.DebeziumEvent;
import org.openmrs.eip.app.management.entity.JMSBroker;
import org.openmrs.eip.app.management.repository.JMSBrokerRepository;
import org.openmrs.eip.app.route.BaseRouteTest;
import org.openmrs.eip.component.SyncProfiles;
import org.openmrs.eip.component.entity.Event;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@ActiveProfiles(SyncProfiles.SENDER)
@TestPropertySource(properties = PROP_SENDER_ID + "=" + SENDER_ID)
@TestPropertySource(properties = PROP_CAMEL_OUTPUT_ENDPOINT + "=" + URI_ACTIVEMQ_SYNC)
@TestPropertySource(properties = "logging.level." + ROUTE_ID_ACTIVEMQ_PUBLISHER + "=DEBUG")
public abstract class BaseSenderRouteTest extends BaseRouteTest {
	
	public static final String SENDER_ID = "test-sender-id";
	
	public static final String URI_ACTIVEMQ_SYNC = "mock:activemq.openmrs.sync";
	
	@EndpointInject(URI_ACTIVEMQ_SYNC)
	protected MockEndpoint mockActiveMqEndpoint;
	
	@Autowired
	protected JMSBrokerRepository jmsBrokerRepository;
	
	@Before
	public void genericSetup() {
		mockActiveMqEndpoint.reset();
	}
	
	protected Event createEvent(String table, String pkId, String identifier, String op) {
		Event event = new Event();
		event.setTableName(table);
		event.setPrimaryKeyId(pkId);
		event.setIdentifier(identifier);
		event.setOperation(op);
		event.setSnapshot(false);
		return event;
	}
	
	protected DebeziumEvent createDebeziumEvent(String table, String pkId, String uuid, String op) {
		DebeziumEvent dbzmEvent = new DebeziumEvent();
		dbzmEvent.setEvent(createEvent(table, pkId, uuid, op));
		return dbzmEvent;
	}
	
	protected JMSBroker createJMSBroker(String identifier, String host, Integer port, String username, String password) {
		JMSBroker receiver = new JMSBroker();
		receiver.setIdentifier(identifier);
		receiver.setName(identifier);
		receiver.setHost(host);
		receiver.setPort(port);
		receiver.setUsername(username);
		receiver.setPassword(password);
		receiver.setDateCreated(new Date());
		
		return jmsBrokerRepository.save(receiver);
	}
	
	@Override
	public String getAppFolderName() {
		return "sender";
	}
	
}
