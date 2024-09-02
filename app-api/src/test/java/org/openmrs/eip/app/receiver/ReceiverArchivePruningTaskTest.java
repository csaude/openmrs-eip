package org.openmrs.eip.app.receiver;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openmrs.eip.app.AppUtils;
import org.openmrs.eip.app.management.entity.receiver.ReceiverSyncArchive;
import org.openmrs.eip.app.management.entity.receiver.SiteInfo;
import org.openmrs.eip.app.management.repository.ReceiverSyncArchiveRepository;
import org.openmrs.eip.component.SyncContext;
import org.openmrs.eip.component.SyncOperation;
import org.openmrs.eip.component.model.PersonModel;
import org.openmrs.eip.component.utils.DateUtils;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.data.domain.Pageable;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ SyncContext.class, AppUtils.class, DateUtils.class })
public class ReceiverArchivePruningTaskTest {
	
	@Mock
	private ReceiverSyncArchiveRepository mockRepo;
	
	@Mock
	private Pageable mockPage;
	
	@Mock
	private Date mockMaxDate;
	
	@Mock
	private PreparedStatement mockStatement;
	
	private ReceiverArchivePruningTask task;
	
	@Before
	public void setup() {
		PowerMockito.mockStatic(SyncContext.class);
		PowerMockito.mockStatic(AppUtils.class);
		PowerMockito.mockStatic(DateUtils.class);
		setInternalState(BaseReceiverSyncPrioritizingTask.class, "initialized", true);
	}
	
	@After
	public void tearDown() {
		setInternalState(BaseReceiverSyncPrioritizingTask.class, "initialized", false);
	}
	
	@Test
	public void getNextBatch_shouldReadTheNextPageOfArchivesToBePruned() {
		when(AppUtils.getTaskPage()).thenReturn(mockPage);
		final int maxAgeDays = 3;
		final List<Date> asOfDates = new ArrayList();
		when(DateUtils.subtractDays(any(Date.class), eq(maxAgeDays))).thenAnswer(invocation -> {
			asOfDates.add(invocation.getArgument(0));
			return mockMaxDate;
		});
		task = new ReceiverArchivePruningTask(maxAgeDays);
		setInternalState(task, ReceiverSyncArchiveRepository.class, mockRepo);
		Long timestamp = System.currentTimeMillis();
		
		task.getNextBatch();
		
		verify(mockRepo).findByDateCreatedLessThanEqual(mockMaxDate, mockPage);
		Date asOfDate = asOfDates.get(0);
		assertTrue(asOfDate.getTime() == timestamp || asOfDate.getTime() > timestamp);
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
		ReceiverSyncArchive archive = new ReceiverSyncArchive();
		archive.setDateCreated(new Date());
		archive.setModelClassName(modelClass);
		archive.setIdentifier(uuid);
		archive.setEntityPayload(payload);
		SiteInfo site = new SiteInfo();
		site.setId(siteId);
		archive.setSite(site);
		archive.setSnapshot(true);
		archive.setMessageUuid(msgUuid);
		archive.setDateSentBySender(dateSent);
		archive.setDateReceived(dateReceived);
		archive.setOperation(SyncOperation.u);
		archive.setSyncVersion(version);
        task = new ReceiverArchivePruningTask(0);
		
		task.addItem(mockStatement, archive);
		
		Mockito.verify(mockStatement).setString(1, modelClass);
		Mockito.verify(mockStatement).setString(2, uuid);
		Mockito.verify(mockStatement).setString(3, payload);
		Mockito.verify(mockStatement).setLong(4, siteId);
		Mockito.verify(mockStatement).setBoolean(5, true);
		Mockito.verify(mockStatement).setString(6, msgUuid);
		Mockito.verify(mockStatement).setObject(7, dateSent);
		Mockito.verify(mockStatement).setString(8, SyncOperation.u.name());
		Mockito.verify(mockStatement).setObject(9, dateReceived);
		Mockito.verify(mockStatement).setString(10, version);
	}
	
}
