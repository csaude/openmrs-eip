package org.openmrs.eip.app.management.repository;

import java.util.List;

import org.openmrs.eip.app.management.entity.receiver.SiteInfo;
import org.openmrs.eip.app.management.entity.receiver.SyncedMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SyncedMessageRepository extends JpaRepository<SyncedMessage, Long> {
	
	String RESPONSE_QUERY = "SELECT m FROM SyncedMessage m WHERE m.site = :site AND m.responseSent = false";
	
	String EVICT_QUERY = "SELECT m FROM SyncedMessage m WHERE m.site = :site AND m.outcome = 'SUCCESS' AND "
	        + "m.cached = true AND m.evictedFromCache = false ORDER BY m.dateCreated ASC";
	
	String INDEX_QUERY = "SELECT m FROM SyncedMessage m WHERE m.site = :site AND m.outcome = 'SUCCESS' AND "
	        + "m.indexed = true AND m.searchIndexUpdated = false AND (m.cached = false OR m.evictedFromCache = true) "
	        + "ORDER BY m.dateCreated ASC";
	
	String ARCHIVE_QUERY = "SELECT m FROM SyncedMessage m WHERE m.site = :site AND m.outcome = 'SUCCESS' AND "
	        + "m.responseSent = true AND (m.cached = false OR m.evictedFromCache = true) AND (m.indexed = false OR "
	        + "m.searchIndexUpdated = true)";
	
	String DELETE_QUERY = "SELECT m FROM SyncedMessage m WHERE m.site = :site AND m.responseSent = true AND "
	        + "(m.outcome = 'CONFLICT' OR m.outcome = 'ERROR')";
	
	String CACHE_UPDATE_QUERY = "UPDATE SyncedMessage SET evictedFromCache = true WHERE id <= :maxId AND "
	        + "cached = true AND outcome = 'SUCCESS'";
	
	String INDEX_UPDATE_QUERY = "UPDATE SyncedMessage SET searchIndexUpdated = true WHERE id <= :maxId AND "
	        + "indexed = true AND outcome = 'SUCCESS'";
	
	/**
	 * Gets a batch of messages for which responses have not yet been sent
	 *
	 * @param site the site to match against
	 * @param pageable {@link Pageable} instance
	 * @return list of synced messages
	 */
	@Query(RESPONSE_QUERY)
	List<SyncedMessage> getBatchOfMessagesForResponse(@Param("site") SiteInfo site, Pageable pageable);
	
	/**
	 * Gets a batch of messages ordered by ascending date created for cached entities for which
	 * evictions have not yet been done.
	 *
	 * @param site the site to match against
	 * @param pageable {@link Pageable} instance
	 * @return list of synced messages
	 */
	@Query(EVICT_QUERY)
	List<SyncedMessage> getBatchOfMessagesForEviction(@Param("site") SiteInfo site, Pageable pageable);
	
	/**
	 * Gets a batch of messages ordered by ascending date created for indexed entities for which the
	 * index have not yet been updated.
	 *
	 * @param site the site to match against
	 * @param pageable {@link Pageable} instance
	 * @return list of synced messages
	 */
	@Query(INDEX_QUERY)
	List<SyncedMessage> getBatchOfMessagesForIndexing(@Param("site") SiteInfo site, Pageable pageable);
	
	/**
	 * Gets a batch of post processed synced messages for archiving for the specified site
	 *
	 * @param site the site to match against
	 * @param pageable {@link Pageable} instance
	 * @return list of synced messages
	 */
	@Query(ARCHIVE_QUERY)
	List<SyncedMessage> getBatchOfMessagesForArchiving(@Param("site") SiteInfo site, Pageable pageable);
	
	/**
	 * Gets a batch of synced messages for deleting for the specified site where responses are sent and
	 * the outcome is set to ERROR or CONFLICT
	 *
	 * @param site the site to match against
	 * @param pageable {@link Pageable} instance
	 * @return list of synced messages
	 */
	@Query(DELETE_QUERY)
	List<SyncedMessage> getBatchOfMessagesForDeleting(@Param("site") SiteInfo site, Pageable pageable);
	
	/**
	 * Gets the maximum synced message id
	 *
	 * @return maximum id
	 */
	@Query("SELECT MAX(m.id) FROM SyncedMessage m")
	Long getMaxId();
	
	/**
	 * Marks all messages with for cached entities as cached, matches only those with an id that is less
	 * than or equal to the specified maximum id.
	 *
	 * @@param maxId maximum id to match
	 */
	@Modifying
	@Query(CACHE_UPDATE_QUERY)
	void markAsEvictedFromCache(@Param("maxId") Long maxId);
	
	/**
	 * Marks all messages with for indexed entities as re-indexed, matches only those with an id that is
	 * less than or equal to the specified maximum id.
	 *
	 * @param maxId maximum id to match
	 */
	@Modifying
	@Query(INDEX_UPDATE_QUERY)
	void markAsReIndexed(@Param("maxId") Long maxId);
	
}
