package org.openmrs.eip.app.route.receiver;

import org.openmrs.eip.app.receiver.ReceiverConstants;
import org.openmrs.eip.app.route.BaseRouteTest;
import org.openmrs.eip.component.SyncProfiles;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import static org.openmrs.eip.app.route.receiver.BaseReceiverRouteTest.RECEIVER_ID;

@ActiveProfiles(SyncProfiles.RECEIVER)
@TestPropertySource(properties = ReceiverConstants.PROP_CAMEL_OUTPUT_ENDPOINT + "=")
@TestPropertySource(properties = ReceiverConstants.PROP_RECEIVER_ID + "=" + RECEIVER_ID)
public abstract class BaseReceiverRouteTest extends BaseRouteTest {
	
	public static final String RECEIVER_ID = "test-receiver-id";
	
	@Override
	public String getAppFolderName() {
		return "receiver";
	}
	
}
