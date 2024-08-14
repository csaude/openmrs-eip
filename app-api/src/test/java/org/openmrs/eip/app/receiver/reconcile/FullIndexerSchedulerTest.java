package org.openmrs.eip.app.receiver.reconcile;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openmrs.eip.app.receiver.task.FullIndexer;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class FullIndexerSchedulerTest {
	
	@Mock
	private FullIndexer mockIndexer;
	
	private FullIndexerScheduler scheduler;
	
	@Before
	public void setup() {
		scheduler = new FullIndexerScheduler(mockIndexer);
	}
	
	@Test
	public void execute_shouldAddAReconciliation() {
		scheduler.execute();
		Mockito.verify(mockIndexer).start();
	}
	
}
