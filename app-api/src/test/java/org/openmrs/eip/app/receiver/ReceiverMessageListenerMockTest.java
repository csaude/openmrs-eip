package org.openmrs.eip.app.receiver;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openmrs.eip.app.SyncConstants;
import org.openmrs.eip.app.management.entity.receiver.JmsMessage;
import org.openmrs.eip.app.management.repository.JmsMessageRepository;
import org.openmrs.eip.app.management.service.ReceiverService;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import jakarta.jms.Message;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ReceiverMessageListenerContainer.class)
public class ReceiverMessageListenerMockTest {
	
	@Mock
	private JmsMessageRepository mockRepo;
	
	@Mock
	private ReceiverService mockService;
	
	@Mock
	public ReceiverMessageListener listener;
	
	@Before
	public void setup() {
		PowerMockito.mockStatic(ReceiverMessageListenerContainer.class);
		listener = new ReceiverMessageListener(mockRepo, mockService);
	}
	
	@Test
	public void onMessage_shouldSkipCheckingForDuplicateMessages() throws Exception {
		Message msg = Mockito.mock(Message.class);
		Mockito.when(msg.getBody(byte[].class)).thenReturn(new byte[] {});
		
		listener.onMessage(msg);
		
		Mockito.verify(mockService).saveJmsMessage(ArgumentMatchers.any(JmsMessage.class));
		Mockito.verifyNoInteractions(mockRepo);
	}
	
	@Test
	public void onMessage_shouldSkipDuplicateMessagesAndSendAcknowledgement() throws Exception {
		final String msgId = "test-id";
		Message msg = Mockito.mock(Message.class);
		Mockito.when(msg.getStringProperty(SyncConstants.JMS_HEADER_MSG_ID)).thenReturn(msgId);
		Mockito.when(mockRepo.existsByMessageId(msgId)).thenReturn(true);
		Whitebox.setInternalState(listener, "skipDuplicates", true);
		
		listener.onMessage(msg);
		
		PowerMockito.verifyStatic(ReceiverMessageListenerContainer.class);
		ReceiverMessageListenerContainer.enableAcknowledgement();
		Mockito.verifyNoInteractions(mockService);
	}
	
}
