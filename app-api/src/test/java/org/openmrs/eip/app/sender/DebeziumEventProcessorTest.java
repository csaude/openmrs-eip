package org.openmrs.eip.app.sender;

import static org.apache.commons.collections.CollectionUtils.isEqualCollection;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openmrs.eip.app.BaseQueueProcessor;
import org.openmrs.eip.app.management.entity.sender.DebeziumEvent;
import org.openmrs.eip.app.management.repository.DebeziumEventRepository;
import org.openmrs.eip.component.SyncOperation;
import org.openmrs.eip.component.entity.Event;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@RunWith(PowerMockRunner.class)
public class DebeziumEventProcessorTest {
	
	private DebeziumEventProcessor processor;
	
	@Mock
	private DebeziumEventRepository mockRepo;
	
	private DebeziumEvent createEvent() {
		DebeziumEvent de = new DebeziumEvent();
		de.setEvent(new Event());
		return de;
	}
	
	@Before
	public void setup() {
		Whitebox.setInternalState(BaseQueueProcessor.class, "initialized", true);
		processor = new DebeziumEventProcessor(null, null, mockRepo);
	}
	
	@After
	public void tearDown() {
		setInternalState(BaseQueueProcessor.class, "initialized", false);
	}
	
	@Test
	public void getProcessorName_shouldReturnTheProcessorName() {
		assertEquals("db event", processor.getProcessorName());
	}
	
	@Test
	public void getThreadName_shouldReturnTheThreadNameContainingEventDetails() {
		final String table = "visit";
		final String visitId = "4";
		final Long id = 2L;
		final String uuid = "som-visit-uuid";
		DebeziumEvent de = createEvent();
		de.setId(id);
		de.getEvent().setTableName(table);
		de.getEvent().setIdentifier(uuid);
		de.getEvent().setPrimaryKeyId(visitId);
		assertEquals(table + "-" + visitId + "-" + uuid, processor.getThreadName(de));
	}
	
	@Test
	public void getThreadName_shouldExcludeTheIdentifierInTheThreadNameIfNotSpecified() {
		final String table = "visit";
		final String visitId = "4";
		final Long id = 2L;
		DebeziumEvent de = createEvent();
		de.setId(id);
		de.getEvent().setTableName(table);
		de.getEvent().setPrimaryKeyId(visitId);
		assertEquals(table + "-" + visitId, processor.getThreadName(de));
	}
	
	@Test
	public void getUniqueId_shouldReturnThePrimaryKeyId() {
		final String visitId = "4";
		DebeziumEvent de = createEvent();
		de.getEvent().setPrimaryKeyId(visitId);
		assertEquals(visitId, processor.getUniqueId(de));
	}
	
	@Test
	public void getLogicalType_shouldReturnTheTableName() {
		final String table = "visit";
		DebeziumEvent de = createEvent();
		de.getEvent().setTableName(table);
		assertEquals(table, processor.getLogicalType(de));
	}
	
	@Test
	public void getLogicalTypeHierarchy_shouldReturnTheTablesInTheSameHierarchy() {
		assertEquals(1, processor.getLogicalTypeHierarchy("visit").size());
		assertEquals(2, processor.getLogicalTypeHierarchy("person").size());
		assertEquals(3, processor.getLogicalTypeHierarchy("orders").size());
	}
	
	@Test
	public void getQueueName_shouldReturnTheQueueName() {
		assertEquals("db-event", processor.getQueueName());
	}
	
	@Test
	public void processWork_shouldSquashEventsForTheSameRowAndSendOnlyTheMostRecent() throws Exception {
		final String pk1 = "1";
		final String personTable = "person";
		Event event = new Event();
		event.setTableName(personTable);
		event.setPrimaryKeyId(pk1);
		DebeziumEvent e1 = new DebeziumEvent();
		e1.setId(1L);
		e1.setEvent(event);
		DebeziumEvent e2 = new DebeziumEvent();
		e2.setId(2L);
		e2.setEvent(event);
		
		final String pk2 = "2";
		event = new Event();
		event.setTableName(personTable);
		event.setPrimaryKeyId(pk2);
		DebeziumEvent e3 = new DebeziumEvent();
		e3.setId(3L);
		e3.setEvent(event);
		DebeziumEvent e4 = new DebeziumEvent();
		e4.setId(4L);
		e4.setEvent(event);
		DebeziumEvent e5 = new DebeziumEvent();
		e5.setId(5L);
		e5.setEvent(event);
		event = new Event();
		event.setTableName(personTable);
		event.setPrimaryKeyId(pk2);
		event.setOperation(SyncOperation.d.name());
		DebeziumEvent e6 = new DebeziumEvent();
		e6.setId(6L);
		e6.setEvent(event);
		
		processor = Mockito.spy(processor);
		List<DebeziumEvent> events = List.of(e1, e2, e3, e4, e5, e6);
		List<DebeziumEvent> processedEvents = new ArrayList();
		Mockito.doAnswer(invocation -> {
			List<DebeziumEvent> eventList = invocation.getArgument(0);
			processedEvents.addAll(eventList);
			return null;
		}).when(processor).doProcessWork(anyList());
		
		processor.processWork(events);
		
		Mockito.verify(processor).doProcessWork(processedEvents);
		assertTrue(isEqualCollection(List.of(e2, e5), processedEvents));
		Mockito.verify(mockRepo).deleteAll(List.of(e1, e3, e4));
	}
	
}
