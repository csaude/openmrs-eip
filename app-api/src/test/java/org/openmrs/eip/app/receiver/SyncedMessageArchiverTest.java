package org.openmrs.eip.app.receiver;

import static org.mockito.Mockito.when;

import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openmrs.eip.app.AppUtils;
import org.openmrs.eip.app.management.entity.receiver.SiteInfo;
import org.openmrs.eip.app.management.entity.receiver.SyncedMessage;
import org.openmrs.eip.app.management.repository.SyncedMessageRepository;
import org.openmrs.eip.component.SyncContext;
import org.openmrs.eip.component.SyncOperation;
import org.openmrs.eip.component.model.PersonModel;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.springframework.data.domain.Pageable;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ SyncContext.class, AppUtils.class })
public class SyncedMessageArchiverTest {
	
	@Mock
	private SyncedMessageRepository mockRepo;
	
	@Mock
	private Pageable mockPage;
	
	@Mock
	private PreparedStatement mockStatement;
	
	private SyncedMessageArchiver archiver;
	
	@Before
	public void setup() {
		PowerMockito.mockStatic(SyncContext.class);
		PowerMockito.mockStatic(AppUtils.class);
		when(AppUtils.getTaskPage()).thenReturn(mockPage);
		archiver = new SyncedMessageArchiver();
		Whitebox.setInternalState(archiver, SyncedMessageRepository.class, mockRepo);
	}
	
	@Test
	public void getNextBatch_shouldLoadABatchOfSyncedMessages() {
		List<SyncedMessage> expected = List.of(new SyncedMessage(), new SyncedMessage());
		when(mockRepo.getBatchOfMessagesForArchiving(mockPage)).thenReturn(expected);
		Assert.assertEquals(expected, archiver.getNextBatch());
	}
	
	@Test
	public void addItem_shouldAddTheItemToThePreparedStatement() throws Exception {
		final String modelClass = PersonModel.class.getName();
		final Long siteId = 3L;
		final String uuid = "test-uuid";
		final String payload = "{}";
		final String msgUuid = "msg-uuid";
		final LocalDateTime dateSent = LocalDateTime.now();
		final Date dateReceived = new Date();
		final String version = "1.0";
		SyncedMessage msg = new SyncedMessage();
		msg.setDateCreated(new Date());
		msg.setModelClassName(modelClass);
		msg.setIdentifier(uuid);
		msg.setEntityPayload(payload);
		SiteInfo site = new SiteInfo();
		site.setId(siteId);
		msg.setSite(site);
		msg.setSnapshot(true);
		msg.setMessageUuid(msgUuid);
		msg.setDateSentBySender(dateSent);
		msg.setDateReceived(dateReceived);
		msg.setOperation(SyncOperation.u);
		
		archiver.addItem(mockStatement, msg);
		
		Mockito.verify(mockStatement).setString(1, modelClass);
		Mockito.verify(mockStatement).setString(2, uuid);
		Mockito.verify(mockStatement).setString(3, payload);
		Mockito.verify(mockStatement).setLong(4, siteId);
		Mockito.verify(mockStatement).setBoolean(5, true);
		Mockito.verify(mockStatement).setString(6, msgUuid);
		Mockito.verify(mockStatement).setObject(7, dateSent);
		Mockito.verify(mockStatement).setString(8, SyncOperation.u.name());
		Mockito.verify(mockStatement).setObject(9, dateReceived);
	}
	
}
