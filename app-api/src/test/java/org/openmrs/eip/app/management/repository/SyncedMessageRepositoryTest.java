package org.openmrs.eip.app.management.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.openmrs.eip.app.SyncConstants.MGT_DATASOURCE_NAME;
import static org.openmrs.eip.app.SyncConstants.MGT_TX_MGR;
import static org.openmrs.eip.app.route.TestUtils.getEntity;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openmrs.eip.app.management.entity.receiver.SiteInfo;
import org.openmrs.eip.app.management.entity.receiver.SyncedMessage;
import org.openmrs.eip.app.receiver.BaseReceiverTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;

@Sql(scripts = { "classpath:mgt_site_info.sql",
        "classpath:mgt_receiver_synced_msg.sql" }, config = @SqlConfig(dataSource = MGT_DATASOURCE_NAME, transactionManager = MGT_TX_MGR))
public class SyncedMessageRepositoryTest extends BaseReceiverTest {
	
	@Autowired
	private SyncedMessageRepository repo;
	
	@Autowired
	private SiteRepository siteRepo;
	
	@Test
	public void getBatchOfMessagesForEviction_shouldReturnAOrderedBatchOfMessagesToEvict() {
		SiteInfo site = getEntity(SiteInfo.class, 3L);
		Pageable page = PageRequest.of(0, 10);
		
		List<SyncedMessage> msgs = repo.getBatchOfMessagesForEviction(site, page);
		
		assertEquals(3, msgs.size());
		assertEquals(102l, msgs.get(0).getId().longValue());
		assertEquals(103l, msgs.get(1).getId().longValue());
		assertEquals(101l, msgs.get(2).getId().longValue());
	}
	
	@Test
	public void getBatchOfMessagesForEviction_shouldReturnResultsBasedOnThePageSize() {
		SiteInfo site = getEntity(SiteInfo.class, 3L);
		assertEquals(3, repo.getBatchOfMessagesForEviction(site, Pageable.unpaged()).size());
		
		List<SyncedMessage> msgs = repo.getBatchOfMessagesForEviction(site, PageRequest.of(0, 2));
		
		assertEquals(2, msgs.size());
		assertEquals(102l, msgs.get(0).getId().longValue());
		assertEquals(103l, msgs.get(1).getId().longValue());
	}
	
	@Test
	public void getBatchOfMessagesForIndexing_shouldReturnAOrderedBatchOfMessagesToIndex() {
		SiteInfo site = getEntity(SiteInfo.class, 4L);
		Pageable page = PageRequest.of(0, 10);
		
		List<SyncedMessage> msgs = repo.getBatchOfMessagesForIndexing(site, page);
		
		assertEquals(4, msgs.size());
		assertEquals(202l, msgs.get(0).getId().longValue());
		assertEquals(203l, msgs.get(1).getId().longValue());
		assertEquals(201l, msgs.get(2).getId().longValue());
		assertEquals(204l, msgs.get(3).getId().longValue());
	}
	
	@Test
	public void getBatchOfMessagesForIndexing_shouldReturnResultsBasedOnThePageSize() {
		SiteInfo site = getEntity(SiteInfo.class, 4L);
		assertEquals(4, repo.getBatchOfMessagesForIndexing(site, Pageable.unpaged()).size());
		
		List<SyncedMessage> msgs = repo.getBatchOfMessagesForIndexing(site, PageRequest.of(0, 2));
		
		assertEquals(2, msgs.size());
		assertEquals(202l, msgs.get(0).getId().longValue());
		assertEquals(203l, msgs.get(1).getId().longValue());
	}
	
	@Test
	public void getBatchOfMessagesForResponse_shouldReturnAOrderedBatchOfMessagesToSendResponses() {
		SiteInfo site = getEntity(SiteInfo.class, 1L);
		Pageable page = PageRequest.of(0, 10);
		
		List<SyncedMessage> msgs = repo.getBatchOfMessagesForResponse(site, page);
		
		assertEquals(3, msgs.size());
		assertEquals(1l, msgs.get(0).getId().longValue());
		assertEquals(2l, msgs.get(1).getId().longValue());
		assertEquals(3l, msgs.get(2).getId().longValue());
	}
	
	@Test
	public void getBatchOfMessagesForResponse_shouldReturnResultsBasedOnThePageSize() {
		SiteInfo site = getEntity(SiteInfo.class, 1L);
		assertEquals(3, repo.getBatchOfMessagesForResponse(site, Pageable.unpaged()).size());
		
		List<SyncedMessage> msgs = repo.getBatchOfMessagesForResponse(site, PageRequest.of(0, 2));
		
		assertEquals(2, msgs.size());
		assertEquals(1l, msgs.get(0).getId().longValue());
		assertEquals(2l, msgs.get(1).getId().longValue());
	}
	
