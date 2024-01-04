package org.openmrs.eip.app.management.entity.sender;

import static org.apache.commons.lang3.reflect.MethodUtils.invokeMethod;
import static org.junit.Assert.assertEquals;

import java.beans.PropertyDescriptor;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.openmrs.eip.app.route.TestUtils;
import org.springframework.beans.BeanUtils;

public class SenderSyncArchiveTest {
	
	@Test
	public void shouldCreateASenderArchiveFromASyncMessage() throws Exception {
		PropertyDescriptor[] descriptors = BeanUtils.getPropertyDescriptors(SenderSyncMessage.class);
		SenderSyncMessage syncMessage = new SenderSyncMessage();
		syncMessage.setId(1L);
		syncMessage.setDateCreated(new Date());
		Event event = TestUtils.createEvent("person", "uuid", "c");
		event.setDateCreated(new Date());
		event.setSnapshot(true);
		event.setRequestUuid("request-uuid");
		syncMessage.setEvent(event);
		syncMessage.setMessageUuid("message-uuid");
		syncMessage.markAsSent(LocalDateTime.now());
		syncMessage.setData("{}");
		
		SenderSyncArchive archive = new SenderSyncArchive(syncMessage);
		
		Assert.assertNull(archive.getId());
		Assert.assertNull(archive.getDateCreated());
		Set<String> ignored = new HashSet();
		ignored.add("id");
		ignored.add("class");
		ignored.add("dateCreated");
		ignored.add("status");
		for (PropertyDescriptor descriptor : descriptors) {
			if (ignored.contains(descriptor.getName())) {
				continue;
			}
			
			String getter = descriptor.getReadMethod().getName();
			assertEquals(invokeMethod(syncMessage, getter), invokeMethod(archive, getter));
		}
	}
	
}
