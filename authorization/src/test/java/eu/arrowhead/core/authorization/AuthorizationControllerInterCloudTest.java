package eu.arrowhead.core.authorization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.arrowhead.common.CommonConstants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.database.entity.Cloud;
import eu.arrowhead.common.database.entity.AuthorizationInterCloud;
import eu.arrowhead.common.database.entity.ServiceDefinition;
import eu.arrowhead.common.dto.DTOConverter;
import eu.arrowhead.common.dto.AuthorizationInterCloudCheckRequestDTO;
import eu.arrowhead.common.dto.AuthorizationInterCloudCheckResponseDTO;
import eu.arrowhead.common.dto.AuthorizationInterCloudListResponseDTO;
import eu.arrowhead.common.dto.AuthorizationInterCloudRequestDTO;
import eu.arrowhead.common.dto.AuthorizationInterCloudResponseDTO;
import eu.arrowhead.core.authorization.database.service.AuthorizationDBService;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = AuthorizationMain.class)
@ContextConfiguration(classes = AuthorizationDBServiceTestContext.class)
public class AuthorizationControllerInterCloudTest {
	
	//=================================================================================================
	// members
	
	private static final String INTER_CLOUD_AUTHORIZATION_MGMT_URI = "/authorization/mgmt/intercloud";
	private static final String INTER_CLOUD_AUTHORIZATION_CHECK_URI = "/authorization/intercloud/check";
	
	@Autowired
	private WebApplicationContext wac;
	
	private MockMvc mockMvc;
	
	@Autowired
	private ObjectMapper objectMapper;
	
	@MockBean(name = "mockAuthorizationDBService")
	private AuthorizationDBService authorizationDBService;
	
	private static final ZonedDateTime zdTime = Utilities.parseUTCStringToLocalZonedDateTime("2222-12-12 12:00:00");
	
