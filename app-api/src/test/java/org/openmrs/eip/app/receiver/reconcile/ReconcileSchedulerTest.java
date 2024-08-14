package org.openmrs.eip.app.receiver.reconcile;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openmrs.eip.app.management.service.ReceiverReconcileService;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class ReconcileSchedulerTest {
	
	@Mock
	private ReceiverReconcileService mockService;
	
	private ReconcileScheduler scheduler;
	
	@Before
	public void setup() {
		scheduler = new ReconcileScheduler(mockService);
	}
	
	@Test
	public void execute_shouldAddAReconciliation() {
		scheduler.execute();
		Mockito.verify(mockService).addNewReconciliation();
	}
	
}
