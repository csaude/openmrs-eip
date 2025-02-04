package org.openmrs.eip.web.receiver;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.openmrs.eip.app.SyncConstants.MGT_DATASOURCE_NAME;
import static org.openmrs.eip.app.SyncConstants.MGT_TX_MGR;
import static org.openmrs.eip.web.RestConstants.PATH_PARAM_SITE_ID;
import static org.openmrs.eip.web.RestConstants.PATH_RECEIVER_RECONCILE;
import static org.openmrs.eip.web.RestConstants.PATH_REC_RECONCILE_PROGRESS;
import static org.openmrs.eip.web.RestConstants.PATH_REC_SITE_PROGRESS;
import static org.openmrs.eip.web.RestConstants.PATH_REC_TABLE_RECONCILE;
import static org.openmrs.eip.web.RestConstants.PATH_VAR_REC_ID;
import static org.openmrs.eip.web.RestConstants.PATH_VAR_SITE_ID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.hamcrest.CoreMatchers;
import org.hamcrest.CustomMatcher;
import org.hamcrest.Matcher;
import org.junit.Assert;
import org.junit.Test;
import org.openmrs.eip.app.AppUtils;
import org.openmrs.eip.app.management.entity.receiver.ReceiverReconciliation;
import org.openmrs.eip.app.management.repository.ReceiverReconcileRepository;
import org.openmrs.eip.web.RestConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ReceiverReconcileControllerTest extends BaseReceiverWebTest {
	
	@Autowired
	private ReceiverReconcileRepository reconcileRepo;
	
	@Test
	@Sql(scripts = {
	        "classpath:mgt_receiver_reconcile.sql" }, config = @SqlConfig(dataSource = MGT_DATASOURCE_NAME, transactionManager = MGT_TX_MGR))
	public void getReconciliation_shouldGetTheReconciliation() throws Exception {
		MockHttpServletRequestBuilder builder = get(PATH_RECEIVER_RECONCILE);
		
		ResultActions result = mockMvc.perform(builder);
		
		result.andExpect(status().isOk());
		result.andExpect(jsonPath("identifier", equalTo("rec-5")));
	}
	
	@Test
	public void getReconciliation_shouldReturnNoneIfThereIsNoIncompleteReconciliation() throws Exception {
		MockHttpServletRequestBuilder builder = get(PATH_RECEIVER_RECONCILE);
		
		ResultActions result = mockMvc.perform(builder);
		
		result.andExpect(status().isOk());
		result.andExpect(content().string(CoreMatchers.is("")));
	}
	
	@Test
	public void startReconciliation_shouldAddANewReconciliation() throws Exception {
		Assert.assertNull(reconcileRepo.getReconciliation());
		MockHttpServletRequestBuilder builder = post(PATH_RECEIVER_RECONCILE);
		
		ResultActions result = mockMvc.perform(builder);
		
		result.andExpect(status().isOk());
		ReceiverReconciliation rec = reconcileRepo.getReconciliation();
		result.andExpect(jsonPath("identifier", equalTo(rec.getIdentifier())));
	}
	
	@Test
	@Sql(scripts = { "classpath:mgt_site_info.sql",
	        "classpath:mgt_site_reconcile.sql" }, config = @SqlConfig(dataSource = MGT_DATASOURCE_NAME, transactionManager = MGT_TX_MGR))
	public void getProgress_shouldGetTheReconciliationProgressDetails() throws Exception {
		MockHttpServletRequestBuilder builder = get(PATH_REC_RECONCILE_PROGRESS);
		
		ResultActions result = mockMvc.perform(builder);
		
		result.andExpect(status().isOk());
		result.andExpect(jsonPath("length()", equalTo(3)));
		result.andExpect(jsonPath("completedSiteCount", equalTo(1)));
		result.andExpect(jsonPath("totalCount", equalTo(5)));
		result.andExpect(jsonPath("tableCount", equalTo(AppUtils.getTablesToSync().size())));
	}
	
	@Test
	@Sql(scripts = { "classpath:mgt_site_info.sql", "classpath:mgt_site_reconcile.sql",
	        "classpath:mgt_receiver_table_reconcile.sql" }, config = @SqlConfig(dataSource = MGT_DATASOURCE_NAME, transactionManager = MGT_TX_MGR))
	public void getSiteProgress_shouldGetTheSiteReconciliationProgressDetails() throws Exception {
		MockHttpServletRequestBuilder builder = get(PATH_REC_SITE_PROGRESS);
		
		ResultActions result = mockMvc.perform(builder);
		
		result.andExpect(status().isOk());
		Map<String, Integer> siteAndCount = new HashMap<>();
		result.andDo(result1 -> {
			final String response = result1.getResponse().getContentAsString(StandardCharsets.UTF_8);
			siteAndCount.putAll(new ObjectMapper().readValue(response, Map.class));
		});
		assertEquals(4, siteAndCount.size());
		assertEquals(0, siteAndCount.get("2^Remote 2").intValue());
		assertEquals(0, siteAndCount.get("3^Remote 3").intValue());
		assertEquals(1, siteAndCount.get("4^Remote 4").intValue());
		assertEquals(2, siteAndCount.get("5^Remote 5").intValue());
	}
	
	@Test
	@Sql(scripts = { "classpath:mgt_site_info.sql", "classpath:mgt_site_reconcile.sql",
	        "classpath:mgt_receiver_table_reconcile.sql" }, config = @SqlConfig(dataSource = MGT_DATASOURCE_NAME, transactionManager = MGT_TX_MGR))
	public void getIncompleteTableReconciliations_shouldGetTheInCompleteTableReconciliationsForTheSite() throws Exception {
		MockHttpServletRequestBuilder builder = get(PATH_REC_TABLE_RECONCILE + "/{" + PATH_VAR_SITE_ID + "}", 5);
		
		ResultActions result = mockMvc.perform(builder);
		
		result.andExpect(status().isOk());
		result.andExpect(jsonPath("length()", equalTo(17)));
		result.andExpect(jsonPath("[0].tableName", equalTo("encounter")));
		result.andExpect(jsonPath("[0].rowCount", equalTo(10)));
		result.andExpect(jsonPath("[0].processedCount", equalTo(9)));
		result.andExpect(jsonPath("[0].lastBatchReceived", equalTo(false)));
		result.andExpect(jsonPath("[0].remoteStartDate", equalTo("2024-02-07T00:04:00")));
		Set<String> expectedTables = AppUtils.getTablesToSync();
		expectedTables.remove("PERSON");
		expectedTables.remove("VISIT");
		expectedTables.remove("ENCOUNTER");
		Matcher matcher = new CustomMatcher<>("Found unexpected table") {
			
			@Override
			public boolean matches(Object o) {
				return expectedTables.contains(o.toString().toUpperCase());
			}
		};
		
		for (int i = 1; i < expectedTables.size(); i++) {
			result.andExpect(jsonPath("[" + i + "].tableName", matcher));
			result.andExpect(jsonPath("[" + i + "].rowCount", equalTo(0)));
			result.andExpect(jsonPath("[" + i + "].processedCount", equalTo(0)));
			result.andExpect(jsonPath("[" + i + "].lastBatchReceived", equalTo(false)));
			result.andExpect(jsonPath("[" + i + "].remoteStartDate", CoreMatchers.nullValue()));
			i++;
		}
	}
	
	@Test
	@Sql(scripts = {
	        "classpath:mgt_receiver_reconcile.sql" }, config = @SqlConfig(dataSource = MGT_DATASOURCE_NAME, transactionManager = MGT_TX_MGR))
	public void getHistory_shouldReturnTheThreeMostRecentCompletedReconciliations() throws Exception {
		MockHttpServletRequestBuilder builder = get(RestConstants.PATH_RECONCILE_HISTORY);
		
		ResultActions result = mockMvc.perform(builder);
		
		result.andExpect(status().isOk());
		result.andExpect(jsonPath("length()", equalTo(3)));
		result.andExpect(jsonPath("[0].identifier", equalTo("rec-4")));
		result.andExpect(jsonPath("[1].identifier", equalTo("rec-3")));
		result.andExpect(jsonPath("[2].identifier", equalTo("rec-2")));
	}
	
	@Test
	@Sql(scripts = { "classpath:mgt_site_info.sql", "classpath:mgt_receiver_reconcile.sql",
	        "classpath:mgt_receiver_reconcile_table_summary.sql" }, config = @SqlConfig(dataSource = MGT_DATASOURCE_NAME, transactionManager = MGT_TX_MGR))
	public void getReportTotals_shouldReturnTheReconciliationCountTotals() throws Exception {
		MockHttpServletRequestBuilder builder = get(RestConstants.PATH_RECONCILE_TOTALS + "/{" + PATH_VAR_REC_ID + "}",
		    "rec-1");
		
		ResultActions result = mockMvc.perform(builder);
		
		result.andExpect(status().isOk());
		result.andExpect(jsonPath("length()", equalTo(6)));
		result.andExpect(jsonPath("[0]", equalTo(11)));
		result.andExpect(jsonPath("[1]", equalTo(3)));
		result.andExpect(jsonPath("[2]", equalTo(4)));
		result.andExpect(jsonPath("[3]", equalTo(18)));
		result.andExpect(jsonPath("[4]", equalTo(7)));
		result.andExpect(jsonPath("[5]", equalTo(5)));
	}
	
	@Test
	@Sql(scripts = { "classpath:mgt_site_info.sql", "classpath:mgt_receiver_reconcile.sql",
	        "classpath:mgt_receiver_reconcile_table_summary.sql" }, config = @SqlConfig(dataSource = MGT_DATASOURCE_NAME, transactionManager = MGT_TX_MGR))
	public void getReportTotals_shouldReturnTheReconciliationCountTotalsForTheSite() throws Exception {
		MockHttpServletRequestBuilder builder = get(RestConstants.PATH_RECONCILE_TOTALS + "/{" + PATH_VAR_REC_ID + "}",
		    "rec-1");
		builder.param(PATH_PARAM_SITE_ID, "remote1");
		
		ResultActions result = mockMvc.perform(builder);
		
		result.andExpect(status().isOk());
		result.andExpect(jsonPath("length()", equalTo(6)));
		result.andExpect(jsonPath("[0]", equalTo(8)));
		result.andExpect(jsonPath("[1]", equalTo(2)));
		result.andExpect(jsonPath("[2]", equalTo(3)));
		result.andExpect(jsonPath("[3]", equalTo(14)));
		result.andExpect(jsonPath("[4]", equalTo(6)));
		result.andExpect(jsonPath("[5]", equalTo(4)));
	}
	
	@Test
	@Sql(scripts = { "classpath:mgt_site_info.sql", "classpath:mgt_receiver_reconcile.sql",
	        "classpath:mgt_receiver_reconcile_table_summary.sql" }, config = @SqlConfig(dataSource = MGT_DATASOURCE_NAME, transactionManager = MGT_TX_MGR))
	public void getReport_shouldReturnTheReconciliationCountTotals() throws Exception {
		MockHttpServletRequestBuilder builder = get(RestConstants.PATH_RECONCILE_REPORT + "/{" + PATH_VAR_REC_ID + "}",
		    "rec-1");
		
		ResultActions result = mockMvc.perform(builder);
		
		result.andExpect(status().isOk());
		result.andExpect(jsonPath("length()", equalTo(3)));
		result.andExpect(jsonPath("[0][0]", equalTo("patient")));
		result.andExpect(jsonPath("[0][1]", equalTo(0)));
		result.andExpect(jsonPath("[0][2]", equalTo(0)));
		result.andExpect(jsonPath("[0][3]", equalTo(0)));
		result.andExpect(jsonPath("[0][4]", equalTo(0)));
		result.andExpect(jsonPath("[0][5]", equalTo(0)));
		result.andExpect(jsonPath("[0][6]", equalTo(0)));
		result.andExpect(jsonPath("[1][0]", equalTo("person")));
		result.andExpect(jsonPath("[1][1]", equalTo(5)));
		result.andExpect(jsonPath("[1][2]", equalTo(1)));
		result.andExpect(jsonPath("[1][3]", equalTo(2)));
		result.andExpect(jsonPath("[1][4]", equalTo(9)));
		result.andExpect(jsonPath("[1][5]", equalTo(4)));
		result.andExpect(jsonPath("[1][6]", equalTo(3)));
		result.andExpect(jsonPath("[2][0]", equalTo("person_name")));
		result.andExpect(jsonPath("[2][1]", equalTo(6)));
		result.andExpect(jsonPath("[2][2]", equalTo(2)));
		result.andExpect(jsonPath("[2][3]", equalTo(2)));
		result.andExpect(jsonPath("[2][4]", equalTo(9)));
		result.andExpect(jsonPath("[2][5]", equalTo(3)));
		result.andExpect(jsonPath("[2][6]", equalTo(2)));
	}
	
	@Test
	@Sql(scripts = { "classpath:mgt_site_info.sql", "classpath:mgt_receiver_reconcile.sql",
	        "classpath:mgt_receiver_reconcile_table_summary.sql" }, config = @SqlConfig(dataSource = MGT_DATASOURCE_NAME, transactionManager = MGT_TX_MGR))
	public void getReport_shouldReturnTheReconciliationCountTotalsForTheSite() throws Exception {
		MockHttpServletRequestBuilder builder = get(RestConstants.PATH_RECONCILE_REPORT + "/{" + PATH_VAR_REC_ID + "}",
		    "rec-1");
		builder.param(PATH_PARAM_SITE_ID, "remote1");
		
		ResultActions result = mockMvc.perform(builder);
		
		result.andExpect(status().isOk());
		result.andExpect(jsonPath("length()", equalTo(3)));
		result.andExpect(jsonPath("[0].tableName", equalTo("person")));
		result.andExpect(jsonPath("[0].missingCount", equalTo(5)));
		result.andExpect(jsonPath("[0].missingSyncCount", equalTo(1)));
		result.andExpect(jsonPath("[0].missingErrorCount", equalTo(2)));
		result.andExpect(jsonPath("[0].undeletedCount", equalTo(9)));
		result.andExpect(jsonPath("[0].undeletedSyncCount", equalTo(4)));
		result.andExpect(jsonPath("[0].undeletedErrorCount", equalTo(3)));
		result.andExpect(jsonPath("[1].tableName", equalTo("patient")));
		result.andExpect(jsonPath("[1].missingCount", equalTo(0)));
		result.andExpect(jsonPath("[1].missingSyncCount", equalTo(0)));
		result.andExpect(jsonPath("[1].missingErrorCount", equalTo(0)));
		result.andExpect(jsonPath("[1].undeletedCount", equalTo(0)));
		result.andExpect(jsonPath("[1].undeletedSyncCount", equalTo(0)));
		result.andExpect(jsonPath("[1].undeletedErrorCount", equalTo(0)));
		result.andExpect(jsonPath("[2].tableName", equalTo("person_name")));
		result.andExpect(jsonPath("[2].missingCount", equalTo(3)));
		result.andExpect(jsonPath("[2].missingSyncCount", equalTo(1)));
		result.andExpect(jsonPath("[2].missingErrorCount", equalTo(1)));
		result.andExpect(jsonPath("[2].undeletedCount", equalTo(5)));
		result.andExpect(jsonPath("[2].undeletedSyncCount", equalTo(2)));
		result.andExpect(jsonPath("[2].undeletedErrorCount", equalTo(1)));
	}
	
}
