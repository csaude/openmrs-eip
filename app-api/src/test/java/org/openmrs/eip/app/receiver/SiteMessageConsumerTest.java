package org.openmrs.eip.app.receiver;

import static java.util.Collections.synchronizedList;
import static java.util.Collections.synchronizedSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.openmrs.eip.app.SyncConstants.THREAD_THRESHOLD_MULTIPLIER;
import static org.openmrs.eip.app.receiver.ReceiverConstants.DEFAULT_TASK_BATCH_SIZE;
import static org.openmrs.eip.app.receiver.ReceiverConstants.EX_PROP_ERR_MSG;
import static org.openmrs.eip.app.receiver.ReceiverConstants.EX_PROP_ERR_TYPE;
import static org.openmrs.eip.app.receiver.ReceiverConstants.EX_PROP_FOUND_CONFLICT;
import static org.openmrs.eip.app.receiver.ReceiverConstants.EX_PROP_MSG_PROCESSED;
import static org.openmrs.eip.app.receiver.ReceiverConstants.PROP_SYNC_TASK_BATCH_SIZE;
import static org.openmrs.eip.app.receiver.ReceiverConstants.ROUTE_ID_MSG_PROCESSOR;
import static org.openmrs.eip.app.receiver.ReceiverConstants.URI_MSG_PROCESSOR;
import static org.powermock.reflect.Whitebox.getInternalState;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openmrs.eip.app.management.entity.receiver.SiteInfo;
import org.openmrs.eip.app.management.entity.receiver.SyncMessage;
import org.openmrs.eip.app.management.entity.receiver.SyncedMessage.SyncOutcome;
import org.openmrs.eip.app.management.service.ReceiverService;
import org.openmrs.eip.component.SyncContext;
import org.openmrs.eip.component.camel.utils.CamelUtils;
import org.openmrs.eip.component.exception.EIPException;
import org.openmrs.eip.component.model.DrugOrderModel;
import org.openmrs.eip.component.model.OrderModel;
import org.openmrs.eip.component.model.PatientModel;
import org.openmrs.eip.component.model.PersonModel;
import org.openmrs.eip.component.model.TestOrderModel;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.PageRequest;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ SyncContext.class, ReceiverContext.class, CamelUtils.class })
public class SiteMessageConsumerTest {
	
	private static final String MOCK_PROCESSOR_URI = "mock:" + ROUTE_ID_MSG_PROCESSOR;
	
	private SiteMessageConsumer consumer;
	
	private ThreadPoolExecutor executor;
	
	private SiteInfo siteInfo;
	
	@Mock
	private ProducerTemplate mockProducerTemplate;
	
	@Mock
	private CamelContext mockCamelContext;
	
	@Mock
	private ReceiverService mockService;
	
	@Mock
	private Environment mockEnv;
	
	@Before
	public void setup() {
		PowerMockito.mockStatic(SyncContext.class);
		PowerMockito.mockStatic(ReceiverContext.class);
		PowerMockito.mockStatic(CamelUtils.class);
		setInternalState(SiteMessageConsumer.class, "initialized", true);
		siteInfo = new SiteInfo();
		siteInfo.setIdentifier("testSite");
		Mockito.when(mockProducerTemplate.getCamelContext()).thenReturn(mockCamelContext);
		Mockito.when(SyncContext.getBean(Environment.class)).thenReturn(mockEnv);
		setInternalState(SiteMessageConsumer.class, "PROCESSING_MSG_QUEUE", synchronizedSet(new HashSet<>()));
	}
	
	@After
	public void tearDown() {
		setInternalState(BaseSiteRunnable.class, "initialized", false);
		setInternalState(SiteMessageConsumer.class, "page", (Object) null);
		setInternalState(SiteMessageConsumer.class, "PROCESSING_MSG_QUEUE", (Object) null);
	}
	
	private void setupConsumer(final int size) {
		consumer = createConsumer(size);
	}
	
	private SiteMessageConsumer createConsumer(final int size) {
		executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(size);
		SiteMessageConsumer c = new SiteMessageConsumer(URI_MSG_PROCESSOR, siteInfo, executor);
		Whitebox.setInternalState(c, ProducerTemplate.class, mockProducerTemplate);
		Whitebox.setInternalState(c, ReceiverService.class, mockService);
		setInternalState(SiteMessageConsumer.class, "page", PageRequest.of(0, DEFAULT_TASK_BATCH_SIZE));
		return c;
	}
	
	private SyncMessage createMessage(int index, boolean snapshot) {
		SyncMessage m = new SyncMessage();
		m.setId(Long.valueOf(index));
		m.setModelClassName(PersonModel.class.getName());
		m.setIdentifier("msg" + index);
		m.setSite(siteInfo);
		m.setSnapshot(snapshot);
		return m;
	}
	
