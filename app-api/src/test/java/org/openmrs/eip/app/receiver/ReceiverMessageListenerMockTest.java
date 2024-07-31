package org.openmrs.eip.app.receiver;

import static org.mockito.Mockito.when;
import static org.openmrs.eip.component.model.SyncModel.builder;
import static org.openmrs.eip.component.utils.JsonUtils.marshall;

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
import org.openmrs.eip.component.model.PersonModel;
import org.openmrs.eip.component.model.SyncMetadata;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import jakarta.jms.BytesMessage;
import jakarta.jms.Message;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ReceiverMessageListenerContainer.class)
public class ReceiverMessageListenerMockTest {
	
	@Mock
	private JmsMessageRepository mockRepo;
	
	@Mock
	private ReceiverService mockService;
	
	@Mock
	private SyncStatusProcessor mockProcessor;
	
	@Mock
	public ReceiverMessageListener listener;
	
	@Before
	public void setup() {
		PowerMockito.mockStatic(ReceiverMessageListenerContainer.class);
		listener = new ReceiverMessageListener(mockRepo, mockService, mockProcessor);
	}
	
	@Test
	public void onMessage_shouldSkipCheckingForDuplicateMessages() throws Exception {
		SyncMetadata md = new SyncMetadata();
		md.setMessageUuid("msg-uuid");
		final String body = marshall(builder().tableToSyncModelClass(PersonModel.class).metadata(md).build());
		Message msg = Mockito.mock(BytesMessage.class);
		when(msg.getStringProperty(SyncConstants.JMS_HEADER_SITE)).thenReturn("testId");
		when(msg.getBody(byte[].class)).thenReturn(body.getBytes());
		
		listener.onMessage(msg);
		
		Mockito.verify(mockService).saveJmsMessage(ArgumentMatchers.any(JmsMessage.class));
		PowerMockito.verifyStatic(ReceiverMessageListenerContainer.class);
		ReceiverMessageListenerContainer.enableAcknowledgement();
		Mockito.verifyNoInteractions(mockRepo);
	}
	
	@Test
	public void onMessage_shouldSkipDuplicateMessagesAndSendAcknowledgement() throws Exception {
		final String msgId = "test-id";
		SyncMetadata md = new SyncMetadata();
		md.setMessageUuid(msgId);
		final String body = marshall(builder().tableToSyncModelClass(PersonModel.class).metadata(md).build());
		Message msg = Mockito.mock(BytesMessage.class);
		when(msg.getBody(byte[].class)).thenReturn(body.getBytes());
		when(msg.getStringProperty(SyncConstants.JMS_HEADER_MSG_ID)).thenReturn(msgId);
		when(mockRepo.existsByMessageId(msgId)).thenReturn(true);
		Whitebox.setInternalState(listener, "skipDuplicates", true);
		
		listener.onMessage(msg);
		
		Mockito.verifyNoInteractions(mockService);
		Mockito.verifyNoInteractions(mockProcessor);
		PowerMockito.verifyStatic(ReceiverMessageListenerContainer.class);
		ReceiverMessageListenerContainer.enableAcknowledgement();
	}
	
}
