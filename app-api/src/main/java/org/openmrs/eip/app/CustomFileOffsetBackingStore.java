package org.openmrs.eip.app;

import org.apache.kafka.connect.storage.FileOffsetBackingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom {@link FileOffsetBackingStore} that only saves the offset if no exception was encountered
 * while processing a source record read by debezium from the MySQL binlog to ensure no binlog entry
 * goes unprocessed.
 */
public class CustomFileOffsetBackingStore extends FileOffsetBackingStore {
	
	private static final Logger log = LoggerFactory.getLogger(CustomFileOffsetBackingStore.class);
	
	public static void enable() {
		CustomOffsetBackingStore.enable();
	}
	
	public static void disable() {
		CustomOffsetBackingStore.disable();
	}
	
	public static boolean isDisabled() {
		return CustomOffsetBackingStore.isDisabled();
	}
	
	public static boolean isPaused() {
		return CustomOffsetBackingStore.isPaused();
	}
	
	public static void pause() {
		CustomOffsetBackingStore.pause();
	}
	
	public static void unpause() {
		CustomOffsetBackingStore.unpause();
	}
	
	/**
	 * @see FileOffsetBackingStore#save()
	 */
	@Override
	protected void save() {
		CustomOffsetBackingStore.save(() -> {
			super.save();
			return true;
		});
	}
	
}
