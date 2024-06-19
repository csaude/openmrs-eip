package org.openmrs.eip.app.receiver;

import static org.openmrs.eip.component.model.SyncModel.builder;
import static org.openmrs.eip.component.utils.JsonUtils.marshall;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openmrs.eip.app.management.entity.receiver.JmsMessage;
import org.openmrs.eip.app.management.repository.JmsMessageRepository;
import org.openmrs.eip.app.management.service.ReceiverService;
import org.openmrs.eip.component.model.PersonModel;
import org.openmrs.eip.component.model.SyncMetadata;
import org.powermock.modules.junit4.PowerMockRunner;

import jakarta.jms.Message;

@RunWith(PowerMockRunner.class)
public class ReceiverMessageListenerMockTest {
	
	@Mock
	private JmsMessageRepository mockRepo;
	
	@Mock
	private ReceiverService mockService;
	
	@Mock
	public ReceiverMessageListener listener;
	
	@Before
	public void setup() {
		listener = new ReceiverMessageListener(mockRepo, mockService);
	}
	
	@Test
	public void onMessage_shouldSkipCheckingForDuplicateMessages() throws Exception {
		SyncMetadata md = new SyncMetadata();
		md.setMessageUuid("msg-uuid");
		final String body = marshall(builder().tableToSyncModelClass(PersonModel.class).metadata(md).build());
		Message msg = Mockito.mock(Message.class);
		Mockito.when(msg.getBody(byte[].class)).thenReturn(body.getBytes());
		
		listener.onMessage(msg);
		
		Mockito.verify(mockService).saveJmsMessage(ArgumentMatchers.any(JmsMessage.class));
		Mockito.verifyNoInteractions(mockRepo);
	}
	
}