	@Test
	public void initIfNecessary_shouldInitializeStaticFields() {
		setupConsumer(1);
		setInternalState(SiteMessageConsumer.class, "initialized", false);
		setInternalState(SiteMessageConsumer.class, "taskThreshold", 0);
		setInternalState(SiteMessageConsumer.class, "page", (Object) null);
		final int batchSize = 4;
		Mockito.when(mockEnv.getProperty(PROP_SYNC_TASK_BATCH_SIZE, Integer.class, DEFAULT_TASK_BATCH_SIZE))
		        .thenReturn(batchSize);
		
		consumer.initIfNecessary();
		
		Assert.assertTrue(getInternalState(SiteMessageConsumer.class, "initialized"));
		final int expected = executor.getMaximumPoolSize() * THREAD_THRESHOLD_MULTIPLIER;
		assertEquals(expected, ((Integer) getInternalState(SiteMessageConsumer.class, "taskThreshold")).intValue());
		assertEquals(PageRequest.of(0, 4), getInternalState(SiteMessageConsumer.class, "page"));
	}
	
	@Test
	public void initIfNecessary_shouldSkipIfAlreadyInitialized() {
		setupConsumer(1);
		setInternalState(SiteMessageConsumer.class, "taskThreshold", 0);
		assertEquals(true, getInternalState(SiteMessageConsumer.class, "initialized"));
		assertEquals(0, ((Integer) getInternalState(SiteMessageConsumer.class, "taskThreshold")).intValue());
		assertEquals(PageRequest.of(0, DEFAULT_TASK_BATCH_SIZE), getInternalState(SiteMessageConsumer.class, "page"));
		
		consumer.initIfNecessary();
		
		assertEquals(true, getInternalState(SiteMessageConsumer.class, "initialized"));
		assertEquals(0, ((Integer) getInternalState(SiteMessageConsumer.class, "taskThreshold")).intValue());
		assertEquals(PageRequest.of(0, DEFAULT_TASK_BATCH_SIZE), getInternalState(SiteMessageConsumer.class, "page"));
	}
	
	@Test
	public void processMessages_shouldProcessAllSnapshotMessagesInParallelForSlowThreads() throws Exception {
		Thread originalThread = Thread.currentThread();
		final String originalThreadName = Thread.currentThread().getName();
		final int size = 50;
		setupConsumer(size);
		List<SyncMessage> messages = new ArrayList(size);
		List<Long> expectedResults = synchronizedList(new ArrayList(size));
		Map<Long, Thread> expectedMsgIdThreadMap = new ConcurrentHashMap(size);
		Map<Long, String> expectedMsgIdThreadNameMap = new ConcurrentHashMap(size);
		
		for (int i = 0; i < size; i++) {
			SyncMessage m = createMessage(i, true);
			messages.add(m);
			Mockito.when(CamelUtils.send(eq(URI_MSG_PROCESSOR), any(Exchange.class))).thenAnswer(invocation -> {
				Thread.sleep(500);
				Exchange exchange = invocation.getArgument(1);
				SyncMessage arg = exchange.getIn().getBody(SyncMessage.class);
				expectedResults.add(arg.getId());
				expectedMsgIdThreadNameMap.put(arg.getId(), Thread.currentThread().getName());
				exchange.setProperty(EX_PROP_MSG_PROCESSED, true);
				return null;
			});
		}
		
		consumer.processMessages(messages);
		
		assertEquals(originalThreadName, Thread.currentThread().getName());
		assertEquals(size, expectedResults.size());
		assertEquals(size, expectedMsgIdThreadNameMap.size());
		
		for (int i = 0; i < size; i++) {
			SyncMessage msg = messages.get(i);
			assertTrue(expectedResults.contains(msg.getId()));
			assertNotEquals(originalThread, expectedMsgIdThreadMap.get(msg.getId()));
			assertEquals(consumer.getThreadName(msg), expectedMsgIdThreadNameMap.get(msg.getId()).split(":")[1]);
		}
	}
	
	@Test
	public void processMessages_shouldProcessAllSnapshotMessagesInParallelForFastThreads() throws Exception {
		Thread originalThread = Thread.currentThread();
		final String originalThreadName = Thread.currentThread().getName();
		final int size = 100;
		setupConsumer(size);
		List<SyncMessage> messages = new ArrayList(size);
		List<Long> expectedResults = synchronizedList(new ArrayList(size));
		Map<Long, Thread> expectedMsgIdThreadMap = new ConcurrentHashMap(size);
		Map<Long, String> expectedMsgIdThreadNameMap = new ConcurrentHashMap(size);
		
		for (int i = 0; i < size; i++) {
			SyncMessage m = createMessage(i, true);
			messages.add(m);
			Mockito.when(CamelUtils.send(eq(URI_MSG_PROCESSOR), any(Exchange.class))).thenAnswer(invocation -> {
				Exchange exchange = invocation.getArgument(1);
				SyncMessage arg = exchange.getIn().getBody(SyncMessage.class);
				expectedResults.add(arg.getId());
				expectedMsgIdThreadNameMap.put(arg.getId(), Thread.currentThread().getName());
				exchange.setProperty(EX_PROP_MSG_PROCESSED, true);
				return null;
			});
		}
		
		consumer.processMessages(messages);
		
		assertEquals(originalThreadName, Thread.currentThread().getName());
		assertEquals(size, expectedResults.size());
		assertEquals(size, expectedMsgIdThreadNameMap.size());
		
		for (int i = 0; i < size; i++) {
			SyncMessage msg = messages.get(i);
			assertTrue(expectedResults.contains(msg.getId()));
			assertNotEquals(originalThread, expectedMsgIdThreadMap.get(msg.getId()));
			assertEquals(consumer.getThreadName(msg), expectedMsgIdThreadNameMap.get(msg.getId()).split(":")[1]);
		}
	}
	
