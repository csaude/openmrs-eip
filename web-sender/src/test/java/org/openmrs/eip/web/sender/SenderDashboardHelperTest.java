package org.openmrs.eip.web.sender;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.openmrs.eip.app.management.entity.sender.DebeziumEvent;
import org.openmrs.eip.app.management.entity.sender.SenderRetryQueueItem;
import org.openmrs.eip.app.management.entity.sender.SenderSyncMessage;

public class SenderDashboardHelperTest {
	
	private SenderDashboardHelper helper = new SenderDashboardHelper(null, null);
	
	@Test
	public void getCategorizationProperty_shouldReturnTableName() {
		assertEquals("event.tableName", helper.getCategorizationProperty(SenderSyncMessage.class.getSimpleName()));
		assertEquals("event.tableName", helper.getCategorizationProperty(DebeziumEvent.class.getSimpleName()));
		assertEquals("event.tableName", helper.getCategorizationProperty(SenderRetryQueueItem.class.getSimpleName()));
	}
	
}
