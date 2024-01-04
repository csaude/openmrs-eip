package org.openmrs.eip.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.openmrs.eip.app.receiver.ReceiverConstants.DEFAULT_TASK_BATCH_SIZE;
import static org.openmrs.eip.app.receiver.ReceiverConstants.PROP_SYNC_TASK_BATCH_SIZE;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openmrs.eip.app.management.entity.sender.Event;
import org.openmrs.eip.app.management.service.ConflictService;
import org.openmrs.eip.app.management.service.impl.ConflictServiceImpl;
import org.openmrs.eip.app.receiver.ConflictResolution;
import org.openmrs.eip.component.SyncContext;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Pageable;

@RunWith(PowerMockRunner.class)
@PrepareForTest(SyncContext.class)
public class AppUtilsTest {
	
	private static final String TEST_VERSION = "1.0 Caramel";
	
	private static final String TEST_BUILD_NO = "123456";
	
	@Mock
	private Environment mockEnv;
	
	@Before
	public void setup() {
		PowerMockito.mockStatic(SyncContext.class);
		when(SyncContext.getBean(Environment.class)).thenReturn(mockEnv);
	}
	
	@After
	public void tearDown() {
		Whitebox.setInternalState(AppUtils.class, "props", (Object) null);
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
		Field field = Event.class.getDeclaredField("tableName");
		assertFalse(field.isAccessible());
		final String tableName = "person";
		Event event = new Event();
		event.setTableName(tableName);
		assertEquals(tableName, AppUtils.getFieldValue(event, field));
		assertFalse(field.isAccessible());
	}
	
	@Test
	public void setFieldValue_shouldSetTheFieldValue() throws Exception {
		Field field = Event.class.getDeclaredField("tableName");
		assertFalse(field.isAccessible());
		final String tableName = "person";
		Event event = new Event();
		assertNull(event.getTableName());
		AppUtils.setFieldValue(event, field, tableName);
		assertEquals(tableName, event.getTableName());
		assertFalse(field.isAccessible());
	}
	
	@Test
	public void invokeMethod_shouldInvokeTheMethod() throws Exception {
		Method method = ConflictServiceImpl.class.getDeclaredMethod("resolveWithMerge", ConflictResolution.class);
		assertFalse(method.isAccessible());
		ConflictServiceImpl service = new ConflictServiceImpl(null, null, null, null, null, null, null);
		ConflictResolution mockResolution = Mockito.mock(ConflictResolution.class);
		ConflictService mockService = Mockito.mock(ConflictService.class);
		when(SyncContext.getBean(ConflictService.class)).thenReturn(mockService);
		
		AppUtils.invokeMethod(service, method, mockResolution);
		
		Mockito.verify(mockService).resolveWithMerge(any(), any());
	}
	
}
