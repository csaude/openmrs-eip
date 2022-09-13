package org.openmrs.eip.app.sender;

import static java.lang.Boolean.TRUE;
import static java.util.Collections.singletonList;
import static java.util.Collections.synchronizedList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.debezium.DebeziumConstants;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.support.DefaultMessage;
import org.apache.kafka.connect.data.ConnectSchema;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.Struct;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.openmrs.eip.app.CustomFileOffsetBackingStore;
import org.powermock.reflect.Whitebox;

public class ChangeEventProcessorTest {
	
	private static final String TABLE_ENC = "encounter";
	
	private static final String TABLE_PERSON = "person";
	
	private static final String TABLE_VISIT = "visit";
	
	private ChangeEventProcessor processor;
	
	@Mock
	private ProducerTemplate mockProducerTemplate;
	
	@Mock
	private SnapshotSavePointStore mockStore;
	
	@Mock
	private ChangeEventHandler handler;
	
	private ExecutorService executor;
	
	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		Whitebox.setInternalState(ChangeEventProcessor.class, "batchSize", (Integer) null);
		Mockito.reset(mockStore);
	}
	
	private ChangeEventProcessor createProcessor(int threadCount) {
		processor = new ChangeEventProcessor(handler);
		Whitebox.setInternalState(processor, "threadCount", threadCount);
		executor = Executors.newFixedThreadPool(threadCount);
		Whitebox.setInternalState(ChangeEventProcessor.class, "executor", executor);
		Whitebox.setInternalState(processor, ProducerTemplate.class, mockProducerTemplate);
		Whitebox.setInternalState(processor, SnapshotSavePointStore.class, mockStore);
		Mockito.when(mockStore.getSavedRowId(ArgumentMatchers.anyString())).thenReturn(null);
		return processor;
	}
	
	private Exchange createExchange(int index, String snapshot, String table) {
		Exchange exchange = new DefaultExchange(new DefaultCamelContext());
		Message message = new DefaultMessage(exchange);
		exchange.setMessage(message);
		final Field id = new Field(table + "_id", 0, new ConnectSchema(Schema.Type.INT32));
		final List<Field> ids = singletonList(id);
		final Struct primaryKey = new Struct(
		        new ConnectSchema(Schema.Type.STRUCT, false, null, "key", null, null, null, ids, null, null));
		primaryKey.put(table + "_id", index);
		message.setHeader(DebeziumConstants.HEADER_KEY, primaryKey);
		Map<String, Object> sourceMetadata = new HashMap();
		sourceMetadata.put("table", table);
		sourceMetadata.put("snapshot", snapshot);
		message.setHeader(DebeziumConstants.HEADER_SOURCE_METADATA, sourceMetadata);
		return exchange;
	}
	
	private Integer getId(Exchange e) {
		Struct primaryKeyStruct = e.getMessage().getHeader(DebeziumConstants.HEADER_KEY, Struct.class);
		return primaryKeyStruct.getInt32(primaryKeyStruct.schema().fields().get(0).name());
	}
	
	@Test
	public void process_shouldProcessAllSnapshotEventsInParallelForSlowThreads() throws Exception {
		final String originalThreadName = Thread.currentThread().getName();
		final int size = 100;
		List<Exchange> exchanges = new ArrayList(size);
		List<Integer> expectedResults = synchronizedList(new ArrayList(size));
		Map<Integer, String> expectedIdThreadNameMap = new ConcurrentHashMap(size);
		
		for (int i = 0; i < size; i++) {
			Exchange e = createExchange(i, i < size - 1 ? TRUE.toString() : "last", TABLE_PERSON);
			exchanges.add(e);
			Mockito.doAnswer(invocation -> {
				Thread.sleep(500);
				Exchange arg = invocation.getArgument(4);
				Integer id = getId(arg);
				expectedResults.add(id);
				expectedIdThreadNameMap.put(id, Thread.currentThread().getName());
				assertTrue(CustomFileOffsetBackingStore.isPaused());
				return arg;
			}).when(handler).handle(eq(TABLE_PERSON), eq(String.valueOf(i)), eq(true), anyMap(), eq(e));
		}
		
		processor = createProcessor(size);
		for (Exchange exchange : exchanges) {
			processor.process(exchange);
		}
		
		assertEquals(originalThreadName, Thread.currentThread().getName());
		assertEquals(size, expectedResults.size());
		assertEquals(size, expectedIdThreadNameMap.size());
		assertFalse(CustomFileOffsetBackingStore.isPaused());
		Mockito.verify(mockStore).discard();
		assertNull(Whitebox.getInternalState(processor, SnapshotSavePointStore.class));
		
		for (int i = 0; i < size; i++) {
			Integer id = getId(exchanges.get(i));
			assertTrue(expectedResults.contains(id));
			assertEquals(processor.getThreadName(TABLE_PERSON, id.toString()),
			    expectedIdThreadNameMap.get(id).split(":")[2]);
		}
	}
	
	@Test
	public void process_shouldProcessAllSnapshotEventsInParallelForFastThreads() throws Exception {
		final String originalThreadName = Thread.currentThread().getName();
		final int size = 100;
		List<Exchange> exchanges = new ArrayList(size);
		List<Integer> expectedResults = synchronizedList(new ArrayList(size));
		Map<Integer, String> expectedIdThreadNameMap = new ConcurrentHashMap(size);
		
		for (int i = 0; i < size; i++) {
			Exchange e = createExchange(i, i < size - 1 ? TRUE.toString() : "last", TABLE_PERSON);
			exchanges.add(e);
			Mockito.doAnswer(invocation -> {
				Exchange arg = invocation.getArgument(4);
				Integer id = getId(arg);
				expectedResults.add(id);
				expectedIdThreadNameMap.put(id, Thread.currentThread().getName());
				assertTrue(CustomFileOffsetBackingStore.isPaused());
				return arg;
			}).when(handler).handle(eq(TABLE_PERSON), eq(String.valueOf(i)), eq(true), anyMap(), eq(e));
		}
		
		processor = createProcessor(size);
		for (Exchange exchange : exchanges) {
			processor.process(exchange);
		}
		
		assertEquals(originalThreadName, Thread.currentThread().getName());
		assertEquals(size, expectedResults.size());
		assertEquals(size, expectedIdThreadNameMap.size());
		assertFalse(CustomFileOffsetBackingStore.isPaused());
		Mockito.verify(mockStore).discard();
		assertNull(Whitebox.getInternalState(processor, SnapshotSavePointStore.class));
		
		for (int i = 0; i < size; i++) {
			Integer id = getId(exchanges.get(i));
			assertTrue(expectedResults.contains(id));
			assertEquals(processor.getThreadName(TABLE_PERSON, id.toString()),
			    expectedIdThreadNameMap.get(id).split(":")[2]);
		}
	}
	
	@Test
	public void process_UpdateTheSavePointStoreBasedOnTheBatchSize() throws Exception {
		final String originalThreadName = Thread.currentThread().getName();
		final int size = 67;
		List<Exchange> exchanges = new ArrayList(size);
		List<Integer> expectedResults = synchronizedList(new ArrayList(size));
		Map<Integer, String> expectedIdThreadNameMap = new ConcurrentHashMap(size);
		List<Map<String, Integer>> storeUpdates = synchronizedList(new ArrayList());
		
		for (int i = 0; i < size; i++) {
			Exchange e = createExchange(i, i < size - 1 ? TRUE.toString() : "last", TABLE_PERSON);
			exchanges.add(e);
			Mockito.doAnswer(invocation -> {
				Exchange arg = invocation.getArgument(4);
				Integer id = getId(arg);
				expectedResults.add(id);
				expectedIdThreadNameMap.put(id, Thread.currentThread().getName());
				assertTrue(CustomFileOffsetBackingStore.isPaused());
				return arg;
			}).when(handler).handle(eq(TABLE_PERSON), eq(String.valueOf(i)), eq(true), anyMap(), eq(e));
		}
		
		processor = createProcessor(1);
		final int batchSize = 10;
		Whitebox.setInternalState(ChangeEventProcessor.class, "batchSize", batchSize);
		Mockito.doAnswer(invocation -> {
			storeUpdates.add(new HashMap(invocation.getArgument(0)));
			return null;
		}).when(mockStore).update(ArgumentMatchers.anyMap());
		
		for (Exchange exchange : exchanges) {
			processor.process(exchange);
		}
		
		assertEquals(originalThreadName, Thread.currentThread().getName());
		assertEquals(size, expectedResults.size());
		assertEquals(size, expectedIdThreadNameMap.size());
		assertFalse(CustomFileOffsetBackingStore.isPaused());
		assertNull(Whitebox.getInternalState(processor, SnapshotSavePointStore.class));
		final int storeUpdateCallCount = size / batchSize;
		Mockito.verify(mockStore, Mockito.times(storeUpdateCallCount)).update(ArgumentMatchers.anyMap());
		Mockito.verify(mockStore).discard();
		assertEquals(size / batchSize, storeUpdates.size());
		for (int i = 0; i < storeUpdateCallCount; i++) {
			assertEquals(1, storeUpdates.get(i).size());
			assertEquals(TABLE_PERSON, storeUpdates.get(i).keySet().iterator().next());
			assertEquals(((i + 1) * batchSize) - 1, storeUpdates.get(i).values().iterator().next().intValue());
		}
		
		for (int i = 0; i < size; i++) {
			Integer id = getId(exchanges.get(i));
			assertTrue(expectedResults.contains(id));
			assertEquals(processor.getThreadName(TABLE_PERSON, id.toString()),
			    expectedIdThreadNameMap.get(id).split(":")[2]);
		}
	}
	
	@Test
	public void process_shouldProcessAllNonSnapshotEventsInSerialInCurrentThread() throws Exception {
		final String originalThreadName = Thread.currentThread().getName();
		final int size = 10;
		List<Exchange> exchanges = new ArrayList(size);
		List<Integer> expectedResults = new ArrayList(size);
		List<String> threadNames = new ArrayList(size);
		
		for (int i = 0; i < size; i++) {
			Exchange e = createExchange(i, Boolean.FALSE.toString(), TABLE_PERSON);
			exchanges.add(e);
			Mockito.doAnswer(invocation -> {
				Exchange arg = invocation.getArgument(4);
				Integer id = getId(arg);
				expectedResults.add(id);
				threadNames.add(Thread.currentThread().getName());
				assertFalse(CustomFileOffsetBackingStore.isPaused());
				return arg;
			}).when(handler).handle(eq(TABLE_PERSON), eq(String.valueOf(i)), eq(false), anyMap(), eq(e));
		}
		
		processor = createProcessor(size);
		for (Exchange exchange : exchanges) {
			processor.process(exchange);
		}
		
		assertEquals(originalThreadName, Thread.currentThread().getName());
		assertEquals(size, expectedResults.size());
		assertEquals(size, threadNames.size());
		assertFalse(CustomFileOffsetBackingStore.isPaused());
		Mockito.verifyNoInteractions(mockStore);
		
		for (int i = 0; i < size; i++) {
			assertEquals(i, expectedResults.get(i).intValue());
			String threadName = threadNames.get(i);
			Integer id = getId(exchanges.get(i));
			assertEquals(originalThreadName, threadName.split(":")[0]);
			assertEquals(processor.getThreadName(TABLE_PERSON, id.toString()), threadName.split(":")[2]);
		}
	}
	
	@Test
	public void process_shouldUpdateAllTablesWithTheMaxRowIdProcessed() throws Exception {
		final String originalThreadName = Thread.currentThread().getName();
		final int encTableRowCount = 20;
		final int personTableRowCount = 10;
		final int visitTableRowCount = 15;
		final int totalRowCount = encTableRowCount + personTableRowCount + visitTableRowCount;
		List<Exchange> exchanges = new ArrayList(totalRowCount);
		List<String> expectedResults = synchronizedList(new ArrayList(totalRowCount));
		Map<String, String> expectedRowThreadNameMap = new ConcurrentHashMap(totalRowCount);
		List<Map<String, Integer>> storeUpdates = synchronizedList(new ArrayList());
		
		for (int i = 0; i < encTableRowCount; i++) {
			Exchange e = createExchange(i, TRUE.toString(), TABLE_ENC);
			exchanges.add(e);
			Mockito.doAnswer(invocation -> {
				Exchange arg = invocation.getArgument(4);
				Integer id = getId(arg);
				expectedResults.add(TABLE_ENC + id);
				expectedRowThreadNameMap.put(TABLE_ENC + id, Thread.currentThread().getName());
				assertTrue(CustomFileOffsetBackingStore.isPaused());
				return arg;
			}).when(handler).handle(eq(TABLE_ENC), eq(String.valueOf(i)), eq(true), anyMap(), eq(e));
		}
		
		for (int i = 0; i < personTableRowCount; i++) {
			Exchange e = createExchange(i, TRUE.toString(), TABLE_PERSON);
			exchanges.add(e);
			Mockito.doAnswer(invocation -> {
				Exchange arg = invocation.getArgument(4);
				Integer id = getId(arg);
				expectedResults.add(TABLE_PERSON + id);
				expectedRowThreadNameMap.put(TABLE_PERSON + id, Thread.currentThread().getName());
				assertTrue(CustomFileOffsetBackingStore.isPaused());
				return arg;
			}).when(handler).handle(eq(TABLE_PERSON), eq(String.valueOf(i)), eq(true), anyMap(), eq(e));
		}
		
		for (int i = 0; i < visitTableRowCount; i++) {
			Exchange e = createExchange(i, i < visitTableRowCount - 1 ? TRUE.toString() : "last", TABLE_VISIT);
			exchanges.add(e);
			Mockito.doAnswer(invocation -> {
				Exchange arg = invocation.getArgument(4);
				Integer id = getId(arg);
				expectedResults.add(TABLE_VISIT + id);
				expectedRowThreadNameMap.put(TABLE_VISIT + id, Thread.currentThread().getName());
				assertTrue(CustomFileOffsetBackingStore.isPaused());
				return arg;
			}).when(handler).handle(eq(TABLE_VISIT), eq(String.valueOf(i)), eq(true), anyMap(), eq(e));
		}
		
		processor = createProcessor(1);
		final int batchSize = 10;
		Whitebox.setInternalState(ChangeEventProcessor.class, "batchSize", batchSize);
		Mockito.doAnswer(invocation -> {
			storeUpdates.add(new HashMap(invocation.getArgument(0)));
			return null;
		}).when(mockStore).update(ArgumentMatchers.anyMap());
		
		for (Exchange exchange : exchanges) {
			processor.process(exchange);
		}
		
		assertEquals(originalThreadName, Thread.currentThread().getName());
		assertEquals(totalRowCount, expectedResults.size());
		assertEquals(totalRowCount, expectedRowThreadNameMap.size());
		assertFalse(CustomFileOffsetBackingStore.isPaused());
		assertNull(Whitebox.getInternalState(processor, SnapshotSavePointStore.class));
		final int storeUpdateCallCount = totalRowCount / batchSize;
		Mockito.verify(mockStore, Mockito.times(storeUpdateCallCount)).update(ArgumentMatchers.anyMap());
		Mockito.verify(mockStore).discard();
		assertEquals(totalRowCount / batchSize, storeUpdates.size());
		Map<String, Integer> storeUpdate1 = storeUpdates.get(0);
		Map<String, Integer> storeUpdate2 = storeUpdates.get(1);
		Map<String, Integer> storeUpdate3 = storeUpdates.get(2);
		Map<String, Integer> storeUpdate4 = storeUpdates.get(3);
		assertEquals(1, storeUpdate1.size());//0-9
		assertEquals(TABLE_ENC, storeUpdate1.keySet().iterator().next());
		assertEquals(9, storeUpdate1.values().iterator().next().intValue());
		
		assertEquals(1, storeUpdate2.size());//10-19
		assertEquals(TABLE_ENC, storeUpdate2.keySet().iterator().next());
		assertEquals(19, storeUpdate2.values().iterator().next().intValue());
		
		assertEquals(2, storeUpdate3.size());//20-29
		assertTrue(storeUpdate3.keySet().contains(TABLE_ENC));
		assertTrue(storeUpdate3.keySet().contains(TABLE_PERSON));
		assertEquals(19, storeUpdate3.get(TABLE_ENC).intValue());
		assertEquals(9, storeUpdate3.get(TABLE_PERSON).intValue());
		
		assertEquals(3, storeUpdate4.size());//30-39 40-45
		assertTrue(storeUpdate4.keySet().contains(TABLE_ENC));
		assertTrue(storeUpdate4.keySet().contains(TABLE_PERSON));
		assertTrue(storeUpdate4.keySet().contains(TABLE_VISIT));
		assertEquals(19, storeUpdate4.get(TABLE_ENC).intValue());
		assertEquals(9, storeUpdate4.get(TABLE_PERSON).intValue());
		assertEquals(9, storeUpdate4.get(TABLE_VISIT).intValue());
		
		for (int i = 0; i < encTableRowCount; i++) {
			Integer id = getId(exchanges.get(i));
			assertTrue(expectedResults.contains(TABLE_ENC + id));
			assertEquals(processor.getThreadName(TABLE_ENC, id.toString()),
			    expectedRowThreadNameMap.get(TABLE_ENC + id).split(":")[2]);
		}
		
		for (int i = 0; i < personTableRowCount; i++) {
			Integer id = getId(exchanges.get(i));
			assertTrue(expectedResults.contains(TABLE_PERSON + id));
			assertEquals(processor.getThreadName(TABLE_PERSON, id.toString()),
			    expectedRowThreadNameMap.get(TABLE_PERSON + id).split(":")[2]);
		}
		
		for (int i = 0; i < visitTableRowCount; i++) {
			Integer id = getId(exchanges.get(i));
			assertTrue(expectedResults.contains(TABLE_VISIT + id));
			assertEquals(processor.getThreadName(TABLE_VISIT, id.toString()),
			    expectedRowThreadNameMap.get(TABLE_VISIT + id).split(":")[2]);
		}
	}
	
	@Test
	public void process_shouldNotProcessPreviouslyProcessedSnapshotEvent() throws Exception {
		final String originalThreadName = Thread.currentThread().getName();
		final int size = 10;
		final int maxRowId = 6;
		List<Exchange> exchanges = new ArrayList(size);
		List<Integer> expectedResults = synchronizedList(new ArrayList(size));
		Map<Integer, String> expectedIdThreadNameMap = new ConcurrentHashMap(size);
		
		for (int i = 0; i < size; i++) {
			Exchange e = createExchange(i, i < size - 1 ? TRUE.toString() : "last", TABLE_PERSON);
			exchanges.add(e);
			Mockito.doAnswer(invocation -> {
				Exchange arg = invocation.getArgument(4);
				Integer id = getId(arg);
				expectedResults.add(id);
				expectedIdThreadNameMap.put(id, Thread.currentThread().getName());
				assertTrue(CustomFileOffsetBackingStore.isPaused());
				return arg;
			}).when(handler).handle(eq(TABLE_PERSON), eq(String.valueOf(i)), eq(true), anyMap(), eq(e));
		}
		
		processor = createProcessor(size);
		Mockito.when(mockStore.getSavedRowId(TABLE_PERSON)).thenReturn(maxRowId);
		for (Exchange exchange : exchanges) {
			processor.process(exchange);
		}
		
		assertEquals(originalThreadName, Thread.currentThread().getName());
		assertEquals(3, expectedResults.size());
		assertEquals(3, expectedIdThreadNameMap.size());
		assertFalse(CustomFileOffsetBackingStore.isPaused());
		Mockito.verify(mockStore).discard();
		assertNull(Whitebox.getInternalState(processor, SnapshotSavePointStore.class));
		
		for (int i = 0; i < size; i++) {
			Integer id = getId(exchanges.get(i));
			if (i <= maxRowId) {
				assertFalse(expectedResults.contains(id));
				assertNull(expectedIdThreadNameMap.get(id));
			} else {
				assertTrue(expectedResults.contains(id));
				assertEquals(processor.getThreadName(TABLE_PERSON, id.toString()),
				    expectedIdThreadNameMap.get(id).split(":")[2]);
			}
		}
	}
	
}