	//-------------------------------------------------------------------------------------------------
	@Before
	public void setup() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
	}
	
	//=================================================================================================
	// Tests of getAuthorizationInterClouds
	
	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetAuthorizationInterCloudsWithoutParameters() throws Exception {
		final int numOfEntries = 4;
		final AuthorizationInterCloudListResponseDTO dto = DTOConverter.convertAuthorizationInterCloudListToAuthorizationInterCloudListResponseDTO(
																																			createPageForMockingAuthorizationDBService(numOfEntries));
		when(authorizationDBService.getAuthorizationInterCloudEntriesResponse(anyInt(), anyInt(), any(), any())).thenReturn(dto);
		
		final MvcResult response = this.mockMvc.perform(get(INTER_CLOUD_AUTHORIZATION_MGMT_URI)
											   .accept(MediaType.APPLICATION_JSON))
											   .andExpect(status().isOk())
											   .andReturn();
		
		final AuthorizationInterCloudListResponseDTO responseBody = objectMapper.readValue(response.getResponse().getContentAsByteArray(), AuthorizationInterCloudListResponseDTO.class);
		assertEquals(numOfEntries, responseBody.getData().size());
		assertEquals(numOfEntries, responseBody.getCount());
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetAuthorizationInterCloudsWithPageAndSizeParameter() throws Exception {
		final int numOfEntries = 4;
		final AuthorizationInterCloudListResponseDTO dto = DTOConverter.convertAuthorizationInterCloudListToAuthorizationInterCloudListResponseDTO(
																																			createPageForMockingAuthorizationDBService(numOfEntries));
		when(authorizationDBService.getAuthorizationInterCloudEntriesResponse(anyInt(), anyInt(), any(), any())).thenReturn(dto);
		
		final MvcResult response = this.mockMvc.perform(get(INTER_CLOUD_AUTHORIZATION_MGMT_URI)
											   .param(CommonConstants.REQUEST_PARAM_PAGE, "0")
											   .param(CommonConstants.REQUEST_PARAM_ITEM_PER_PAGE, String.valueOf(numOfEntries))
											   .accept(MediaType.APPLICATION_JSON))
											   .andExpect(status().isOk())
											   .andReturn();
		
		final AuthorizationInterCloudListResponseDTO responseBody = objectMapper.readValue(response.getResponse().getContentAsByteArray(), AuthorizationInterCloudListResponseDTO.class);
		assertEquals(numOfEntries, responseBody.getData().size());
		assertEquals(numOfEntries, responseBody.getCount());
	}
	
	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("squid:S2699")
	@Test
	public void testGetAuthorizationInterCloudsWithNullPageButDefinedSizeParameter() throws Exception {
		this.mockMvc.perform(get(INTER_CLOUD_AUTHORIZATION_MGMT_URI)
					.param(CommonConstants.REQUEST_PARAM_ITEM_PER_PAGE, String.valueOf(5))
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest());
	}
	
	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("squid:S2699")
	@Test
	public void testGetAuthorizationInterCloudsWithDefinedPageButNullSizeParameter() throws Exception {
		this.mockMvc.perform(get(INTER_CLOUD_AUTHORIZATION_MGMT_URI)
					.param(CommonConstants.REQUEST_PARAM_PAGE, "0")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest());
	}
	
	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("squid:S2699")
	@Test
	public void testGetAuthorizationInterCloudsWithInvalidSortDirectionFlagParameter() throws Exception {
		this.mockMvc.perform(get(INTER_CLOUD_AUTHORIZATION_MGMT_URI)
					.param(CommonConstants.REQUEST_PARAM_DIRECTION, "invalid")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest());
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetAuthorizationInterCloudsWithExistingId() throws Exception {
		final AuthorizationInterCloudResponseDTO dto = DTOConverter.convertAuthorizationInterCloudToAuthorizationInterCloudResponseDTO(createPageForMockingAuthorizationDBService(1).getContent().
																																	   get(0));
		when(authorizationDBService.getAuthorizationInterCloudEntryByIdResponse(anyLong())).thenReturn(dto);
		
		final MvcResult response = this.mockMvc.perform(get(INTER_CLOUD_AUTHORIZATION_MGMT_URI + "/1")
											   .accept(MediaType.APPLICATION_JSON))
											   .andExpect(status().isOk())
											   .andReturn();
		
		final AuthorizationInterCloudResponseDTO responseBody = objectMapper.readValue(response.getResponse().getContentAsByteArray(), AuthorizationInterCloudResponseDTO.class);
		assertEquals("testCloudName", responseBody.getCloud().getName());
	}
	
	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("squid:S2699")
	@Test
	public void testGetAuthorizationInterCloudsWithInvalidId() throws Exception {
		this.mockMvc.perform(get(INTER_CLOUD_AUTHORIZATION_MGMT_URI + "/0")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest());
	}
	
	//=================================================================================================
	// Test of removeAuthorizationInterCloudById
	
	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("squid:S2699")
	@Test
	public void testRemoveAuthorizationInterCloudByIdWithExistingId() throws Exception {
		this.mockMvc.perform(delete(INTER_CLOUD_AUTHORIZATION_MGMT_URI + "/1")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk());
	}
	
	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("squid:S2699")
	@Test
	public void testRemoveAuthorizationInterCloudByIdWithInvalidId() throws Exception {
		this.mockMvc.perform(delete(INTER_CLOUD_AUTHORIZATION_MGMT_URI + "/0")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest());
	}
	
	//=================================================================================================
	// Test of registerAuthorizationInterCloud
	
	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("squid:S2699")
	@Test
	public void testRegisterAuthorizationInterCloudWithInvalidCloudId() throws Exception {
		this.mockMvc.perform(post(INTER_CLOUD_AUTHORIZATION_MGMT_URI)
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsBytes(new AuthorizationInterCloudRequestDTO(0L, createIdList(1, 2))))
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest());
	}
	
	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("squid:S2699")
	@Test
	public void testRegisterAuthorizationInterCloudWithEmptyServiceDefinitionIdList() throws Exception {
		this.mockMvc.perform(post(INTER_CLOUD_AUTHORIZATION_MGMT_URI)
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsBytes(new AuthorizationInterCloudRequestDTO(1L, null)))
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest());
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterAuthorizationInterCloudDBCall() throws Exception {
		final Page<AuthorizationInterCloud> entries = createPageForMockingAuthorizationDBService(1);
		when(authorizationDBService.createAuthorizationInterCloudResponse(anyLong(),any())).thenReturn(DTOConverter.convertAuthorizationInterCloudListToAuthorizationInterCloudListResponseDTO(entries));
		
		final MvcResult response = this.mockMvc.perform(post(INTER_CLOUD_AUTHORIZATION_MGMT_URI)
											   .contentType(MediaType.APPLICATION_JSON)
											   .content(objectMapper.writeValueAsBytes(new AuthorizationInterCloudRequestDTO(1L,createIdList(1, 1))))
											   .accept(MediaType.APPLICATION_JSON))
											   .andExpect(status().isCreated())
											   .andReturn();
		
		final AuthorizationInterCloudListResponseDTO responseBody = objectMapper.readValue(response.getResponse().getContentAsByteArray(), AuthorizationInterCloudListResponseDTO.class);
		assertEquals("testCloudName", responseBody.getData().get(0).getCloud() .getName());
		assertEquals(1, responseBody.getData().size());
		assertEquals(1, responseBody.getCount());
	}
	
	//=================================================================================================
	// Test of checkAuthorizationInterCloudRequest
	
	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("squid:S2699")
	@Test
	public void testCheckAuthorizationInterCloudRequestWithInvalidCloudId() throws Exception {
		this.mockMvc.perform(post(INTER_CLOUD_AUTHORIZATION_CHECK_URI)
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsBytes(new AuthorizationInterCloudCheckRequestDTO(0L, 1L)))
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest());
	}
	
	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("squid:S2699")
	@Test
	public void testCheckAuthorizationInterCloudRequestWithNullCloudId() throws Exception {
		this.mockMvc.perform(post(INTER_CLOUD_AUTHORIZATION_CHECK_URI)
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsBytes(new AuthorizationInterCloudCheckRequestDTO(null, 1L)))
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest());
	}
	
	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("squid:S2699")
	@Test
	public void testCheckAuthorizationInterCloudRequestWithInvalidServiceDefinitionId() throws Exception {
		this.mockMvc.perform(post(INTER_CLOUD_AUTHORIZATION_CHECK_URI)
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsBytes(new AuthorizationInterCloudCheckRequestDTO(1L, 0L)))
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest());
	}
	
	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("squid:S2699")
	@Test
	public void testCheckAuthorizationInterCloudRequestWithNullServiceDefinitionId() throws Exception {
		this.mockMvc.perform(post(INTER_CLOUD_AUTHORIZATION_CHECK_URI)
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsBytes(new AuthorizationInterCloudCheckRequestDTO(1L, null)))
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest());
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCheckAuthorizationInterCloudRequestDBCall() throws Exception {
		final long cloudId = 1L;
		final long serviceDefinitionId =  3L;
		when(authorizationDBService.checkAuthorizationInterCloudResponse(anyLong(), anyLong())).thenReturn(new AuthorizationInterCloudCheckResponseDTO(cloudId, serviceDefinitionId, true));
		
		final MvcResult response = this.mockMvc.perform(post(INTER_CLOUD_AUTHORIZATION_CHECK_URI)
											   .contentType(MediaType.APPLICATION_JSON)
											   .content(objectMapper.writeValueAsBytes(new AuthorizationInterCloudCheckRequestDTO(cloudId, serviceDefinitionId)))
											   .accept(MediaType.APPLICATION_JSON))
											   .andExpect(status().isOk())
											   .andReturn();
		
		final AuthorizationInterCloudCheckResponseDTO responseBody = objectMapper.readValue(response.getResponse().getContentAsByteArray(), AuthorizationInterCloudCheckResponseDTO.class);
		assertTrue(responseBody.getCloudIdAuthorizationState());
	}
	
	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private List<Long> createIdList(final int firstNum, final int lastNum) {
		final List<Long> idList = new ArrayList<>(lastNum);
		for (int i = firstNum; i <= lastNum; ++i) {
			idList.add((long) i);
		}
		
		return idList;
	}
	
	//-------------------------------------------------------------------------------------------------
	private Page<AuthorizationInterCloud> createPageForMockingAuthorizationDBService(final int numberOfRequestedEntry) {
		final List<AuthorizationInterCloud> entries = new ArrayList<>(numberOfRequestedEntry);
		final Cloud cloud = getValidTestCloud();
		for (int i = 1; i <= numberOfRequestedEntry; ++i) {
			final ServiceDefinition serviceDefinition = new ServiceDefinition("testService"+i);
			serviceDefinition.setId(i);
			serviceDefinition.setCreatedAt(zdTime);
			serviceDefinition.setUpdatedAt(zdTime);
			final AuthorizationInterCloud entry = new AuthorizationInterCloud(cloud, serviceDefinition);
			entry.setId(i);
			entry.setCreatedAt(zdTime);
			entry.setUpdatedAt(zdTime);
			entries.add(entry);
		}
		
		return new PageImpl<>(entries);
	}
	
	//-------------------------------------------------------------------------------------------------
	private static Cloud getValidTestCloud() {
		final boolean secure = true;
		final boolean neighbor = false;
		final boolean ownCloud = true;
		
		final Cloud cloud = new Cloud("testOperator", "testCloudName", secure, neighbor, ownCloud);
		cloud.setId(1);
		cloud.setCreatedAt(zdTime);
		cloud.setUpdatedAt(zdTime);

		return cloud;
	}	
}