	@Test
	public void processMessages_shouldProcessAllNonSnapshotMessagesInParallel() throws Exception {
		Thread originalThread = Thread.currentThread();
		final String originalThreadName = Thread.currentThread().getName();
		final int size = 10;
		setupConsumer(size);
		List<SyncMessage> messages = new ArrayList(size);
		List<Long> expectedResults = synchronizedList(new ArrayList(size));
		Map<Long, Thread> expectedMsgIdThreadMap = new ConcurrentHashMap(size);
		Map<Long, String> expectedMsgIdThreadNameMap = new ConcurrentHashMap(size);
		
		for (int i = 0; i < size; i++) {
			SyncMessage m = createMessage(i, false);
			messages.add(m);
			Mockito.when(CamelUtils.send(eq(URI_MSG_PROCESSOR), any(Exchange.class))).thenAnswer(invocation -> {
				Thread.sleep(500);
				Exchange exchange = invocation.getArgument(1);
				SyncMessage arg = exchange.getIn().getBody(SyncMessage.class);
				expectedResults.add(arg.getId());
				expectedMsgIdThreadMap.put(arg.getId(), Thread.currentThread());
				expectedMsgIdThreadNameMap.put(arg.getId(), Thread.currentThread().getName());
				exchange.setProperty(EX_PROP_MSG_PROCESSED, true);
				return null;
			});
		}
		
		consumer.processMessages(messages);
		
		assertEquals(originalThreadName, Thread.currentThread().getName());
		assertEquals(size, expectedResults.size());
		assertEquals(size, expectedMsgIdThreadNameMap.size());
		
		for (int i = 0; i < size; i++) {
			SyncMessage msg = messages.get(i);
			assertTrue(expectedResults.contains(msg.getId()));
			assertNotEquals(originalThread, expectedMsgIdThreadMap.get(msg.getId()));
			assertEquals(consumer.getThreadName(msg), expectedMsgIdThreadNameMap.get(msg.getId()).split(":")[1]);
		}
	}
	
	@Test
	public void processMessages_shouldProcessOnlyTheFirstMessageForAnEntityAndSkipTheOthersForTheSameEntity()
	    throws Exception {
		Thread originalThread = Thread.currentThread();
		final String originalThreadName = Thread.currentThread().getName();
		final int size = 20;
		setupConsumer(size);
		List<SyncMessage> messages = new ArrayList(size);
		List<Long> expectedResults = synchronizedList(new ArrayList(size));
		Map<Long, Thread> expectedMsgIdThreadMap = new ConcurrentHashMap(size);
		Map<Long, String> expectedMsgIdThreadNameMap = new ConcurrentHashMap(size);
		List<SyncMessage> sameEntityMessages = new ArrayList();
		final int multiplesOf = 4;
		final int expectedProcessedMsgSize = 17;
		
		for (int i = 0; i < size; i++) {
			SyncMessage m = createMessage(i, false);
			if (i > 0 && i % multiplesOf == 0) {
				m.setIdentifier("same-uuid");
				sameEntityMessages.add(m);
			}
			messages.add(m);
			Mockito.when(CamelUtils.send(eq(URI_MSG_PROCESSOR), any(Exchange.class))).thenAnswer(invocation -> {
				Exchange exchange = invocation.getArgument(1);
				SyncMessage arg = exchange.getIn().getBody(SyncMessage.class);
				expectedResults.add(arg.getId());
				expectedMsgIdThreadMap.put(arg.getId(), Thread.currentThread());
				expectedMsgIdThreadNameMap.put(arg.getId(), Thread.currentThread().getName());
				exchange.setProperty(EX_PROP_MSG_PROCESSED, true);
				return null;
			});
		}
		
		consumer.processMessages(messages);
		
		assertEquals(originalThreadName, Thread.currentThread().getName());
		assertEquals(expectedProcessedMsgSize, expectedResults.size());
		assertEquals(expectedProcessedMsgSize, expectedMsgIdThreadNameMap.size());
		
		for (int i = 0; i < size; i++) {
			SyncMessage msg = messages.get(i);
			//All other messages for the same entity are skipped after first for the entity is encountered
			if (i > multiplesOf && i % multiplesOf == 0) {
				assertFalse(expectedResults.contains(msg.getId()));
				assertFalse(expectedMsgIdThreadMap.containsKey(msg.getId()));
				assertFalse(expectedMsgIdThreadNameMap.containsKey(msg.getId()));
			} else {
				assertTrue(expectedResults.contains(msg.getId()));
				assertNotNull(expectedMsgIdThreadMap.get(msg.getId()));
				assertNotEquals(originalThread, expectedMsgIdThreadMap.get(msg.getId()));
				assertEquals(consumer.getThreadName(msg), expectedMsgIdThreadNameMap.get(msg.getId()).split(":")[1]);
			}
		}
	}
	
