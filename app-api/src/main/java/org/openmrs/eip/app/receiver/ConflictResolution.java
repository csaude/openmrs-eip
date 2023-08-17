package org.openmrs.eip.app.receiver;

import java.util.ArrayList;
import java.util.List;

import org.openmrs.eip.app.management.entity.receiver.ConflictQueueItem;
import org.openmrs.eip.component.exception.EIPException;

import lombok.Getter;

/**
 * Encapsulates resolutions details for a single {@link ConflictQueueItem}
 */
public class ConflictResolution {
	
	/**
	 * Enumeration of the possible winning states during a merge
	 */
	public enum ResolutionDecision {
		/**
		 * The current state in the receiving database is preserved i.e. the entire new state is effectively
		 * ignored.
		 */
		IGNORE_NEW,
		
		/**
		 * The new state from the remote site is synced as is i.e. the database state is effectively all
		 * overwritten just like a regular sync.
		 */
		SYNC_NEW,
		
		/**
		 * A merged state is synced i.e. the user provides the individual property resolutions for the
		 * fields they wish to overwrite with the new values from the remote sites and those they wish
		 * ignore to sync to preserve their current databases values. In the future may be the user can be
		 * allowed to provide a different value that is neither the new nor the current.
		 */
		MERGE
	}
	
	@Getter
	private ConflictQueueItem conflict;
	
	@Getter
	private ResolutionDecision decision;
	
	@Getter
	private List<PropertyResolutionDecision> propertyResolutions = new ArrayList<>();
	
	public ConflictResolution(ConflictQueueItem conflict, ResolutionDecision decision) {
		this.conflict = conflict;
		this.decision = decision;
	}
	
	/**
	 * Adds a resolution decision for a single property
	 * 
	 * @param propertyName the name of the property
	 * @param ignoreNewValue Specifies if the new value should be ignored or synced
	 */
	public void addPropertyDecision(String propertyName, boolean ignoreNewValue) {
		if (decision == ResolutionDecision.MERGE) {
			propertyResolutions.add(new PropertyResolutionDecision(propertyName, ignoreNewValue));
		} else {
			throw new EIPException("Only merge resolution decision supports property level decisions");
		}
	}
	
}
