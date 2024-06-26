package org.openmrs.eip.component.utils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public final class DateUtils {
	
	private DateUtils() {
	}
	
	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	
	public static String dateToString(final LocalDateTime dateTime) {
		if (dateTime == null) {
			return null;
		}
		return dateTime.format(FORMATTER);
	}
	
	public static LocalDateTime stringToDate(final String dateAsString) {
		if (dateAsString == null) {
			return null;
		}
		return LocalDateTime.parse(dateAsString, FORMATTER);
	}
	
	/**
	 * Checks the dates in the first collection argument to those in the second to determine the
	 * collection containing the latest date, null being considered to be the earliest value.
	 *
	 * @param dates1 Collection of dates
	 * @param dates2 Collection of dates
	 * @return true ONLY if dates1 contains the latest non null date instance otherwise false
	 */
	public static boolean containsLatestDate(Collection<LocalDateTime> dates1, Collection<LocalDateTime> dates2) {
		//The algorithm is:
		//1: For each collection remove nulls first
		//2: Sort the dates and get the last item because that would be the latest in the collection
		//3: Compare the 2 latest dates from each collection to determine the latest between them
		List<LocalDateTime> sorted1 = dates1.stream().filter(d -> d != null).sorted().collect(Collectors.toList());
		List<LocalDateTime> sorted2 = dates2.stream().filter(d -> d != null).sorted().collect(Collectors.toList());
		LocalDateTime latestDateFromColl1 = sorted1.isEmpty() ? null : sorted1.get(sorted1.size() - 1);
		LocalDateTime latestDateFromColl2 = sorted2.isEmpty() ? null : sorted2.get(sorted2.size() - 1);
		if (latestDateFromColl1 == null) {
			return false;
		}
		
		return latestDateFromColl2 == null || latestDateFromColl1.isAfter(latestDateFromColl2);
	}
	
	/**
	 * Convert Date to LocalDateTime if the @param dateToConvert is not null
	 * 
	 * @param dateToConvert
	 * @return
	 */
	public static LocalDateTime dateToLocalDateTime(Date dateToConvert) {
		if (dateToConvert != null) {
			return dateToConvert.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
		}
		
		return null;
		
	}
	
	/**
	 * Subtracts the specified days from the specified the date
	 *
	 * @param date date to subtract from
	 * @param days the number of days to subtract
	 * @return Date
	 */
	public static Date subtractDays(Date date, int days) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(Calendar.DAY_OF_YEAR, -days);
		return calendar.getTime();
	}
	
	/**
	 * Checks if a date comes after another date with null being the earliest date
	 *
	 * @param date the date to check
	 * @param other the other date to check against
	 * @return true ONLY if date is not null and is the same or comes after other date otherwise false
	 */
	public static boolean isDateAfterOrEqual(LocalDateTime date, LocalDateTime other) {
		if (date == null) {
			return other == null;
		}
		
		return other == null || date.isAfter(other) || date.equals(other);
	}
	
}