	@Test
	public void processMessages_shouldSkipMessagesForTheSamePatientIfPrecededByPersonMessages() throws Exception {
		Thread originalThread = Thread.currentThread();
		final String originalThreadName = Thread.currentThread().getName();
		final int size = 3;
		setupConsumer(size);
		List<SyncMessage> messages = new ArrayList(size);
		List<Long> expectedResults = synchronizedList(new ArrayList(size));
		Map<Long, Thread> expectedMsgIdThreadMap = new ConcurrentHashMap(size);
		Map<Long, String> expectedMsgIdThreadNameMap = new ConcurrentHashMap(size);
		
		for (int i = 0; i < size; i++) {
			SyncMessage m = createMessage(i, true);
			m.setIdentifier("same-uuid");
			m.setModelClassName(i == 0 ? PersonModel.class.getName() : PatientModel.class.getName());
			messages.add(m);
			Mockito.when(CamelUtils.send(eq(URI_MSG_PROCESSOR), any(Exchange.class))).thenAnswer(invocation -> {
				Exchange exchange = invocation.getArgument(1);
				SyncMessage arg = exchange.getIn().getBody(SyncMessage.class);
				expectedResults.add(arg.getId());
				expectedMsgIdThreadMap.put(arg.getId(), Thread.currentThread());
				expectedMsgIdThreadNameMap.put(arg.getId(), Thread.currentThread().getName());
				exchange.setProperty(EX_PROP_MSG_PROCESSED, true);
				return null;
			});
		}
		
		consumer.processMessages(messages);
		
		assertEquals(originalThreadName, Thread.currentThread().getName());
		assertEquals(1, expectedResults.size());
		assertEquals(1, expectedMsgIdThreadNameMap.size());
		
		SyncMessage firstMsg = messages.get(0);
		assertTrue(expectedResults.contains(firstMsg.getId()));
		assertNotNull(expectedMsgIdThreadMap.get(firstMsg.getId()));
		assertNotEquals(originalThread, expectedMsgIdThreadMap.get(firstMsg.getId()));
		assertEquals(consumer.getThreadName(firstMsg), expectedMsgIdThreadNameMap.get(firstMsg.getId()).split(":")[1]);
		
		//All other subclass messages for the same entity are skipped after first for the entity is encountered
		for (int i = 1; i < size; i++) {
			SyncMessage msg = messages.get(i);
			assertFalse(expectedResults.contains(msg.getId()));
			assertFalse(expectedMsgIdThreadMap.containsKey(msg.getId()));
			assertFalse(expectedMsgIdThreadNameMap.containsKey(msg.getId()));
		}
	}
	
	@Test
	public void processMessages_shouldSkipMessagesForTheSamePersonIfPrecededByPatientMessages() throws Exception {
		Thread originalThread = Thread.currentThread();
		final String originalThreadName = Thread.currentThread().getName();
		final int size = 3;
		setupConsumer(size);
		List<SyncMessage> messages = new ArrayList(size);
		List<Long> expectedResults = synchronizedList(new ArrayList(size));
		Map<Long, Thread> expectedMsgIdThreadMap = new ConcurrentHashMap(size);
		Map<Long, String> expectedMsgIdThreadNameMap = new ConcurrentHashMap(size);
		
		for (int i = 0; i < size; i++) {
			SyncMessage m = createMessage(i, true);
			m.setIdentifier("same-uuid");
			m.setModelClassName(i == 0 ? PatientModel.class.getName() : PersonModel.class.getName());
			messages.add(m);
			Mockito.when(CamelUtils.send(eq(URI_MSG_PROCESSOR), any(Exchange.class))).thenAnswer(invocation -> {
				Exchange exchange = invocation.getArgument(1);
				SyncMessage arg = exchange.getIn().getBody(SyncMessage.class);
				expectedResults.add(arg.getId());
				expectedMsgIdThreadMap.put(arg.getId(), Thread.currentThread());
				expectedMsgIdThreadNameMap.put(arg.getId(), Thread.currentThread().getName());
				exchange.setProperty(EX_PROP_MSG_PROCESSED, true);
				return null;
			});
		}
		
		consumer.processMessages(messages);
		
		assertEquals(originalThreadName, Thread.currentThread().getName());
		assertEquals(1, expectedResults.size());
		assertEquals(1, expectedMsgIdThreadNameMap.size());
		
		SyncMessage firstMsg = messages.get(0);
		assertTrue(expectedResults.contains(firstMsg.getId()));
		assertNotNull(expectedMsgIdThreadMap.get(firstMsg.getId()));
		assertNotEquals(originalThread, expectedMsgIdThreadMap.get(firstMsg.getId()));
		assertEquals(consumer.getThreadName(firstMsg), expectedMsgIdThreadNameMap.get(firstMsg.getId()).split(":")[1]);
		
		//All other subclass messages for the same entity are skipped after first for the entity is encountered
		for (int i = 1; i < size; i++) {
			SyncMessage msg = messages.get(i);
			assertFalse(expectedResults.contains(msg.getId()));
			assertFalse(expectedMsgIdThreadMap.containsKey(msg.getId()));
			assertFalse(expectedMsgIdThreadNameMap.containsKey(msg.getId()));
		}
	}
	
