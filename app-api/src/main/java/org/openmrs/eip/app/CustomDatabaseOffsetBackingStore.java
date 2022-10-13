package org.openmrs.eip.app;

import org.apache.kafka.connect.storage.MemoryOffsetBackingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomDatabaseOffsetBackingStore extends MemoryOffsetBackingStore {
	
	protected static final Logger log = LoggerFactory.getLogger(CustomDatabaseOffsetBackingStore.class);
	
	@Override
	public synchronized void start() {
		super.start();
		load();
	}
	
	private void load() {
		// TODO load from the database
	}
	
	@Override
	public void save() {
		CustomOffsetBackingStore.save(() -> {
			
			// TODO save to the database
			
			return true;
		});
	}
	
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
	
}
