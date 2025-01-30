package org.openmrs.eip.app.receiver;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

import javax.sql.DataSource;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openmrs.eip.app.AppUtils;
import org.openmrs.eip.app.SyncConstants;
import org.openmrs.eip.app.management.entity.receiver.JmsMessage;
import org.openmrs.eip.app.management.entity.receiver.JmsMessage.MessageType;
import org.openmrs.eip.app.management.repository.JmsMessageRepository;
import org.openmrs.eip.component.SyncContext;
import org.openmrs.eip.component.SyncOperation;
import org.openmrs.eip.component.model.SyncMetadata;
import org.openmrs.eip.component.model.SyncModel;
import org.openmrs.eip.component.utils.JsonUtils;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.data.domain.Pageable;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ SyncContext.class, AppUtils.class })
public class ReceiverSyncJmsMessageTaskMockTest {
	
	@Mock
	private JmsMessageRepository mockRepo;
	
	@Mock
	private ReceiverJmsMessageProcessor mockProcessor;
	
	@Mock
	private Pageable mockPage;
	
	@Mock
	private DataSource mockDatasource;
	
	@Mock
	private Connection mockConnection;
	
	@Mock
	private PreparedStatement mockInsertStatement;
	
	@Mock
	private Statement mockDeleteStatement;
	
	private ReceiverSyncJmsMessageTask task;
	
	@Before
	public void setup() throws Exception {
		PowerMockito.mockStatic(SyncContext.class);
		PowerMockito.mockStatic(AppUtils.class);
		when(SyncContext.getBean(ReceiverJmsMessageProcessor.class)).thenReturn(mockProcessor);
		when(SyncContext.getBean(SyncConstants.MGT_DATASOURCE_NAME)).thenReturn(mockDatasource);
		when(mockDatasource.getConnection()).thenReturn(mockConnection);
		when(mockConnection.prepareStatement(ReceiverSyncJmsMessageTask.SYNC_INSERT)).thenReturn(mockInsertStatement);
		when(mockConnection.createStatement()).thenReturn(mockDeleteStatement);
	}
	
	private JmsMessage createMessage(String operation) {
		JmsMessage msg = new JmsMessage();
		SyncMetadata md = new SyncMetadata();
		md.setOperation(operation);
		SyncModel model = SyncModel.builder().metadata(md).build();
		msg.setBody(JsonUtils.marshalToBytes(model));
		return msg;
	}
	
	@Test
	public void getNextBatch_shouldFetchTheNextBatchOfMessage() {
		when(AppUtils.getTaskPage()).thenReturn(mockPage);
		task = new ReceiverSyncJmsMessageTask();
		setInternalState(task, JmsMessageRepository.class, mockRepo);
		List<JmsMessage> expected = List.of(new JmsMessage(), new JmsMessage(), new JmsMessage());
		when(mockRepo.findByType(MessageType.SYNC, mockPage)).thenReturn(expected);
		
		List<JmsMessage> actual = task.getNextBatch();
		
		Assert.assertEquals(expected, actual);
	}
	
	@Test
	public void process_shouldProcessMessagesIndividuallyIfTheMessageListContainsASyncRequest() throws Exception {
		List<JmsMessage> msgs = List.of(createMessage(SyncOperation.r.name()), createMessage(SyncOperation.c.name()));
		task = new ReceiverSyncJmsMessageTask();
		when(mockConnection.getAutoCommit()).thenReturn(true);
		
		task.process(msgs);
		
		Mockito.verify(mockProcessor).processWork(msgs);
		Mockito.verify(mockInsertStatement, never()).executeBatch();
		Mockito.verify(mockDeleteStatement, never()).executeUpdate(anyString());
		Mockito.verify(mockConnection).setAutoCommit(false);
		Mockito.verify(mockConnection).setAutoCommit(true);
	}
	
}
