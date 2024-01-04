package org.openmrs.eip.app.route;

import java.util.List;
import java.util.Map;

import org.apache.camel.ProducerTemplate;
import org.openmrs.eip.app.SyncConstants;
import org.openmrs.eip.app.management.entity.AbstractEntity;
import org.openmrs.eip.app.management.entity.sender.Event;
import org.openmrs.eip.component.SyncContext;

public final class TestUtils {
	
	public static <T extends AbstractEntity> T getEntity(Class<T> clazz, Long id) {
		ProducerTemplate template = SyncContext.getBean(ProducerTemplate.class);
		final String classname = clazz.getSimpleName();
		String query = "jpa:" + classname + "?query=SELECT i FROM " + classname + " i WHERE i.id = " + id;
		List<T> matches = template.requestBody(query, null, List.class);
		if (matches.size() == 1) {
			return matches.get(0);
		}
		
		return null;
	}
	
	public static <T extends AbstractEntity> List<T> getEntities(Class<T> clazz) {
		ProducerTemplate t = SyncContext.getBean(ProducerTemplate.class);
		final String classname = clazz.getSimpleName();
		return t.requestBody("jpa:" + classname + "?query=SELECT i FROM " + classname + " i", null, List.class);
	}
	
	public static <T extends AbstractEntity> void saveEntity(T entity) {
		ProducerTemplate t = SyncContext.getBean(ProducerTemplate.class);
		t.sendBody("jpa:" + entity.getClass().getSimpleName() + "?usePersist=true", entity);
	}
	
	public static <T extends AbstractEntity> void updateEntity(T entity) {
		ProducerTemplate t = SyncContext.getBean(ProducerTemplate.class);
		t.sendBody("jpa:" + entity.getClass().getSimpleName(), entity);
	}
	
	public static <T extends AbstractEntity> void deleteAll(Class<T> clazz) {
		ProducerTemplate t = SyncContext.getBean(ProducerTemplate.class);
		final String classname = clazz.getSimpleName();
		t.sendBody("jpa:" + classname + "?query=DELETE FROM " + classname, null);
	}
	
	public static Map getRowById(String tableName, Long id) {
		ProducerTemplate t = SyncContext.getBean(ProducerTemplate.class);
		List<Map> matches = t.requestBody(
		    "sql:SELECT * FROM " + tableName + " WHERE id = " + id + "?dataSource=#" + SyncConstants.MGT_DATASOURCE_NAME,
		    null, List.class);
		if (matches.isEmpty()) {
			return null;
		}
		
		return matches.get(0);
	}
	
	public static Event createEvent(String table, String identifier, String op) {
		Event event = new Event();
		event.setTableName(table);
		event.setIdentifier(identifier);
		event.setOperation(op);
		event.setSnapshot(false);
		return event;
	}
	
}
