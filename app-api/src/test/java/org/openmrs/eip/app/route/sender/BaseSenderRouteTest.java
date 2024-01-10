package org.openmrs.eip.app.route.sender;

import java.util.Date;

import org.openmrs.eip.app.management.entity.sender.DebeziumEvent;
import org.openmrs.eip.app.management.entity.sender.Event;
import org.openmrs.eip.app.route.BaseRouteTest;
import org.openmrs.eip.component.SyncProfiles;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles(SyncProfiles.SENDER)
public abstract class BaseSenderRouteTest extends BaseRouteTest {
	
	protected Event createEvent(String table, String pkId, String identifier, String op) {
		Event event = new Event();
		event.setTableName(table);
		event.setPrimaryKeyId(pkId);
		event.setIdentifier(identifier);
		event.setOperation(op);
		event.setSnapshot(false);
		event.setEventDate(new Date());
		event.setDateCreated(event.getEventDate());
		return event;
	}
	
	protected DebeziumEvent createDebeziumEvent(String table, String pkId, String uuid, String op) {
		DebeziumEvent dbzmEvent = new DebeziumEvent();
		dbzmEvent.setEvent(createEvent(table, pkId, uuid, op));
		return dbzmEvent;
	}
	
	@Override
	public String getAppFolderName() {
		return "sender";
	}
	
}
