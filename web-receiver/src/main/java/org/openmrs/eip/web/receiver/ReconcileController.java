package org.openmrs.eip.web.receiver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openmrs.eip.app.AppUtils;
import org.openmrs.eip.app.management.entity.receiver.ReceiverReconciliation;
import org.openmrs.eip.app.management.entity.receiver.SiteReconciliation;
import org.openmrs.eip.app.management.repository.ReceiverReconcileRepository;
import org.openmrs.eip.app.management.repository.ReceiverTableReconcileRepository;
import org.openmrs.eip.app.management.repository.SiteReconciliationRepository;
import org.openmrs.eip.app.management.service.ReceiverReconcileService;
import org.openmrs.eip.component.SyncProfiles;
import org.openmrs.eip.web.RestConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile(SyncProfiles.RECEIVER)
@RequestMapping(RestConstants.PATH_RECEIVER_RECONCILE)
public class ReconcileController {
	
	private static final Logger LOG = LoggerFactory.getLogger(ReconcileController.class);
	
	private ReceiverReconcileRepository reconcileRepo;
	
	private ReceiverReconcileService reconcileService;
	
	private SiteReconciliationRepository siteRecRepo;
	
	private ReceiverTableReconcileRepository tableRecRepo;
	
	public ReconcileController(ReceiverReconcileRepository reconcileRepo, ReceiverReconcileService reconcileService,
	    SiteReconciliationRepository siteRecRepo, ReceiverTableReconcileRepository tableRecRepo) {
		this.reconcileRepo = reconcileRepo;
		this.reconcileService = reconcileService;
		this.siteRecRepo = siteRecRepo;
		this.tableRecRepo = tableRecRepo;
	}
	
	@GetMapping
	public ReceiverReconciliation getReconciliation() {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Getting active reconciliation");
		}
		
		return reconcileRepo.getReconciliation();
	}
	
	@PostMapping
	public ReceiverReconciliation startReconciliation() {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Adding new reconciliation");
		}
		
		return reconcileService.addNewReconciliation();
	}
	
	@GetMapping("/" + RestConstants.PROGRESS)
	public Map<String, Number> getProgress() {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Getting progress of active reconciliation");
		}
		
		long completedSiteCount = siteRecRepo.countByDateCompletedNotNull();
		long totalCount = siteRecRepo.count();
		return Map.of("completedSiteCount", completedSiteCount, "totalCount", totalCount, "tableCount",
		    AppUtils.getTablesToSync().size());
	}
	
	@GetMapping("/" + RestConstants.SITE_PROGRESS)
	public Map<String, Long> getSiteProgress() {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Getting progress of incomplete site reconciliations");
		}
		
		List<SiteReconciliation> siteRecs = siteRecRepo.findAll();
		Map<String, Long> map = new HashMap<>(siteRecs.size());
		for (SiteReconciliation siteRec : siteRecs) {
			if (siteRec.getDateCompleted() != null) {
				continue;
			}
			
			final String key = siteRec.getSite().getId() + "^" + siteRec.getSite().getName();
			map.put(key, tableRecRepo.countByCompletedIsTrueAndSiteReconciliation(siteRec));
		}
		
		return map;
	}
	
}
