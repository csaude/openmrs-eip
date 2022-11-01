package org.openmrs.eip.web;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;

public final class RestConstants {
	
	public static final String API_PATH = "/api/";
	
	public static final String PATH_LOGIN = "/login";
	
	public static final String FIELD_COUNT = "count";
	
	public static final String FIELD_ITEMS = "items";
	
	public static final Integer DEFAULT_MAX_COUNT = 500;
	
	public static final String DATE_PATTERN = "yyyy-MM-dd";
	
	public static final DateFormat DATE_FORMAT = new SimpleDateFormat(DATE_PATTERN);
	
	public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(RestConstants.DATE_PATTERN);
	
}
