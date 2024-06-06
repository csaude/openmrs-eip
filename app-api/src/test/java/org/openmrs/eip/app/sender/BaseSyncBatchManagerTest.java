package org.openmrs.eip.app.sender;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.openmrs.eip.app.SyncConstants.DEFAULT_LARGE_MSG_SIZE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openmrs.eip.app.management.entity.AbstractEntity;
import org.openmrs.eip.app.management.entity.sender.SenderSyncMessage;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import jakarta.jms.ConnectionFactory;

@RunWith(PowerMockRunner.class)
@PrepareForTest(SenderUtils.class)
public class BaseSyncBatchManagerTest {
	
	public class MockSyncBatchManager extends BaseSyncBatchManager {
		
		protected List<Long> updatedItemIds;
		
		protected List<Object> convertedItems = new ArrayList<>();
		
		private int batchSize = 10;
		
		public MockSyncBatchManager() {
			super(mockConnFactory);
		}
		
		public MockSyncBatchManager(int batchSize) {
			super(mockConnFactory);
			this.batchSize = batchSize;
		}
		
		@Override
		protected String getQueueName() {
			return QUEUE_NAME;
		}
		
		@Override
		protected int getBatchSize() {
			return batchSize;
		}
		
		@Override
		protected Object convert(AbstractEntity item) {
			convertedItems.add(item);
			return item;
		}
		
		@Override
		protected void updateItems(List itemIds) {
			updatedItemIds = new ArrayList<>(itemIds);
		}
		
	}
	
	private static final String QUEUE_NAME = "test";
	
	private static final String SITE_ID = "siteId";
	
	@Mock
	private ConnectionFactory mockConnFactory;
	
	private MockSyncBatchManager manager;
	
	@Before
	public void setup() {
		PowerMockito.mockStatic(SenderUtils.class);
		manager = new MockSyncBatchManager();
		Whitebox.setInternalState(manager, "largeMsgSize", DEFAULT_LARGE_MSG_SIZE);
		Whitebox.setInternalState(manager, "siteId", SITE_ID);
	}
	
	@Test
	public void add_shouldConvertStoreItemAndItsIdAndInvokeSend() {
		final Long id = 3L;
		MockSyncBatchManager manager = new MockSyncBatchManager();
		SenderSyncMessage msg = new SenderSyncMessage();
		msg.setId(id);
		manager = Mockito.spy(manager);
		Mockito.doNothing().when(manager).send(false);
		
		manager.add(msg);
		List<Object> items = Whitebox.getInternalState(manager, "buffer");
		assertTrue(items.contains(msg));
		List<Long> itemIds = Whitebox.getInternalState(manager, "itemIds");
		assertTrue(itemIds.contains(id));
		Mockito.verify(manager).send(false);
		
	}
	
