package org.openmrs.eip.app.sender.reconcile;

import static org.powermock.reflect.Whitebox.setInternalState;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openmrs.eip.app.AppUtils;
import org.openmrs.eip.app.management.entity.sender.SenderReconcileMessage;
import org.openmrs.eip.app.management.repository.SenderReconcileMsgRepository;
import org.openmrs.eip.component.SyncContext;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.data.domain.Pageable;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ SyncContext.class, AppUtils.class })
public class SenderReconcileMsgTaskTest {
	
	@Mock
	private SenderReconcileMsgRepository mockRepo;
	
	@Mock
	private Pageable mockPage;
	
	private SenderReconcileMsgTask task;
	
	@Before
	public void setup() {
		PowerMockito.mockStatic(SyncContext.class);
		PowerMockito.mockStatic(AppUtils.class);
		Mockito.when(AppUtils.getTaskPage()).thenReturn(mockPage);
		task = new SenderReconcileMsgTask();
	}
	
	@Test
	public void getNextBatch_shouldReadTheNextPageOfArchivesToBePruned() {
		List<SenderReconcileMessage> expectedMsgs = List.of(new SenderReconcileMessage(), new SenderReconcileMessage());
		setInternalState(task, SenderReconcileMsgRepository.class, mockRepo);
		Mockito.when(mockRepo.getBatch(mockPage)).thenReturn(expectedMsgs);
		Assert.assertEquals(expectedMsgs, task.getNextBatch());
	}
	
}
