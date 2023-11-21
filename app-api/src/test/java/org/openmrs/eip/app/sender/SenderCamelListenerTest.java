package org.openmrs.eip.app.sender;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.reflect.Whitebox.getInternalState;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.io.File;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.camel.impl.event.CamelContextStartedEvent;
import org.apache.camel.impl.event.CamelContextStartingEvent;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openmrs.eip.app.AppUtils;
import org.openmrs.eip.component.SyncContext;
import org.openmrs.eip.component.utils.FileUtils;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.core.env.Environment;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ AppUtils.class, SyncContext.class, FileUtils.class })
public class SenderCamelListenerTest {
	
	@Mock
	private ScheduledExecutorService mockExecutor;
	
	@Mock
	private File mockDbzmOffsetFile;
	
	@Mock
	private Environment mockEnv;
	
	private SenderCamelListener listener;
	
	@Before
	public void setup() {
		PowerMockito.mockStatic(AppUtils.class);
		PowerMockito.mockStatic(SyncContext.class);
		PowerMockito.mockStatic(FileUtils.class);
		listener = new SenderCamelListener(mockExecutor);
	}
	
	@Test
	public void notify_shouldNotStartTheBinlogTaskIfNotEnabled() throws Exception {
		listener.notify(new CamelContextStartedEvent(null));
		
		Mockito.verifyNoInteractions(mockExecutor);
	}
	
	@Test
	public void notify_shouldStartTheBinlogTaskIfEnabledForStartedEvent() throws Exception {
		int maxKeepCount = 5;
		long initialDelay = 2000;
		long delay = 1000;
		setInternalState(listener, "binlogPurgerEnabled", true);
		final String offsetFile = "someFilename";
		when(SyncContext.getBean(Environment.class)).thenReturn(mockEnv);
		when(mockEnv.getProperty(SenderConstants.PROP_DBZM_OFFSET_FILENAME)).thenReturn(offsetFile);
		when(FileUtils.instantiateFile(offsetFile)).thenReturn(mockDbzmOffsetFile);
		setInternalState(listener, "binlogMaxKeepCount", maxKeepCount);
		setInternalState(listener, "initialDelayBinlogPurger", initialDelay);
		setInternalState(listener, "delayBinlogPurger", delay);
		
		listener.notify(new CamelContextStartingEvent(null));
		
		ArgumentCaptor<BinlogPurgingTask> taskCaptor = ArgumentCaptor.forClass(BinlogPurgingTask.class);
		verify(mockExecutor).scheduleWithFixedDelay(taskCaptor.capture(), eq(initialDelay), eq(delay), eq(MILLISECONDS));
		BinlogPurgingTask task = taskCaptor.getValue();
		Assert.assertEquals(mockDbzmOffsetFile, getInternalState(task, "debeziumOffsetFile"));
		Assert.assertEquals(maxKeepCount, (int) getInternalState(task, "maxKeepCount"));
	}
	
	@Test
	public void notify_shouldStartThePrunerTaskIfEnabledForStartedEvent() throws Exception {
		int maxAgeInDays = 1;
		long initialDelay = 2000;
		long delay = 1000;
		setInternalState(listener, "prunerEnabled", true);
		setInternalState(listener, "archivesMaxAgeInDays", maxAgeInDays);
		setInternalState(listener, "initialDelayPruner", initialDelay);
		setInternalState(listener, "delayPruner", delay);
		
		listener.notify(new CamelContextStartingEvent(null));
		
		ArgumentCaptor<SenderArchivePruningTask> taskCaptor = ArgumentCaptor.forClass(SenderArchivePruningTask.class);
		verify(mockExecutor).scheduleWithFixedDelay(taskCaptor.capture(), eq(initialDelay), eq(delay), eq(MILLISECONDS));
		
		SenderArchivePruningTask task = taskCaptor.getValue();
		Assert.assertEquals(maxAgeInDays, (int) getInternalState(task, "maxAgeDays"));
	}
	
	@Test
	public void notify_shouldStopTheExecutorForStoppingEvent() throws Exception {
		listener.notify(new CamelContextStartingEvent(null));
		
		PowerMockito.verifyStatic(AppUtils.class);
		AppUtils.shutdownExecutor(mockExecutor, "scheduled", false);
	}
	
}
