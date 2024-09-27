package org.openmrs.eip.app.management.repository;

import static org.junit.Assert.assertEquals;
import static org.openmrs.eip.app.SyncConstants.MGT_DATASOURCE_NAME;
import static org.openmrs.eip.app.SyncConstants.MGT_TX_MGR;

import java.util.List;

import org.junit.Test;
import org.openmrs.eip.app.management.entity.sender.SenderReconcileMessage;
import org.openmrs.eip.app.sender.BaseSenderTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;

@Sql(scripts = {
        "classpath:mgt_sender_reconcile_msg.sql" }, config = @SqlConfig(dataSource = MGT_DATASOURCE_NAME, transactionManager = MGT_TX_MGR))
public class SenderReconcileMsgRepositoryTest extends BaseSenderTest {
	
	@Autowired
	private SenderReconcileMsgRepository repo;
	
	@Test
	public void getBatch_shouldReadABatchOfMessages() {
		List<SenderReconcileMessage> msgs = repo.getBatch(Pageable.ofSize(5));
		assertEquals(3, msgs.size());
		assertEquals(1, msgs.get(0).getId().longValue());
		assertEquals(2, msgs.get(1).getId().longValue());
		assertEquals(3, msgs.get(2).getId().longValue());
		
		msgs = repo.getBatch(Pageable.ofSize(2));
		assertEquals(2, msgs.size());
		assertEquals(1, msgs.get(0).getId().longValue());
		assertEquals(2, msgs.get(1).getId().longValue());
	}
	
}
