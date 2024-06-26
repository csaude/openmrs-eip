package org.openmrs.eip.app.receiver.processor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openmrs.eip.app.AppUtils;
import org.openmrs.eip.app.BaseQueueProcessor;
import org.openmrs.eip.app.management.entity.receiver.ReceiverRetryQueueItem;
import org.openmrs.eip.app.management.entity.receiver.SiteInfo;
import org.openmrs.eip.app.management.repository.ReceiverRetryRepository;
import org.openmrs.eip.app.management.service.ConflictService;
import org.openmrs.eip.app.management.service.ReceiverService;
import org.openmrs.eip.app.receiver.RetryCacheEvictingProcessor;
import org.openmrs.eip.app.receiver.RetrySearchIndexUpdatingProcessor;
import org.openmrs.eip.app.receiver.SyncHelper;
import org.openmrs.eip.app.receiver.task.ReceiverRetryTask;
import org.openmrs.eip.component.SyncContext;
import org.openmrs.eip.component.exception.EIPException;
import org.openmrs.eip.component.model.PersonModel;
import org.openmrs.eip.component.model.VisitModel;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(SyncContext.class)
public class ReceiverRetryProcessorTest {
	
	private static final ThreadPoolExecutor EXECUTOR = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
	
	@Mock
	private SyncHelper mockHelper;
	
	@Mock
	private ReceiverService mockService;
	
	@Mock
	private ConflictService mockConflictService;
	
	@Mock
	private ReceiverRetryRepository mockRetryRepo;
	
	@Mock
	private RetryCacheEvictingProcessor mockEvictProcessor;
	
	@Mock
	private RetrySearchIndexUpdatingProcessor mockIndexProcessor;
	
	@Mock
	private ReceiverRetryTask mockTask;
	
	private ReceiverRetryProcessor processor;
	
	@Before
	public void setup() {
		PowerMockito.mockStatic(SyncContext.class);
		Mockito.when(SyncContext.getBean(ReceiverRetryTask.class)).thenReturn(mockTask);
		setInternalState(BaseQueueProcessor.class, "initialized", true);
		processor = new ReceiverRetryProcessor(EXECUTOR, mockService, mockConflictService, mockHelper, mockRetryRepo,
		        mockEvictProcessor, mockIndexProcessor);
	}
	
	@After
	public void tearDown() {
		setInternalState(BaseQueueProcessor.class, "initialized", false);
	}
	
	@Test
	public void getUniqueId_shouldReturnDatabaseId() {
		final String uuid = "uuid";
		ReceiverRetryQueueItem retry = new ReceiverRetryQueueItem();
		retry.setIdentifier(uuid);
		assertEquals(uuid, processor.getUniqueId(retry));
	}
	
	@Test
	public void getThreadName_shouldReturnThreadName() {
		final String uuid = "uuid";
		final String messageUuid = "message-uuid";
		final String siteUuid = "site-uuid";
		ReceiverRetryQueueItem retry = new ReceiverRetryQueueItem();
		retry.setModelClassName(PersonModel.class.getName());
		retry.setIdentifier(uuid);
		retry.setMessageUuid(messageUuid);
		SiteInfo siteInfo = new SiteInfo();
		siteInfo.setIdentifier(siteUuid);
		retry.setSite(siteInfo);
		assertEquals(siteUuid + "-" + AppUtils.getSimpleName(retry.getModelClassName()) + "-" + uuid + "-" + messageUuid,
		    processor.getThreadName(retry));
	}
	
	@Test
	public void getLogicalType_shouldReturnTheModelClassName() {
		ReceiverRetryQueueItem msg = new ReceiverRetryQueueItem();
		msg.setModelClassName(VisitModel.class.getName());
		assertEquals(VisitModel.class.getName(), processor.getLogicalType(msg));
	}
	
	@Test
	public void getSyncPayload_shouldReturnPayload() {
		final String payload = "{}";
		ReceiverRetryQueueItem retry = new ReceiverRetryQueueItem();
		retry.setEntityPayload(payload);
		assertEquals(payload, processor.getSyncPayload(retry));
	}
	
	@Test
	public void beforeSync_shouldSyncIncrementAttemptCount() {
		final int attemptCount = 1;
		ReceiverRetryQueueItem retry = new ReceiverRetryQueueItem();
		retry.setAttemptCount(attemptCount);
		
		processor.beforeSync(retry);
		
		assertEquals(attemptCount + 1, retry.getAttemptCount().intValue());
	}
	
	@Test
	public void beforeSync_shouldFailIfThereIsAnEarlierRetryForTheEntity() {
		final String uuid = "uuid";
		final String modelClass = VisitModel.class.getName();
		ReceiverRetryQueueItem retry = new ReceiverRetryQueueItem();
		retry.setIdentifier(uuid);
		retry.setModelClassName(modelClass);
		Set<String> failures = Set.of(modelClass + "#" + uuid);
		when(mockTask.getFailedEntities()).thenReturn(failures);
		
		Exception ex = Assert.assertThrows(EIPException.class, () -> processor.beforeSync(retry));
		
		assertEquals("Skipped because the entity still has earlier failed event(s) in the queue", ex.getMessage());
	}
	
	@Test
	public void beforeSync_shouldFailIfTheEntityHasAConflict() {
		final String uuid = "uuid";
		final String modelClass = VisitModel.class.getName();
		ReceiverRetryQueueItem retry = new ReceiverRetryQueueItem();
		retry.setIdentifier(uuid);
		retry.setModelClassName(modelClass);
		when(mockConflictService.hasConflictItem(uuid, modelClass)).thenReturn(true);
		
		Exception ex = Assert.assertThrows(EIPException.class, () -> processor.beforeSync(retry));
		
		assertEquals("Cannot process the message because the entity has a conflict item in the queue", ex.getMessage());
	}
	
	@Test
	public void afterSync_shouldPostProcessTheRetryItem() {
		ReceiverRetryQueueItem retry = new ReceiverRetryQueueItem();
		
		processor.afterSync(retry);
		
		verify(mockEvictProcessor).process(retry);
		verify(mockIndexProcessor).process(retry);
		verify(mockService).archiveRetry(retry);
		verify(mockTask).postProcess(retry, false);
	}
	
	@Test
	public void onConflict_shouldAddTheItemToTheConflictQueueIfAConflictIsDetected() {
		ReceiverRetryQueueItem retry = new ReceiverRetryQueueItem();
		
		processor.onConflict(retry);
		
		verify(mockService).moveToConflictQueue(retry);
		verify(mockTask).postProcess(retry, false);
	}
	
	@Test
	public void onError_shouldUpdateAndSaveTheRetry() {
		final String errorMsg = "test";
		ReceiverRetryQueueItem retry = new ReceiverRetryQueueItem();
		Assert.assertNull(retry.getExceptionType());
		Assert.assertNull(retry.getMessage());
		Assert.assertNull(retry.getDateChanged());
		Exception ex = new EIPException(errorMsg);
		long timestamp = System.currentTimeMillis();
		
		processor.onError(retry, ex.getClass().getName(), ex.getMessage());
		
		assertEquals(ex.getClass().getName(), retry.getExceptionType());
		assertEquals(errorMsg, retry.getMessage());
		assertTrue(retry.getDateChanged().getTime() == timestamp || retry.getDateChanged().getTime() > timestamp);
		verify(mockRetryRepo).save(retry);
		verify(mockTask).postProcess(retry, true);
	}
	
}