	@Test
	public void processMessages_shouldSkipMessagesForTheSameTestOrderIfPrecededByOrderMessages() throws Exception {
		Thread originalThread = Thread.currentThread();
		final String originalThreadName = Thread.currentThread().getName();
		final int size = 3;
		setupConsumer(size);
		List<SyncMessage> messages = new ArrayList(size);
		List<Long> expectedResults = synchronizedList(new ArrayList(size));
		Map<Long, Thread> expectedMsgIdThreadMap = new ConcurrentHashMap(size);
		Map<Long, String> expectedMsgIdThreadNameMap = new ConcurrentHashMap(size);
		
		for (int i = 0; i < size; i++) {
			SyncMessage m = createMessage(i, true);
			m.setIdentifier("same-uuid");
			m.setModelClassName(i == 0 ? OrderModel.class.getName() : TestOrderModel.class.getName());
			messages.add(m);
			Mockito.when(CamelUtils.send(eq(URI_MSG_PROCESSOR), any(Exchange.class))).thenAnswer(invocation -> {
				Exchange exchange = invocation.getArgument(1);
				SyncMessage arg = exchange.getIn().getBody(SyncMessage.class);
				expectedResults.add(arg.getId());
				expectedMsgIdThreadMap.put(arg.getId(), Thread.currentThread());
				expectedMsgIdThreadNameMap.put(arg.getId(), Thread.currentThread().getName());
				exchange.setProperty(EX_PROP_MSG_PROCESSED, true);
				return null;
			});
		}
		
		consumer.processMessages(messages);
		
		assertEquals(originalThreadName, Thread.currentThread().getName());
		assertEquals(1, expectedResults.size());
		assertEquals(1, expectedMsgIdThreadNameMap.size());
		
		SyncMessage firstMsg = messages.get(0);
		assertTrue(expectedResults.contains(firstMsg.getId()));
		assertNotNull(expectedMsgIdThreadMap.get(firstMsg.getId()));
		assertNotEquals(originalThread, expectedMsgIdThreadMap.get(firstMsg.getId()));
		assertEquals(consumer.getThreadName(firstMsg), expectedMsgIdThreadNameMap.get(firstMsg.getId()).split(":")[1]);
		
		//All other subclass messages for the same entity are skipped after first for the entity is encountered
		for (int i = 1; i < size; i++) {
			SyncMessage msg = messages.get(i);
			assertFalse(expectedResults.contains(msg.getId()));
			assertFalse(expectedMsgIdThreadMap.containsKey(msg.getId()));
			assertFalse(expectedMsgIdThreadNameMap.containsKey(msg.getId()));
		}
	}
	