	@Test
	public void getBatchOfMessagesForArchiving_shouldReturnAOrderedBatchOfMessagesToArchive() {
		Pageable page = PageRequest.of(0, 10);
		
		List<SyncedMessage> msgs = repo.getBatchOfMessagesForArchiving(page);
		
		assertEquals(6, msgs.size());
		assertEquals(4l, msgs.get(0).getId().longValue());
		assertEquals(301l, msgs.get(1).getId().longValue());
		assertEquals(302l, msgs.get(2).getId().longValue());
		assertEquals(303l, msgs.get(3).getId().longValue());
		assertEquals(304l, msgs.get(4).getId().longValue());
		assertEquals(312l, msgs.get(5).getId().longValue());
	}
	
	@Test
	public void getBatchOfMessagesForArchiving_shouldReturnResultsBasedOnTheBatchSize() {
		assertEquals(6, repo.getBatchOfMessagesForArchiving(Pageable.unpaged()).size());
		
		List<SyncedMessage> msgs = repo.getBatchOfMessagesForArchiving(PageRequest.of(0, 2));
		
		assertEquals(2, msgs.size());
		assertEquals(4l, msgs.get(0).getId().longValue());
		assertEquals(301l, msgs.get(1).getId().longValue());
	}
	
	@Test
	public void getBatchOfMessagesForDeleting_shouldReturnAOrderedBatchOfMessagesToDelete() {
		SiteInfo site = getEntity(SiteInfo.class, 5L);
		Pageable page = PageRequest.of(0, 10);
		
		List<SyncedMessage> msgs = repo.getBatchOfMessagesForDeleting(site, page);
		
		assertEquals(2, msgs.size());
		assertEquals(305l, msgs.get(0).getId().longValue());
		assertEquals(306l, msgs.get(1).getId().longValue());
		//Site has error and confict items for which responses are not yet sent
		site = getEntity(SiteInfo.class, 1L);
		assertTrue(repo.getBatchOfMessagesForDeleting(site, page).isEmpty());
	}
	
	@Test
	public void getBatchOfMessagesForDeleting_shouldReturnResultsBasedOnTheBatchSize() {
		SiteInfo site = getEntity(SiteInfo.class, 5L);
		assertEquals(6, repo.getBatchOfMessagesForArchiving(Pageable.unpaged()).size());
		
		List<SyncedMessage> msgs = repo.getBatchOfMessagesForDeleting(site, PageRequest.of(0, 1));
		
		assertEquals(1, msgs.size());
		assertEquals(305l, msgs.get(0).getId().longValue());
	}
	
	@Test
	public void getMaxId_shouldReturnTheMaximumId() {
		assertEquals(312l, repo.getMaxId().longValue());
	}
	
	@Test
	public void markAsEvictedFromCache_shouldUpdatedAllSuccessfulRowsForCachedEntities() {
		final Pageable page = Pageable.ofSize(Long.valueOf(repo.count()).intValue());
		int evictableCount = 0;
		for (SiteInfo site : siteRepo.findAll()) {
			evictableCount += repo.getBatchOfMessagesForEviction(site, page).size();
		}
		Assert.assertTrue(evictableCount > 0);
		
		repo.markAsEvictedFromCache(repo.getMaxId());
		
		evictableCount = 0;
		for (SiteInfo site : siteRepo.findAll()) {
			evictableCount += repo.getBatchOfMessagesForEviction(site, page).size();
		}
		
		assertEquals(0, evictableCount);
	}
	
	@Test
	public void markAsReIndexed_shouldUpdatedAllSuccessfulRowsForCachedEntities() {
		final Pageable page = Pageable.ofSize(Long.valueOf(repo.count()).intValue());
		int indexableCount = 0;
		for (SiteInfo site : siteRepo.findAll()) {
			indexableCount += repo.getBatchOfMessagesForIndexing(site, page).size();
		}
		Assert.assertTrue(indexableCount > 0);
		
		repo.markAsReIndexed(repo.getMaxId());
		
		indexableCount = 0;
		for (SiteInfo site : siteRepo.findAll()) {
			indexableCount += repo.getBatchOfMessagesForIndexing(site, page).size();
		}
		
		assertEquals(0, indexableCount);
	}
	
}
