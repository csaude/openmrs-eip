package org.openmrs.eip.app;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.reflect.MethodUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.openmrs.eip.app.sender.OffsetUtils;
import org.openmrs.eip.component.exception.EIPException;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ OffsetUtils.class, AppUtils.class })
public class CustomFileOffsetBackingStoreTest {
	
	private CustomFileOffsetBackingStore store;
	
	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		store = Mockito.spy(new CustomFileOffsetBackingStore());
		Whitebox.setInternalState(CustomFileOffsetBackingStore.class, "paused", false);
		Whitebox.setInternalState(CustomFileOffsetBackingStore.class, "disabled", false);
	}
	
	@Test
	public void save_shouldNotSaveOffsetsIfTheStoreIsPaused() {
		Whitebox.setInternalState(CustomFileOffsetBackingStore.class, "paused", true);
		
		store.save();
		
		Mockito.verify(store, Mockito.never()).doSave();
	}
	
	@Test
	public void save_shouldNotSaveOffsetsIfTheStoreIsDisabled() {
		Whitebox.setInternalState(CustomFileOffsetBackingStore.class, "paused", true);
		
		store.save();
		
		Mockito.verify(store, Mockito.never()).doSave();
	}
	
	@Test
	public void save_shouldSaveOffsetsIfTheStoreIsNotPausedAndIsNotDisabled() throws Exception {
		Mockito.doNothing().when(store).doSave();
		
		store.save();
		
		Mockito.verify(store).doSave();
	}
	
	@Test
	public void start_shouldTransformAndVerifyTheExistingOffset() throws Exception {
		Mockito.doNothing().when(store).doStart();
		Map mockData = new HashMap();
		PowerMockito.mockStatic(OffsetUtils.class);
		Whitebox.setInternalState(store, Map.class, mockData);
		
		store.start();
		
		Mockito.verify(store).doStart();
		PowerMockito.verifyStatic(OffsetUtils.class);
		OffsetUtils.transformOffsetIfNecessary(mockData);
		PowerMockito.verifyStatic(OffsetUtils.class);
		OffsetUtils.verifyOffsetAndResetIfInvalid(mockData);
	}
	
	@Test
	public void start_shouldFailIfAnExceptionIsThrownWhenVerifyingTheOffset() throws Exception {
		Mockito.doNothing().when(store).doStart();
		Map mockData = new HashMap();
		PowerMockito.mockStatic(OffsetUtils.class);
		PowerMockito.mockStatic(AppUtils.class);
		Whitebox.setInternalState(store, Map.class, mockData);
		Method method = MethodUtils.getAccessibleMethod(OffsetUtils.class, "verifyOffsetAndResetIfInvalid", Map.class);
		PowerMockito.when(OffsetUtils.class, method).withArguments(mockData).thenThrow(new EIPException("test"));
		
		store.start();
		
		Mockito.verify(store).doStart();
		PowerMockito.verifyStatic(OffsetUtils.class);
		OffsetUtils.verifyOffsetAndResetIfInvalid(mockData);
		PowerMockito.verifyStatic(AppUtils.class);
		AppUtils.shutdown(false);
	}
	
}