	@Test
	public void add_shouldAddAndSendAllTheItemsIfTheyMatchTheBatchSizeInAThreadSafeWayAndResetTheBuffer() {
		final int count = 50;
		ExecutorService executor = Executors.newFixedThreadPool(count);
		List<CompletableFuture<Void>> futures = new ArrayList(count);
		final MockSyncBatchManager manager = new MockSyncBatchManager(count);
		Whitebox.setInternalState(manager, "largeMsgSize", DEFAULT_LARGE_MSG_SIZE);
		Whitebox.setInternalState(manager, "siteId", SITE_ID);
		List<Object> expectedItems = new ArrayList<>();
		PowerMockito.doAnswer(i -> expectedItems.addAll(i.getArgument(2))).when(SenderUtils.class);
		SenderUtils.sendBatch(eq(mockConnFactory), eq(SITE_ID), ArgumentMatchers.anyList(), eq(DEFAULT_LARGE_MSG_SIZE));
		List<SenderSyncMessage> msgs = Collections.synchronizedList(new ArrayList<>(count));
		for (long i = 0; i < count; i++) {
			final Long id = i + 1;
			futures.add(CompletableFuture.runAsync(() -> {
				SenderSyncMessage msg = new SenderSyncMessage();
				msg.setId(id);
				manager.add(msg);
				msgs.add(msg);
			}, executor));
		}
		
		CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()])).join();
		assertEquals(count, expectedItems.size());
		assertTrue(expectedItems.containsAll(msgs));
		List<Long> items = Whitebox.getInternalState(manager, "buffer");
		assertTrue(items.isEmpty());
		List<Long> itemIds = Whitebox.getInternalState(manager, "itemIds");
		assertTrue(itemIds.isEmpty());
		PowerMockito.verifyStatic(SenderUtils.class);
		SenderUtils.sendBatch(eq(mockConnFactory), eq(SITE_ID), anyList(), eq(DEFAULT_LARGE_MSG_SIZE));
	}
	
	@Test
	public void add_shouldAddAndSendABatchOfItemsInAThreadSafeWay() {
		final int count = 67;
		ExecutorService executor = Executors.newFixedThreadPool(count);
		List<CompletableFuture<Void>> futures = new ArrayList(count);
		List<List<SenderSyncMessage>> expectedBatches = Collections.synchronizedList(new ArrayList<>());
		PowerMockito.doAnswer(i -> expectedBatches.add(new ArrayList<>(i.getArgument(2)))).when(SenderUtils.class);
		SenderUtils.sendBatch(eq(mockConnFactory), eq(SITE_ID), ArgumentMatchers.anyList(), eq(DEFAULT_LARGE_MSG_SIZE));
		for (long i = 0; i < count; i++) {
			final Long id = i + 1;
			futures.add(CompletableFuture.runAsync(() -> {
				SenderSyncMessage msg = new SenderSyncMessage();
				msg.setId(id);
				manager.add(msg);
			}, executor));
		}
		
		CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()])).join();
		assertEquals(6, expectedBatches.size());
		expectedBatches.forEach(l -> assertEquals(manager.getBatchSize(), l.size()));
		List<Long> items = Whitebox.getInternalState(manager, "buffer");
		assertEquals(7, items.size());
		List<Long> itemIds = Whitebox.getInternalState(manager, "itemIds");
		assertEquals(7, itemIds.size());
		PowerMockito.verifyStatic(SenderUtils.class, Mockito.times(6));
		SenderUtils.sendBatch(eq(mockConnFactory), eq(SITE_ID), anyList(), eq(DEFAULT_LARGE_MSG_SIZE));
	}
	
	@Test
	public void send_shouldSkipIfTheItemsBufferIsEmpty() {
		manager.send(false);
		PowerMockito.verifyZeroInteractions(SenderUtils.class);
	}
	
	@Test
	public void send_shouldSkipIfBufferSizeIsLessThanBatchSize() {
		Whitebox.setInternalState(manager, "buffer", List.of(new SenderSyncMessage()));
		
		manager.send(false);
		
		PowerMockito.verifyZeroInteractions(SenderUtils.class);
	}
	
	@Test
	public void send_shouldSendItemsInTheBufferToTheMessageBrokerAndReset() {
		final Long id1 = 1L;
		final Long id2 = 2L;
		List<SenderSyncMessage> items = new ArrayList<>();
		SenderSyncMessage msg1 = new SenderSyncMessage();
		msg1.setId(id1);
		SenderSyncMessage msg2 = new SenderSyncMessage();
		msg2.setId(id2);
		items.add(msg1);
		items.add(msg2);
		List<Long> ids = new ArrayList<>();
		ids.add(id1);
		ids.add(id2);
		manager = new MockSyncBatchManager(items.size());
		Whitebox.setInternalState(manager, "buffer", items);
		Whitebox.setInternalState(manager, "itemIds", ids);
		Whitebox.setInternalState(manager, "largeMsgSize", DEFAULT_LARGE_MSG_SIZE);
		Whitebox.setInternalState(manager, "siteId", SITE_ID);
		
		manager.send(false);
		
		PowerMockito.verifyStatic(SenderUtils.class);
		SenderUtils.sendBatch(mockConnFactory, SITE_ID, items, DEFAULT_LARGE_MSG_SIZE);
		List<Long> buffer = Whitebox.getInternalState(manager, "buffer");
		assertTrue(buffer.isEmpty());
		List<Long> itemIds = Whitebox.getInternalState(manager, "itemIds");
		assertTrue(itemIds.isEmpty());
		assertEquals(List.of(id1, id2), manager.updatedItemIds);
	}
	
	@Test
	public void send_shouldSendItemsOfBufferSizeIsLessThanBatchSizeAndForceIsSetToTrue() {
		final Long id1 = 1L;
		final Long id2 = 2L;
		List<SenderSyncMessage> items = new ArrayList<>();
		SenderSyncMessage msg1 = new SenderSyncMessage();
		msg1.setId(id1);
		SenderSyncMessage msg2 = new SenderSyncMessage();
		msg2.setId(id2);
		items.add(msg1);
		items.add(msg2);
		List<Long> ids = new ArrayList<>();
		ids.add(id1);
		ids.add(id2);
		manager = new MockSyncBatchManager(items.size() + 1);
		Whitebox.setInternalState(manager, "buffer", items);
		Whitebox.setInternalState(manager, "itemIds", ids);
		Whitebox.setInternalState(manager, "largeMsgSize", DEFAULT_LARGE_MSG_SIZE);
		Whitebox.setInternalState(manager, "siteId", SITE_ID);
		
		manager.send(true);
		
		PowerMockito.verifyStatic(SenderUtils.class);
		SenderUtils.sendBatch(mockConnFactory, SITE_ID, items, DEFAULT_LARGE_MSG_SIZE);
		List<Long> buffer = Whitebox.getInternalState(manager, "buffer");
		assertTrue(buffer.isEmpty());
		List<Long> itemIds = Whitebox.getInternalState(manager, "itemIds");
		assertTrue(itemIds.isEmpty());
		assertEquals(List.of(id1, id2), manager.updatedItemIds);
	}
	
}
