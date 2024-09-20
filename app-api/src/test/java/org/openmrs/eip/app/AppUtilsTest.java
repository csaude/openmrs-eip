package org.openmrs.eip.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;
import static org.openmrs.eip.app.receiver.ConflictResolution.ResolutionDecision.MERGE;
import static org.openmrs.eip.app.receiver.ReceiverConstants.DEFAULT_TASK_BATCH_SIZE;
import static org.openmrs.eip.app.receiver.ReceiverConstants.PROP_SYNC_TASK_BATCH_SIZE;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openmrs.eip.app.management.entity.receiver.ConflictQueueItem;
import org.openmrs.eip.app.management.entity.sender.SenderSyncMessage;
import org.openmrs.eip.app.management.service.impl.ConflictServiceImpl;
import org.openmrs.eip.app.receiver.ConflictResolution;
import org.openmrs.eip.app.receiver.SyncHelper;
import org.openmrs.eip.app.receiver.processor.ConflictMessageProcessor;
import org.openmrs.eip.component.SyncContext;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Pageable;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ SyncContext.class, ConflictServiceImpl.class })
public class AppUtilsTest {
	
	private static final String TEST_VERSION = "1.0 Caramel";
	
	private static final String TEST_BUILD_NO = "123456";
	
	@Mock
	private Environment mockEnv;
	
	@Before
	public void setup() {
		PowerMockito.mockStatic(SyncContext.class);
		when(SyncContext.getBean(Environment.class)).thenReturn(mockEnv);
		setInternalState(BaseQueueProcessor.class, "initialized", true);
	}
	
	@After
	public void tearDown() {
		Whitebox.setInternalState(AppUtils.class, "props", (Object) null);
		setInternalState(BaseQueueProcessor.class, "initialized", false);
	}
	
	@Test
	public void getVersion_shouldReturnTheVersion() {
		Properties props = new Properties();
		props.setProperty(SyncConstants.DBSYNC_PROP_VERSION, TEST_VERSION);
		Whitebox.setInternalState(AppUtils.class, "props", props);
		assertEquals(TEST_VERSION, AppUtils.getVersion());
	}
	
	@Test
	public void getBuild_shouldReturnTheBuildNumber() {
		Properties props = new Properties();
		props.setProperty(SyncConstants.DBSYNC_PROP_BUILD_NUMBER, TEST_BUILD_NO);
		Whitebox.setInternalState(AppUtils.class, "props", props);
		assertEquals(TEST_BUILD_NO, AppUtils.getBuildNumber());
	}
	
	@Test
	public void getTaskPage_shouldGetThePageableObjectUsingConfiguredBatchSize() {
		assertNull(Whitebox.getInternalState(AppUtils.class, "taskPage"));
		final int size = 5;
		when(mockEnv.getProperty(PROP_SYNC_TASK_BATCH_SIZE, Integer.class, DEFAULT_TASK_BATCH_SIZE)).thenReturn(size);
		
		Pageable pageable = AppUtils.getTaskPage();
		
		assertEquals(0, pageable.getPageNumber());
		assertEquals(size, pageable.getPageSize());
	}
	
	@Test
	public void getFieldValue_shouldGetTheFieldValue() throws Exception {
		Field field = SenderSyncMessage.class.getDeclaredField("tableName");
		assertFalse(field.isAccessible());
		final String tableName = "person";
		SenderSyncMessage msg = new SenderSyncMessage();
		msg.setTableName(tableName);
		assertEquals(tableName, AppUtils.getFieldValue(msg, field));
		assertFalse(field.isAccessible());
	}
	
	@Test
	public void setFieldValue_shouldSetTheFieldValue() throws Exception {
		Field field = SenderSyncMessage.class.getDeclaredField("tableName");
		assertFalse(field.isAccessible());
		final String tableName = "person";
		SenderSyncMessage msg = new SenderSyncMessage();
		assertNull(msg.getTableName());
		AppUtils.setFieldValue(msg, field, tableName);
		assertEquals(tableName, msg.getTableName());
		assertFalse(field.isAccessible());
	}
	
	@Test
	public void invokeMethod_shouldInvokeTheMethod() throws Exception {
		SyncHelper mockSyncHelper = Mockito.mock(SyncHelper.class);
		Method method = ConflictServiceImpl.class.getDeclaredMethod("resolveWithMerge", ConflictResolution.class);
		assertFalse(method.isAccessible());
		ConflictServiceImpl service = new ConflictServiceImpl(null, null, null, null, null, null, null, mockSyncHelper);
		Constructor<ConflictMessageProcessor> constructor = ConstructorUtils
		        .getAccessibleConstructor(ConflictMessageProcessor.class, SyncHelper.class, Set.class);
		ConflictMessageProcessor mockProcessor = Mockito.mock(ConflictMessageProcessor.class);
		ConflictQueueItem conflict = new ConflictQueueItem();
		ConflictResolution resolution = new ConflictResolution(conflict, MERGE);
		Set<String> props = Set.of("prop1");
		resolution.addPropertyToSync("prop1");
		PowerMockito.whenNew(constructor).withArguments(mockSyncHelper, props).thenReturn(mockProcessor);
		
		AppUtils.invokeMethod(service, method, resolution);
		
		Mockito.verify(mockProcessor).processItem(conflict);
	}
	
}
