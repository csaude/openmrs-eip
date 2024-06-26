package org.openmrs.eip.web;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;

public final class RestConstants {
	
	public static final String API_PATH = "/api/";
	
	public static final String PATH_LOGIN = "/login";
	
	public static final String PATH_VAR_ID = "id";
	
	public static final String SUB_PATH_DB_SYNC = API_PATH + "dbsync/";
	
	public static final String SUB_PATH_RECEIVER = SUB_PATH_DB_SYNC + "receiver/";
	
	public static final String PATH_RECEIVER_SYNC_MSG = SUB_PATH_RECEIVER + "sync";
	
	public static final String PATH_RECEIVER_SYNCED_MSG = SUB_PATH_RECEIVER + "synced";
	
	public static final String PATH_RECEIVER_ARCHIVE = SUB_PATH_RECEIVER + "archive";
	
	public static final String RES_RECEIVER_CONFLICT = SUB_PATH_RECEIVER + "conflict";
	
	public static final String RES_RECEIVER_CONFLICT_BY_ID = SUB_PATH_RECEIVER + "conflict/{" + PATH_VAR_ID + "}";
	
	public static final String PATH_RECEIVER_RECONCILE = SUB_PATH_RECEIVER + "reconcile";
	
	public static final String ACTION_DIFF = "/{" + PATH_VAR_ID + "}/diff";
	
	public static final String PATH_RECEIVER_CONFLICT_DIFF = RES_RECEIVER_CONFLICT + ACTION_DIFF;
	
	public static final String ACTION_RESOLVE = "/{" + PATH_VAR_ID + "}/resolve";
	
	public static final String PATH_RECEIVER_CONFLICT_RESOLVE = RES_RECEIVER_CONFLICT + ACTION_RESOLVE;
	
	public static final String PARAM_GRP_PROP = "groupProperty";
	
	public static final String PARAM_START_DATE = "startDate";
	
	public static final String PARAM_END_DATE = "endDate";
	
	public static final String FIELD_COUNT = "count";
	
	public static final String FIELD_ITEMS = "items";
	
	public static final Integer DEFAULT_MAX_COUNT = 500;
	
	public static final String DATE_PATTERN = "yyyy-MM-dd";
	
	public static final DateFormat DATE_FORMAT = new SimpleDateFormat(DATE_PATTERN);
	
	public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(RestConstants.DATE_PATTERN);
	
	public static final String RES_DASHBOARD = SUB_PATH_DB_SYNC + "dashboard";
	
	public static final String SUB_PATH_DASHBOARD = RES_DASHBOARD + "/";
	
	public static final String PATH_NAME_CATEGORY = "category";
	
	public static final String PATH_DASHBOARD_CATEGORY = SUB_PATH_DASHBOARD + PATH_NAME_CATEGORY;
	
	public static final String PARAM_ENTITY_TYPE = "entityType";
	
	public static final String PATH_NAME_COUNT = "count";
	
	public static final String PATH_DASHBOARD_COUNT = SUB_PATH_DASHBOARD + PATH_NAME_COUNT;
	
	public static final String PARAM_ENTITY_CATEGORY = "category";
	
	public static final String PARAM_ENTITY_OPERATION = "operation";
	
	public static final String PROGRESS = "progress";
	
	public static final String PATH_REC_RECONCILE_PROGRESS = PATH_RECEIVER_RECONCILE + "/" + PROGRESS;
	
	public static final String SITE_PROGRESS = "siteprogress";
	
	public static final String PATH_REC_SITE_PROGRESS = PATH_RECEIVER_RECONCILE + "/" + SITE_PROGRESS;
	
	public static final String TABLE_RECONCILE = "tablereconcile";
	
	public static final String RECONCILE_HISTORY = "history";
	
	public static final String RECONCILE_REPORT = "report";
	
	public static final String RECONCILE_REPORT_TOTALS = "reporttotals";
	
	public static final String PATH_VAR_SITE_ID = "siteId";
	
	public static final String PATH_VAR_REC_ID = "recId";
	
	public static final String PATH_PARAM_SITE_ID = "siteId";
	
	public static final String PATH_REC_TABLE_RECONCILE = PATH_RECEIVER_RECONCILE + "/" + TABLE_RECONCILE;
	
	public static final String PATH_RECONCILE_HISTORY = PATH_RECEIVER_RECONCILE + "/" + RECONCILE_HISTORY;
	
	public static final String PATH_RECONCILE_REPORT = PATH_RECEIVER_RECONCILE + "/" + RECONCILE_REPORT;
	
	public static final String PATH_RECONCILE_TOTALS = PATH_RECEIVER_RECONCILE + "/" + RECONCILE_REPORT_TOTALS;
	
	public static final String RES_SITE = "site";
	
	public static final String PATH_SITE = SUB_PATH_RECEIVER + RES_SITE;
	
}
