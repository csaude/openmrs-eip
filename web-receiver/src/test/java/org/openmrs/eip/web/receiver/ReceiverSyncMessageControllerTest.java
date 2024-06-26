package org.openmrs.eip.web.receiver;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.openmrs.eip.app.SyncConstants.MGT_DATASOURCE_NAME;
import static org.openmrs.eip.app.SyncConstants.MGT_TX_MGR;
import static org.openmrs.eip.app.route.TestUtils.getEntity;
import static org.openmrs.eip.web.RestConstants.PARAM_GRP_PROP;
import static org.openmrs.eip.web.RestConstants.PATH_RECEIVER_SYNC_MSG;
import static org.openmrs.eip.web.RestConstants.PATH_VAR_ID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.openmrs.eip.app.management.entity.receiver.SiteInfo;
import org.openmrs.eip.component.model.PatientModel;
import org.openmrs.eip.component.model.PersonModel;
import org.openmrs.eip.component.model.VisitModel;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

public class ReceiverSyncMessageControllerTest extends BaseReceiverWebTest {
	
	@Test
	@Sql(scripts = { "classpath:mgt_site_info.sql",
	        "classpath:mgt_receiver_sync_msg.sql" }, config = @SqlConfig(dataSource = MGT_DATASOURCE_NAME, transactionManager = MGT_TX_MGR))
	public void get_shouldGetAllSyncMessages() throws Exception {
		MockHttpServletRequestBuilder builder = get(PATH_RECEIVER_SYNC_MSG);
		ResultActions result = mockMvc.perform(builder);
		result.andExpect(status().isOk());
		result.andExpect(jsonPath("length()", equalTo(2)));
		result.andExpect(jsonPath("count", equalTo(6)));
		result.andExpect(jsonPath("items.length()", equalTo(6)));
	}
	
	@Test
	@Sql(scripts = { "classpath:mgt_site_info.sql",
	        "classpath:mgt_receiver_sync_msg.sql" }, config = @SqlConfig(dataSource = MGT_DATASOURCE_NAME, transactionManager = MGT_TX_MGR))
	public void get_shouldGetTheSyncMessageMatchingTheSpecifiedId() throws Exception {
		MockHttpServletRequestBuilder builder = get(PATH_RECEIVER_SYNC_MSG + "/{" + PATH_VAR_ID + "}", 2L);
		ResultActions result = mockMvc.perform(builder);
		result.andExpect(status().isOk());
		result.andExpect(jsonPath("messageUuid", equalTo("27beb8bd-287c-47f2-9786-a7b98c933c05")));
	}
	
	@Test
	@Sql(scripts = { "classpath:mgt_site_info.sql",
	        "classpath:mgt_receiver_sync_msg_web.sql" }, config = @SqlConfig(dataSource = MGT_DATASOURCE_NAME, transactionManager = MGT_TX_MGR))
	public void get_shouldGetTheSyncMessagesGroupedBySite() throws Exception {
		MockHttpServletRequestBuilder builder = get(PATH_RECEIVER_SYNC_MSG);
		builder.param(PARAM_GRP_PROP, "site.name");
		ResultActions result = mockMvc.perform(builder);
		result.andDo(MockMvcResultHandlers.print());
		result.andExpect(status().isOk());
		result.andExpect(jsonPath("length()", equalTo(2)));
		result.andExpect(jsonPath("count", equalTo(4)));
		result.andExpect(jsonPath("items.length()", equalTo(2)));
		result.andExpect(jsonPath("items.['" + getEntity(SiteInfo.class, 1L).getName() + "']", equalTo(3)));
		result.andExpect(jsonPath("items.['" + getEntity(SiteInfo.class, 2L).getName() + "']", equalTo(1)));
		result.andExpect(jsonPath("items.['" + getEntity(SiteInfo.class, 3L).getName() + "']").doesNotHaveJsonPath());
	}
	
	@Test
	@Sql(scripts = { "classpath:mgt_site_info.sql",
	        "classpath:mgt_receiver_sync_msg_web.sql" }, config = @SqlConfig(dataSource = MGT_DATASOURCE_NAME, transactionManager = MGT_TX_MGR))
	public void get_shouldGetTheSyncMessagesGroupedByEntity() throws Exception {
		MockHttpServletRequestBuilder builder = get(PATH_RECEIVER_SYNC_MSG);
		builder.param(PARAM_GRP_PROP, "modelClassName");
		ResultActions result = mockMvc.perform(builder);
		result.andDo(MockMvcResultHandlers.print());
		result.andExpect(status().isOk());
		result.andExpect(jsonPath("length()", equalTo(2)));
		result.andExpect(jsonPath("count", equalTo(4)));
		result.andExpect(jsonPath("items.length()", equalTo(2)));
		result.andExpect(jsonPath("items.['" + PersonModel.class.getName() + "']", equalTo(3)));
		result.andExpect(jsonPath("items.['" + VisitModel.class.getName() + "']", equalTo(1)));
		result.andExpect(jsonPath("items.['" + PatientModel.class.getName() + "']").doesNotHaveJsonPath());
	}
	
}
