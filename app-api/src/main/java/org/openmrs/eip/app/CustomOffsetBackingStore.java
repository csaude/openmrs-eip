package org.openmrs.eip.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomOffsetBackingStore {
	
	protected static final Logger log = LoggerFactory.getLogger(CustomOffsetBackingStore.class);
	
	private static boolean disabled = false;
	
	private static boolean paused = false;
	
	public static synchronized void enable() {
		disabled = false;
		if (log.isDebugEnabled()) {
			log.debug("Enabled saving of offsets");
		}
	}
	
	public static synchronized void disable() {
		disabled = true;
		if (log.isDebugEnabled()) {
			log.debug("Disabled saving of offsets");
		}
	}
	
	public static synchronized boolean isDisabled() {
		return disabled;
	}
	
	public static synchronized boolean isPaused() {
		return paused;
	}
	
	public static synchronized void pause() {
		paused = true;
		if (log.isDebugEnabled()) {
			log.debug("Pause saving of offsets");
		}
	}
	
	public static synchronized void unpause() {
		paused = false;
		if (log.isDebugEnabled()) {
			log.debug("Removing pause on saving of offsets");
		}
	}
	
	public static synchronized boolean save(OffsetSaveFunction saveFunction) {
		if (isDisabled() || isPaused()) {
			if (isPaused()) {
				if (log.isDebugEnabled()) {
					log.debug("Skipping saving of offset because it is paused");
				}
			} else {
				log.warn("Skipping saving of offset because an error was encountered while processing a source record");
			}
			
			return false;
		}
		
		if (log.isDebugEnabled()) {
			log.debug("Saving offset");
		}
		
		return saveFunction.save();
	}
	
	@FunctionalInterface
	public interface OffsetSaveFunction {
		
		boolean save();
	}
}
