package org.openmrs.eip.app;

import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(AppUtils.class)
public class BaseTaskTest {
	
	private MockBaseTask task;
	
	@Before
	public void setup() {
		PowerMockito.mockStatic(AppUtils.class);
		task = new MockBaseTask();
	}
	
	@Test
	public void run_shouldNotRunIfSkipReturnsTrue() throws Exception {
		task = Mockito.spy(task);
		when(task.skip()).thenReturn(true);
		
		task.run();
		
		Mockito.verify(task, Mockito.never()).doRun();
		Mockito.verify(task, Mockito.never()).beforeStart();
		Mockito.verify(task, Mockito.never()).beforeStop();
		Assert.assertFalse(task.doRunCalled);
	}
	
	@Test
	public void run_shouldNotRunIfAppIsStopping() throws Exception {
		task = Mockito.spy(task);
		when(AppUtils.isShuttingDown()).thenReturn(true);
		
		task.run();
		
		Mockito.verify(task, Mockito.never()).doRun();
		Mockito.verify(task, Mockito.never()).beforeStart();
		Mockito.verify(task, Mockito.never()).beforeStop();
		Assert.assertFalse(task.doRunCalled);
	}
	
}
