package org.openmrs.eip.app.receiver.reconcile;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openmrs.eip.app.management.service.ReceiverReconcileService;
import org.openmrs.eip.app.receiver.task.FullIndexer;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class ReceiverSchedulerTest {
	
	@Mock
	private ReceiverReconcileService mockService;
	
	@Mock
	private FullIndexer mockIndexer;
	
	private ReceiverScheduler scheduler;
	
	@Before
	public void setup() {
		scheduler = new ReceiverScheduler(mockService, mockIndexer);
	}
	
	@Test
	public void reconcile_shouldAddAReconciliation() {
		scheduler.reconcile();
		Mockito.verify(mockService).addNewReconciliation();
	}
	
	@Test
	public void index_shouldAddAReconciliation() {
		scheduler.index();
		Mockito.verify(mockIndexer).start();
	}
	
}
