package org.openmrs.eip.app.management.repository;

import java.util.List;

import org.openmrs.eip.app.management.entity.receiver.ConflictQueueItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConflictRepository extends JpaRepository<ConflictQueueItem, Long> {
	
	/**
	 * Gets the ids of all the existing conflicts
	 * 
	 * @return list of ids
	 */
	@Query("SELECT id FROM ConflictQueueItem ORDER BY dateReceived ASC")
	List<Long> getConflictIds();
	
	/**
	 * Gets the count of conflicts matching the specified identifier and model class names
	 * 
	 * @param identifier the identifier
	 * @param modelClassNames the model class names
	 * @param page {@link Pageable} instance
	 * @return count of conflicts
	 */
	@Query("SELECT c from ConflictQueueItem c WHERE c.identifier=:id AND c.modelClassName IN (:classes)")
	List<ConflictQueueItem> getByIdentifierAndModelClasses(@Param("id") String identifier,
	                                                       @Param("classes") List<String> modelClassNames, Pageable page);
	
}
