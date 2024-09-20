package org.openmrs.eip.app.receiver.processor;

import static org.junit.Assert.assertEquals;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openmrs.eip.app.AppUtils;
import org.openmrs.eip.app.BaseQueueProcessor;
import org.openmrs.eip.app.management.entity.receiver.ConflictQueueItem;
import org.openmrs.eip.app.management.entity.receiver.SiteInfo;
import org.openmrs.eip.app.management.service.ConflictService;
import org.openmrs.eip.component.SyncContext;
import org.openmrs.eip.component.model.PersonModel;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(SyncContext.class)
public class ConflictMessageProcessorTest {
	
	@Mock
	private ConflictService mockService;
	
	private ConflictMessageProcessor processor;
	
	@Before
	public void setup() {
		PowerMockito.mockStatic(SyncContext.class);
		Mockito.when(SyncContext.getBean(ConflictService.class)).thenReturn(mockService);
		setInternalState(BaseQueueProcessor.class, "initialized", true);
		processor = new ConflictMessageProcessor(null, null);
	}
	
	@After
	public void tearDown() {
		setInternalState(BaseQueueProcessor.class, "initialized", false);
	}
	
	@Test
	public void getUniqueId_shouldReturnDatabaseId() {
		final String uuid = "uuid";
		ConflictQueueItem conflict = new ConflictQueueItem();
		conflict.setIdentifier(uuid);
		assertEquals(uuid, processor.getUniqueId(conflict));
	}
	
	@Test
	public void getThreadName_shouldReturnThreadName() {
		final String uuid = "uuid";
		final String messageUuid = "message-uuid";
		final String siteUuid = "site-uuid";
		ConflictQueueItem c = new ConflictQueueItem();
		c.setModelClassName(PersonModel.class.getName());
		c.setIdentifier(uuid);
		c.setMessageUuid(messageUuid);
		SiteInfo siteInfo = new SiteInfo();
		siteInfo.setIdentifier(siteUuid);
		c.setSite(siteInfo);
		assertEquals(siteUuid + "-" + AppUtils.getSimpleName(c.getModelClassName()) + "-" + uuid + "-" + messageUuid,
		    processor.getThreadName(c));
	}
	
	@Test
	public void getSyncPayload_shouldReturnPayload() {
		final String payload = "{}";
		ConflictQueueItem conflict = new ConflictQueueItem();
		conflict.setEntityPayload(payload);
		Assert.assertEquals(payload, processor.getSyncPayload(conflict));
	}
	
	@Test
	public void sync_shouldSyncTheConflict() throws Throwable {
		Set<String> propsToSync = Set.of("prop1", "prop2");
		processor = new ConflictMessageProcessor(null, propsToSync);
		ConflictQueueItem conflict = new ConflictQueueItem();
		
		processor.sync(conflict);
		
		Mockito.verify(mockService).resolveWithMerge(conflict, propsToSync);
	}
	
}
