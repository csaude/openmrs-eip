package org.openmrs.eip.web.receiver;

import java.util.Map;

import org.openmrs.eip.app.management.entity.receiver.ReceiverSyncStatus;
import org.openmrs.eip.component.SyncProfiles;
import org.openmrs.eip.web.RestConstants;
import org.openmrs.eip.web.controller.BaseRestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile(SyncProfiles.RECEIVER)
@RequestMapping(RestConstants.API_PATH + "/dbsync/receiver/status")
public class ReceiverSyncStatusController extends BaseRestController {
	
	private static final Logger log = LoggerFactory.getLogger(ReceiverSyncStatusController.class);
	
	@Override
	public Class<?> getClazz() {
		return ReceiverSyncStatus.class;
	}
	
	@GetMapping
	public Map<String, Object> getAll() {
		if (log.isDebugEnabled()) {
			log.debug("Fetching sync status items");
		}
		
		return doGetAll();
	}
}
