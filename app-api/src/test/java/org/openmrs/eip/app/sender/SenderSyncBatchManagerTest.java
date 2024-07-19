package org.openmrs.eip.app.sender;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.ZoneId;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.openmrs.eip.app.AppUtils;
import org.openmrs.eip.app.management.entity.sender.SenderSyncMessage;
import org.openmrs.eip.component.model.SyncMetadata;
import org.openmrs.eip.component.model.SyncModel;
import org.openmrs.eip.component.utils.JsonUtils;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@RunWith(PowerMockRunner.class)
@PrepareForTest(SenderUtils.class)
public class SenderSyncBatchManagerTest {
	
	private SenderSyncBatchManager manager;
	
	@Before
	public void setup() {
		PowerMockito.mockStatic(SenderUtils.class);
		manager = new SenderSyncBatchManager(null);
	}
	
	@Test
	public void getQueueName_shouldReturnTheNameOfTheJmsQueue() {
		final String queueName = "openmrs.sync";
		Mockito.when(SenderUtils.getQueueName()).thenReturn(queueName);
		assertEquals(queueName, manager.getQueueName());
	}
	
	@Test
	public void convert_shouldConvertSyncMessageToModel() {
		SyncMetadata metadata = new SyncMetadata();
		Map<String, Object> syncData = Map.of("metadata", metadata);
		SenderSyncMessage msg = new SenderSyncMessage();
		msg.setData(JsonUtils.marshall(syncData));
		long timestamp = System.currentTimeMillis();
		
		SyncModel model = manager.convert(msg);
		
		assertEquals(AppUtils.getVersion(), model.getMetadata().getSyncVersion());
		long dateSent = model.getMetadata().getDateSent().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
		assertTrue(dateSent == timestamp || dateSent > timestamp);
	}
	
}
