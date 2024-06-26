package org.openmrs.eip.web.receiver;

import static org.openmrs.eip.web.RestConstants.PATH_PARAM_SITE_ID;
import static org.openmrs.eip.web.RestConstants.PATH_VAR_REC_ID;
import static org.openmrs.eip.web.RestConstants.PATH_VAR_SITE_ID;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openmrs.eip.app.AppUtils;
import org.openmrs.eip.app.management.entity.receiver.ReceiverReconciliation;
import org.openmrs.eip.app.management.entity.receiver.ReceiverReconciliation.ReconciliationStatus;
import org.openmrs.eip.app.management.entity.receiver.ReceiverTableReconciliation;
import org.openmrs.eip.app.management.entity.receiver.SiteReconciliation;
import org.openmrs.eip.app.management.repository.ReceiverReconcileRepository;
import org.openmrs.eip.app.management.repository.ReceiverTableReconcileRepository;
import org.openmrs.eip.app.management.repository.ReconcileTableSummaryRepository;
import org.openmrs.eip.app.management.repository.SiteReconciliationRepository;
import org.openmrs.eip.app.management.repository.SiteRepository;
import org.openmrs.eip.app.management.service.ReceiverReconcileService;
import org.openmrs.eip.component.SyncProfiles;
import org.openmrs.eip.web.RestConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile(SyncProfiles.RECEIVER)
@RequestMapping(RestConstants.PATH_RECEIVER_RECONCILE)
public class ReceiverReconcileController {
	
	private static final Logger LOG = LoggerFactory.getLogger(ReceiverReconcileController.class);
	
	private ReceiverReconcileRepository reconcileRepo;
	
	private ReceiverReconcileService reconcileService;
	
	private SiteReconciliationRepository siteRecRepo;
	
	private ReceiverTableReconcileRepository tableRecRepo;
	
	private SiteRepository siteRepo;
	
	private ReconcileTableSummaryRepository summaryRepo;
	
	public ReceiverReconcileController(ReceiverReconcileRepository reconcileRepo, ReceiverReconcileService reconcileService,
	    SiteReconciliationRepository siteRecRepo, ReceiverTableReconcileRepository tableRecRepo, SiteRepository siteRepo,
	    ReconcileTableSummaryRepository summaryRepo) {
		this.reconcileRepo = reconcileRepo;
		this.reconcileService = reconcileService;
		this.siteRecRepo = siteRecRepo;
		this.tableRecRepo = tableRecRepo;
		this.siteRepo = siteRepo;
		this.summaryRepo = summaryRepo;
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
	
	@GetMapping("/" + RestConstants.TABLE_RECONCILE + "/{" + PATH_VAR_SITE_ID + "}")
	public List<ReceiverTableReconciliation> getIncompleteTableReconciliations(@PathVariable(PATH_VAR_SITE_ID) Long siteId) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Getting incomplete table reconciliations for site with id {}", siteId);
		}
		
		SiteReconciliation siteRec = siteRecRepo.getBySite(siteRepo.getReferenceById(siteId));
		List<String> reconciledTables = tableRecRepo.getReconciledTables(siteRec);
		List<ReceiverTableReconciliation> incomplete = tableRecRepo.getByCompletedIsFalseAndSiteReconciliation(siteRec);
		List<String> incompleteTables = incomplete.stream().map(r -> r.getTableName().toLowerCase()).toList();
		//Include tables for which no rows have been received from the remote site
		List<ReceiverTableReconciliation> unStarted = AppUtils.getTablesToSync().stream()
		        .filter(t -> !reconciledTables.contains(t.toLowerCase()) && !incompleteTables.contains(t.toLowerCase()))
		        .map(t -> {
			        ReceiverTableReconciliation r = new ReceiverTableReconciliation();
			        r.setTableName(t.toLowerCase());
			        return r;
		        }).toList();
		
		incomplete.addAll(unStarted);
		return incomplete;
	}
	
	@GetMapping("/" + RestConstants.RECONCILE_HISTORY)
	public List<ReceiverReconciliation> getHistory() {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Getting most recent reconciliation history");
		}
		
		return reconcileRepo.getTop3ByStatusOrderByDateCreatedDesc(ReconciliationStatus.COMPLETED);
	}
	
	@GetMapping("/" + RestConstants.RECONCILE_REPORT_TOTALS + "/{" + PATH_VAR_REC_ID + "}")
	public Object[] getCountTotals(@PathVariable(value = PATH_VAR_REC_ID) String recId,
	                               @RequestParam(value = PATH_PARAM_SITE_ID, required = false) String siteId) {
		if (LOG.isDebugEnabled()) {
			String msg = "Getting count totals for reconciliation with id: " + recId;
			if (siteId != null) {
				msg = msg + " and site with id: " + siteId;
			}
			
			LOG.debug(msg);
		}
		
		ReceiverReconciliation rec = reconcileRepo.getByIdentifier(recId);
		List<Object[]> totals;
		if (siteId == null) {
			totals = summaryRepo.getCountTotals(rec);
		} else {
			totals = summaryRepo.getCountTotalsBySite(rec, siteRepo.getByIdentifier(siteId));
		}
		
		return totals.get(0);
	}
	
	@GetMapping("/" + RestConstants.RECONCILE_REPORT + "/{" + PATH_VAR_REC_ID + "}")
	public Object getReport(@PathVariable(value = PATH_VAR_REC_ID) String recId,
	                        @RequestParam(value = PATH_PARAM_SITE_ID, required = false) String siteId) {
		if (LOG.isDebugEnabled()) {
			String msg = "Getting report for reconciliation with id: " + recId;
			if (siteId != null) {
				msg = msg + " and site with id: " + siteId;
			}
			
			LOG.debug(msg);
		}
		
		ReceiverReconciliation rec = reconcileRepo.getByIdentifier(recId);
		if (siteId == null) {
			return summaryRepo.getReport(rec);
		} else {
			return summaryRepo.getByReconciliationAndSite(rec, siteRepo.getByIdentifier(siteId));
		}
	}
	
}
