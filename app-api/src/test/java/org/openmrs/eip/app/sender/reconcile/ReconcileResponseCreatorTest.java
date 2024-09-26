package org.openmrs.eip.app.sender.reconcile;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openmrs.eip.app.AppUtils;
import org.openmrs.eip.app.SyncConstants;
import org.openmrs.eip.app.management.entity.receiver.JmsMessage.MessageType;
import org.powermock.modules.junit4.PowerMockRunner;

import jakarta.jms.BytesMessage;
import jakarta.jms.Session;

@RunWith(PowerMockRunner.class)
public class ReconcileResponseCreatorTest {
	
	@Mock
	private Session mockSession;
	
	@Mock
	private BytesMessage mockMsg;
	
	@Test
	public void createMessage_shouldCreateJmsMessageForTheResponse() throws Exception {
		final byte[] body = "test_body".getBytes();
		final String siteId = "test_site_id";
		ReconcileResponseCreator creator = new ReconcileResponseCreator(body, siteId);
		Mockito.when(mockSession.createBytesMessage()).thenReturn(mockMsg);
		
		BytesMessage msg = (BytesMessage) creator.createMessage(mockSession);
		
		assertEquals(msg, mockMsg);
		Mockito.verify(msg).writeBytes(body);
		Mockito.verify(msg).setStringProperty(SyncConstants.JMS_HEADER_SITE, siteId);
		Mockito.verify(msg).setStringProperty(SyncConstants.JMS_HEADER_TYPE, MessageType.RECONCILE.name());
		Mockito.verify(msg).setStringProperty(eq(SyncConstants.JMS_HEADER_MSG_ID), ArgumentMatchers.anyString());
		Mockito.verify(msg).setStringProperty(eq(SyncConstants.JMS_HEADER_VERSION), eq(AppUtils.getVersion()));
	}
	
}