	@Test
	public void processMessages_shouldSkipMessagesForTheSameOrderIfPrecededByTestOrderMessages() throws Exception {
		Thread originalThread = Thread.currentThread();
		final String originalThreadName = Thread.currentThread().getName();
		final int size = 3;
		setupConsumer(size);
		List<SyncMessage> messages = new ArrayList(size);
		List<Long> expectedResults = synchronizedList(new ArrayList(size));
		Map<Long, Thread> expectedMsgIdThreadMap = new ConcurrentHashMap(size);
		Map<Long, String> expectedMsgIdThreadNameMap = new ConcurrentHashMap(size);
		
		for (int i = 0; i < size; i++) {
			SyncMessage m = createMessage(i, true);
			m.setIdentifier("same-uuid");
			m.setModelClassName(i == 0 ? TestOrderModel.class.getName() : OrderModel.class.getName());
			messages.add(m);
			Mockito.when(CamelUtils.send(eq(URI_MSG_PROCESSOR), any(Exchange.class))).thenAnswer(invocation -> {
				Exchange exchange = invocation.getArgument(1);
				SyncMessage arg = exchange.getIn().getBody(SyncMessage.class);
				expectedResults.add(arg.getId());
				expectedMsgIdThreadMap.put(arg.getId(), Thread.currentThread());
				expectedMsgIdThreadNameMap.put(arg.getId(), Thread.currentThread().getName());
				exchange.setProperty(EX_PROP_MSG_PROCESSED, true);
				return null;
			});
		}
		
		consumer.processMessages(messages);
		
		assertEquals(originalThreadName, Thread.currentThread().getName());
		assertEquals(1, expectedResults.size());
		assertEquals(1, expectedMsgIdThreadNameMap.size());
		
		SyncMessage firstMsg = messages.get(0);
		assertTrue(expectedResults.contains(firstMsg.getId()));
		assertNotNull(expectedMsgIdThreadMap.get(firstMsg.getId()));
		assertNotEquals(originalThread, expectedMsgIdThreadMap.get(firstMsg.getId()));
		assertEquals(consumer.getThreadName(firstMsg), expectedMsgIdThreadNameMap.get(firstMsg.getId()).split(":")[1]);
		
		//All other subclass messages for the same entity are skipped after first for the entity is encountered
		for (int i = 1; i < size; i++) {
			SyncMessage msg = messages.get(i);
			assertFalse(expectedResults.contains(msg.getId()));
			assertFalse(expectedMsgIdThreadMap.containsKey(msg.getId()));
			assertFalse(expectedMsgIdThreadNameMap.containsKey(msg.getId()));
		}
	}
	
	@Test
	public void processMessages_shouldSkipMessagesForTheSameDrugOrderIfPrecededByOrderMessages() throws Exception {
		Thread originalThread = Thread.currentThread();
		final String originalThreadName = Thread.currentThread().getName();
		final int size = 3;
		setupConsumer(size);
		List<SyncMessage> messages = new ArrayList(size);
		List<Long> expectedResults = synchronizedList(new ArrayList(size));
		Map<Long, Thread> expectedMsgIdThreadMap = new ConcurrentHashMap(size);
		Map<Long, String> expectedMsgIdThreadNameMap = new ConcurrentHashMap(size);
		
		for (int i = 0; i < size; i++) {
			SyncMessage m = createMessage(i, true);
			m.setIdentifier("same-uuid");
			m.setModelClassName(i == 0 ? OrderModel.class.getName() : DrugOrderModel.class.getName());
			messages.add(m);
			Mockito.when(CamelUtils.send(eq(URI_MSG_PROCESSOR), any(Exchange.class))).thenAnswer(invocation -> {
				Exchange exchange = invocation.getArgument(1);
				SyncMessage arg = exchange.getIn().getBody(SyncMessage.class);
				expectedResults.add(arg.getId());
				expectedMsgIdThreadMap.put(arg.getId(), Thread.currentThread());
				expectedMsgIdThreadNameMap.put(arg.getId(), Thread.currentThread().getName());
				exchange.setProperty(EX_PROP_MSG_PROCESSED, true);
				return null;
			});
		}
		
		consumer.processMessages(messages);
		
		assertEquals(originalThreadName, Thread.currentThread().getName());
		assertEquals(1, expectedResults.size());
		assertEquals(1, expectedMsgIdThreadNameMap.size());
		
		SyncMessage firstMsg = messages.get(0);
		assertTrue(expectedResults.contains(firstMsg.getId()));
		assertNotNull(expectedMsgIdThreadMap.get(firstMsg.getId()));
		assertNotEquals(originalThread, expectedMsgIdThreadMap.get(firstMsg.getId()));
		assertEquals(consumer.getThreadName(firstMsg), expectedMsgIdThreadNameMap.get(firstMsg.getId()).split(":")[1]);
		
		//All other subclass messages for the same entity are skipped after first for the entity is encountered
		for (int i = 1; i < size; i++) {
			SyncMessage msg = messages.get(i);
			assertFalse(expectedResults.contains(msg.getId()));
			assertFalse(expectedMsgIdThreadMap.containsKey(msg.getId()));
			assertFalse(expectedMsgIdThreadNameMap.containsKey(msg.getId()));
		}
	}
	
