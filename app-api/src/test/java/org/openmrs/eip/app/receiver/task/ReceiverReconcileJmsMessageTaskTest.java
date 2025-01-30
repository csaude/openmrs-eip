package org.openmrs.eip.app.receiver.task;

import static org.mockito.Mockito.when;
import static org.openmrs.eip.app.management.entity.receiver.JmsMessage.MessageType.RECONCILE;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openmrs.eip.app.AppUtils;
import org.openmrs.eip.app.management.entity.receiver.JmsMessage;
import org.openmrs.eip.app.management.repository.JmsMessageRepository;
import org.openmrs.eip.component.SyncContext;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.data.domain.Pageable;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ SyncContext.class, AppUtils.class })
public class ReceiverReconcileJmsMessageTaskTest {
	
	@Mock
	private JmsMessageRepository mockRepo;
	
	@Mock
	private Pageable mockPage;
	
	private ReceiverReconcileJmsMessageTask task;
	
	@Before
	public void setup() {
		PowerMockito.mockStatic(SyncContext.class);
		PowerMockito.mockStatic(AppUtils.class);
		task = new ReceiverReconcileJmsMessageTask();
		setInternalState(task, JmsMessageRepository.class, mockRepo);
		Mockito.when(AppUtils.getTaskPage()).thenReturn(mockPage);
	}
	
	@Test
	public void getNextBatch_shouldFetchTheNextReconcileJmsMessages() {
		JmsMessage msg = new JmsMessage();
		List<JmsMessage> expected = List.of(msg);
		when(mockRepo.findByType(RECONCILE, mockPage)).thenReturn(expected);
		
		List<JmsMessage> jmsMessages = task.getNextBatch();
		
		Assert.assertEquals(expected, jmsMessages);
	}
	
}