	@Test
	public void processMessages_shouldSkipMessagesForTheSameOrderIfPrecededByDrugOrderMessages() throws Exception {
		Thread originalThread = Thread.currentThread();
		final String originalThreadName = Thread.currentThread().getName();
		final int size = 3;
		setupConsumer(size);
		List<SyncMessage> messages = new ArrayList(size);
		List<Long> expectedResults = synchronizedList(new ArrayList(size));
		Map<Long, Thread> expectedMsgIdThreadMap = new ConcurrentHashMap(size);
		Map<Long, String> expectedMsgIdThreadNameMap = new ConcurrentHashMap(size);
		
		for (int i = 0; i < size; i++) {
			SyncMessage m = createMessage(i, true);
			m.setIdentifier("same-uuid");
			m.setModelClassName(i == 0 ? DrugOrderModel.class.getName() : OrderModel.class.getName());
			messages.add(m);
			Mockito.when(CamelUtils.send(eq(URI_MSG_PROCESSOR), any(Exchange.class))).thenAnswer(invocation -> {
				Exchange exchange = invocation.getArgument(1);
				SyncMessage arg = exchange.getIn().getBody(SyncMessage.class);
				expectedResults.add(arg.getId());
				expectedMsgIdThreadMap.put(arg.getId(), Thread.currentThread());
				expectedMsgIdThreadNameMap.put(arg.getId(), Thread.currentThread().getName());
				exchange.setProperty(EX_PROP_MSG_PROCESSED, true);
				return null;
			});
		}
		
		consumer.processMessages(messages);
		
		assertEquals(originalThreadName, Thread.currentThread().getName());
		assertEquals(1, expectedResults.size());
		assertEquals(1, expectedMsgIdThreadNameMap.size());
		
		SyncMessage firstMsg = messages.get(0);
		assertTrue(expectedResults.contains(firstMsg.getId()));
		assertNotNull(expectedMsgIdThreadMap.get(firstMsg.getId()));
		assertNotEquals(originalThread, expectedMsgIdThreadMap.get(firstMsg.getId()));
		assertEquals(consumer.getThreadName(firstMsg), expectedMsgIdThreadNameMap.get(firstMsg.getId()).split(":")[1]);
		
		//All other subclass messages for the same entity are skipped after first for the entity is encountered
		for (int i = 1; i < size; i++) {
			SyncMessage msg = messages.get(i);
			assertFalse(expectedResults.contains(msg.getId()));
			assertFalse(expectedMsgIdThreadMap.containsKey(msg.getId()));
			assertFalse(expectedMsgIdThreadNameMap.containsKey(msg.getId()));
		}
	}
	
	@Test
	public void processMessage_shouldAddTheMessageToTheSyncedQueueAndDeleteTheProcessedMessage() throws Exception {
		setupConsumer(1);
		Whitebox.setInternalState(consumer, "messageProcessorUri", MOCK_PROCESSOR_URI);
		SyncMessage msg = createMessage(1, false);
		msg.setMessageUuid("msg-uuid");
		msg.setEntityPayload("{}");
		msg.setDateSentBySender(LocalDateTime.now());
		msg.setDateCreated(new Date());
		
		List<SyncMessage> processedMsgs = new ArrayList(1);
		Mockito.when(CamelUtils.send(eq(MOCK_PROCESSOR_URI), any(Exchange.class))).thenAnswer(invocation -> {
			Exchange exchange = invocation.getArgument(1);
			processedMsgs.add(exchange.getIn().getBody(SyncMessage.class));
			exchange.setProperty(EX_PROP_MSG_PROCESSED, true);
			return null;
		});
		
		consumer.processMessage(msg);
		
		assertEquals(1, processedMsgs.size());
		assertEquals(msg, processedMsgs.get(0));
		verify(mockService).moveToSyncedQueue(msg, SyncOutcome.SUCCESS);
	}
	
	@Test
	public void processMessage_shouldAddTheConflictMessageToTheSyncedQueue() {
		setupConsumer(1);
		Whitebox.setInternalState(consumer, "messageProcessorUri", MOCK_PROCESSOR_URI);
		SyncMessage msg = new SyncMessage();
		List<SyncMessage> processedMsgs = new ArrayList(1);
		Mockito.when(CamelUtils.send(eq(MOCK_PROCESSOR_URI), any(Exchange.class))).thenAnswer(invocation -> {
			Exchange exchange = invocation.getArgument(1);
			processedMsgs.add(exchange.getIn().getBody(SyncMessage.class));
			exchange.setProperty(EX_PROP_FOUND_CONFLICT, true);
			return null;
		});
		
		consumer.processMessage(msg);
		
		assertEquals(1, processedMsgs.size());
		assertEquals(msg, processedMsgs.get(0));
		verify(mockService).processConflictedSyncItem(msg);
		verify(mockService, never()).moveToSyncedQueue(any(SyncMessage.class), any(SyncOutcome.class));
	}
	
	@Test
	public void processMessage_shouldAddTheErrorMessageToTheSyncedQueue() {
		setupConsumer(1);
		Whitebox.setInternalState(consumer, "messageProcessorUri", MOCK_PROCESSOR_URI);
		SyncMessage msg = new SyncMessage();
		List<SyncMessage> processedMsgs = new ArrayList(1);
		final String errType = EIPException.class.getName();
		final String errMsg = "testing";
		Mockito.when(CamelUtils.send(eq(MOCK_PROCESSOR_URI), any(Exchange.class))).thenAnswer(invocation -> {
			Exchange exchange = invocation.getArgument(1);
			processedMsgs.add(exchange.getIn().getBody(SyncMessage.class));
			exchange.setProperty(EX_PROP_ERR_TYPE, errType);
			exchange.setProperty(EX_PROP_ERR_MSG, errMsg);
			return null;
		});
		
		consumer.processMessage(msg);
		
		assertEquals(1, processedMsgs.size());
		assertEquals(msg, processedMsgs.get(0));
		verify(mockService).processFailedSyncItem(msg, errType, errMsg);
		verify(mockService, never()).moveToSyncedQueue(any(SyncMessage.class), any(SyncOutcome.class));
	}
	
	@Test
	public void processMessage_shouldFailIfTheSyncOutComeIsUnknown() {
		setupConsumer(1);
		Whitebox.setInternalState(consumer, "messageProcessorUri", MOCK_PROCESSOR_URI);
		SyncMessage msg = new SyncMessage();
		List<SyncMessage> processedMsgs = new ArrayList(1);
		Mockito.when(CamelUtils.send(eq(MOCK_PROCESSOR_URI), any(Exchange.class))).thenAnswer(invocation -> {
			Exchange exchange = invocation.getArgument(1);
			processedMsgs.add(exchange.getIn().getBody(SyncMessage.class));
			return null;
		});
		
		Exception thrown = Assert.assertThrows(EIPException.class, () -> consumer.processMessage(msg));
		assertEquals("Something went wrong while processing sync message -> " + msg, thrown.getMessage());
		assertEquals(1, processedMsgs.size());
		assertEquals(msg, processedMsgs.get(0));
		verifyNoInteractions(mockService);
		verify(mockProducerTemplate, never()).sendBody(anyString(), isNull());
	}
	
	@Test
	public void processMessage_shouldSkipAMessageIfAnotherSiteThreadIsProcessingAnEventForTheSameEntity() throws Exception {
		final String uuid = "same-uuid";
		final int siteCount = 5;
		List<SyncMessage> allSiteMsgs = new ArrayList(siteCount);
		List<Long> expectedResults = synchronizedList(new ArrayList(siteCount));
		List<SyncMessage> sameEntityMessages = new ArrayList();
		final int multiplesOf = 2;
		final int expectedProcessedMsgCount = 3;
		ExecutorService executor = Executors.newFixedThreadPool(siteCount);
		List<CompletableFuture<Void>> futures = new ArrayList(siteCount);
		Set<String> processingMsgQueue = synchronizedSet(new HashSet<>(siteCount));
		final String uniqueIdentifier = PersonModel.class.getName() + "#" + uuid;
		processingMsgQueue.add(uniqueIdentifier);
		setInternalState(SiteMessageConsumer.class, "PROCESSING_MSG_QUEUE", processingMsgQueue);
		for (int i = 0; i < siteCount; i++) {
			SyncMessage m = createMessage(i, false);
			if (i > 0 && i % multiplesOf == 0) {
				m.setIdentifier(uuid);
				sameEntityMessages.add(m);
			}
			allSiteMsgs.add(m);
			Mockito.when(CamelUtils.send(eq(URI_MSG_PROCESSOR), any(Exchange.class))).thenAnswer(invocation -> {
				Exchange exchange = invocation.getArgument(1);
				SyncMessage procMsg = exchange.getIn().getBody(SyncMessage.class);
				expectedResults.add(procMsg.getId());
				assertTrue(processingMsgQueue.contains(PersonModel.class.getName() + "#" + procMsg.getIdentifier()));
				exchange.setProperty(EX_PROP_MSG_PROCESSED, true);
				return null;
			});
			futures.add(CompletableFuture.runAsync(() -> {
				try {
					createConsumer(1).processMessage(m);
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
			}, executor));
		}
		
		CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()])).get();
		assertEquals(expectedProcessedMsgCount, expectedResults.size());
		for (int i = 0; i < siteCount; i++) {
			SyncMessage msg = allSiteMsgs.get(i);
			//All messages for the same entity are skipped if another site thread is processing events for same entity
			if (i > 0 && i % multiplesOf == 0) {
				assertFalse(expectedResults.contains(msg.getId()));
			} else {
				assertTrue(expectedResults.contains(msg.getId()));
			}
		}
		assertEquals(1, processingMsgQueue.size());
		assertEquals(uniqueIdentifier, processingMsgQueue.iterator().next());
	}
	
	@Test
	public void processMessage_shouldSkipASubclassMessageIfAnotherSiteThreadIsProcessingAnEventForTheSameEntity() {
		final String uuid = "person-uuid";
		SyncMessage msg = new SyncMessage();
		msg.setModelClassName(PatientModel.class.getName());
		msg.setIdentifier(uuid);
		final String uniqueId = PersonModel.class.getName() + "#" + uuid;
		Set<String> procMsgQueue = synchronizedSet(new HashSet<>());
		procMsgQueue.add(uniqueId);
		setInternalState(SiteMessageConsumer.class, "PROCESSING_MSG_QUEUE", procMsgQueue);
		
		createConsumer(1).processMessage(msg);
		
		PowerMockito.verifyStatic(CamelUtils.class, never());
		CamelUtils.send(eq(URI_MSG_PROCESSOR), any(Exchange.class));
	}
	
}
