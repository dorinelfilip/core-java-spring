package eu.arrowhead.core.gatekeeper;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.arrowhead.common.CommonConstants;
import eu.arrowhead.common.database.entity.Cloud;
import eu.arrowhead.common.database.entity.CloudGatekeeperRelay;
import eu.arrowhead.common.database.entity.CloudGatewayRelay;
import eu.arrowhead.common.database.entity.Relay;
import eu.arrowhead.common.dto.internal.CloudWithRelaysListResponseDTO;
import eu.arrowhead.common.dto.internal.CloudWithRelaysResponseDTO;
import eu.arrowhead.common.dto.internal.DTOConverter;
import eu.arrowhead.common.dto.internal.RelayType;
import eu.arrowhead.common.dto.internal.SystemAddressSetRelayResponseDTO;
import eu.arrowhead.common.dto.shared.CloudRequestDTO;
import eu.arrowhead.core.gatekeeper.database.service.GatekeeperDBService;
import eu.arrowhead.core.gatekeeper.service.GatekeeperService;
import eu.arrowhead.core.gatekeeper.service.GatekeeperServiceTestContext;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = GatekeeperMain.class)
@ContextConfiguration(classes = { GatekeeperServiceTestContext.class })
public class GatekeeperControllerQoSTest {
	
	//=================================================================================================
	// members
	
	private static final String PULL_CLOUDS_URI = CommonConstants.GATEKEEPER_URI + CommonConstants.OP_GATEKEEPER_PULL_CLOUDS_SERVICE;
	private static final String COLLECT_SYSTEM_ADDRESSES_URI = CommonConstants.GATEKEEPER_URI + CommonConstants.OP_GATEKEEPER_COLLECT_SYSTEM_ADDRESSES_SERVICE;
	
	
	@Autowired
	private WebApplicationContext wac;
	
	private MockMvc mockMvc;
	
	@Autowired
	private ObjectMapper objectMapper;
	
	@MockBean(name = "mockGatekeeperService") 
	private GatekeeperService gatekeeperService;
	
	@MockBean(name = "mockGatekeeperDBService") 
	private GatekeeperDBService gatekeeperDBService;
	
	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	@Before
	public void setup() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test
	public void testPullCloudsOk() throws Exception {
		final int amountOfClouds = 5;
		final CloudWithRelaysListResponseDTO dto = createCloudWithRelaysListResponseDTOForDBMocking(amountOfClouds, RelayType.GATEKEEPER_RELAY, RelayType.GATEWAY_RELAY, false);
		
		when(gatekeeperDBService.getCloudsResponse(anyInt(), anyInt(), any(), any())).thenReturn(dto);
		
		final MvcResult response = this.mockMvc.perform(get(PULL_CLOUDS_URI)
				   .accept(MediaType.APPLICATION_JSON))
				   .andExpect(status().isOk())
				   .andReturn();
		
		final CloudWithRelaysListResponseDTO responseBody = objectMapper.readValue(response.getResponse().getContentAsString(), CloudWithRelaysListResponseDTO.class);
		assertEquals(amountOfClouds, responseBody.getCount());
		assertEquals(RelayType.GATEKEEPER_RELAY, responseBody.getData().get(0).getGatekeeperRelays().get(0).getType());
		assertEquals(RelayType.GATEWAY_RELAY, responseBody.getData().get(0).getGatewayRelays().get(0).getType());
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCollectSystemAddressesOfNeighborCloudOk() throws Exception {
		final CloudRequestDTO requestDTO = new CloudRequestDTO();
		requestDTO.setName("test-cloud");
		requestDTO.setOperator("test-operator");
		
		final SystemAddressSetRelayResponseDTO responseDTO = new SystemAddressSetRelayResponseDTO(Set.of("10.10.10.10", "20.20.20.20"));
		
		when(gatekeeperService.initSystemAddressCollection(any())).thenReturn(responseDTO);
		
		final MvcResult response = this.mockMvc.perform(post(COLLECT_SYSTEM_ADDRESSES_URI)
					.content(objectMapper.writeValueAsBytes(requestDTO))
				   .contentType(MediaType.APPLICATION_JSON)
				   .accept(MediaType.APPLICATION_JSON))
				   .andExpect(status().isOk())
				   .andReturn();
		
		SystemAddressSetRelayResponseDTO responseBody = objectMapper.readValue(response.getResponse().getContentAsString(), SystemAddressSetRelayResponseDTO.class);
		assertEquals(responseDTO.getAddresses().size(), responseBody.getAddresses().size());
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCollectSystemAddressesOfNeighborCloudNullCloudName() throws Exception {
		final CloudRequestDTO requestDTO = new CloudRequestDTO();
		requestDTO.setName(null);
		requestDTO.setOperator("test-operator");
		
		final SystemAddressSetRelayResponseDTO responseDTO = new SystemAddressSetRelayResponseDTO(Set.of("10.10.10.10", "20.20.20.20"));
		
		when(gatekeeperService.initSystemAddressCollection(any())).thenReturn(responseDTO);
		
		this.mockMvc.perform(post(COLLECT_SYSTEM_ADDRESSES_URI)
					.content(objectMapper.writeValueAsBytes(requestDTO))
				   .contentType(MediaType.APPLICATION_JSON)
				   .accept(MediaType.APPLICATION_JSON))
				   .andExpect(status().isBadRequest());
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCollectSystemAddressesOfNeighborCloudBlankCloudName() throws Exception {
		final CloudRequestDTO requestDTO = new CloudRequestDTO();
		requestDTO.setName("");
		requestDTO.setOperator("test-operator");
		
		final SystemAddressSetRelayResponseDTO responseDTO = new SystemAddressSetRelayResponseDTO(Set.of("10.10.10.10", "20.20.20.20"));
		
		when(gatekeeperService.initSystemAddressCollection(any())).thenReturn(responseDTO);
		
		this.mockMvc.perform(post(COLLECT_SYSTEM_ADDRESSES_URI)
					.content(objectMapper.writeValueAsBytes(requestDTO))
				   .contentType(MediaType.APPLICATION_JSON)
				   .accept(MediaType.APPLICATION_JSON))
				   .andExpect(status().isBadRequest());
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCollectSystemAddressesOfNeighborCloudNullCloudOperator() throws Exception {
		final CloudRequestDTO requestDTO = new CloudRequestDTO();
		requestDTO.setName("test-name");
		requestDTO.setOperator(null);
		
		final SystemAddressSetRelayResponseDTO responseDTO = new SystemAddressSetRelayResponseDTO(Set.of("10.10.10.10", "20.20.20.20"));
		
		when(gatekeeperService.initSystemAddressCollection(any())).thenReturn(responseDTO);
		
		this.mockMvc.perform(post(COLLECT_SYSTEM_ADDRESSES_URI)
					.content(objectMapper.writeValueAsBytes(requestDTO))
				   .contentType(MediaType.APPLICATION_JSON)
				   .accept(MediaType.APPLICATION_JSON))
				   .andExpect(status().isBadRequest());
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCollectSystemAddressesOfNeighborCloudBlankCloudOperator() throws Exception {
		final CloudRequestDTO requestDTO = new CloudRequestDTO();
		requestDTO.setName("test-name");
		requestDTO.setOperator("");
		
		final SystemAddressSetRelayResponseDTO responseDTO = new SystemAddressSetRelayResponseDTO(Set.of("10.10.10.10", "20.20.20.20"));
		
		when(gatekeeperService.initSystemAddressCollection(any())).thenReturn(responseDTO);
		
		this.mockMvc.perform(post(COLLECT_SYSTEM_ADDRESSES_URI)
					.content(objectMapper.writeValueAsBytes(requestDTO))
				   .contentType(MediaType.APPLICATION_JSON)
				   .accept(MediaType.APPLICATION_JSON))
				   .andExpect(status().isBadRequest());
	}
	
	//=================================================================================================
	// assistant methods
	
	//-------------------------------------------------------------------------------------------------
	private CloudWithRelaysListResponseDTO createCloudWithRelaysListResponseDTOForDBMocking(final int amountOfClouds, final RelayType gatekeeperRelayType, final RelayType gatewayRelayType, 
																							final boolean gatewayRelayExclusive) {
		final List<CloudWithRelaysResponseDTO> cloudDTOList = new ArrayList<>();
		
		for (int i = 1; i <= amountOfClouds; ++i) {
			final Cloud cloud = new Cloud();
			cloud.setId(i);
			cloud.setOperator("testOperator" + i);
			cloud.setName("testName" + i);
			cloud.setSecure(true);
			cloud.setNeighbor(true);
			cloud.setOwnCloud(false);
			cloud.setAuthenticationInfo("testAuthenticationInfo" + i);
			cloud.setCreatedAt(ZonedDateTime.now());
			cloud.setUpdatedAt(ZonedDateTime.now());
			
			final Relay gatekeeperRelay = new Relay();
			gatekeeperRelay.setId(i);
			gatekeeperRelay.setAddress("testAddress" + i);
			gatekeeperRelay.setPort(i * 1000);
			gatekeeperRelay.setSecure(true);
			gatekeeperRelay.setExclusive(false);
			gatekeeperRelay.setType(gatekeeperRelayType);
			gatekeeperRelay.setCreatedAt(ZonedDateTime.now());
			gatekeeperRelay.setUpdatedAt(ZonedDateTime.now());
			
			final Relay gatewayRelay = new Relay();
			gatewayRelay.setId(i);
			gatewayRelay.setAddress("testAddress" + i);
			gatewayRelay.setPort(i * 1000);
			gatewayRelay.setSecure(true);
			gatewayRelay.setExclusive(gatewayRelayExclusive);
			gatewayRelay.setType(gatewayRelayType);
			gatewayRelay.setCreatedAt(ZonedDateTime.now());
			gatewayRelay.setUpdatedAt(ZonedDateTime.now());
			
			cloud.getGatekeeperRelays().add(new CloudGatekeeperRelay(cloud, gatekeeperRelay));
			cloud.getGatewayRelays().add(new CloudGatewayRelay(cloud, gatewayRelay));
			
			cloudDTOList.add(DTOConverter.convertCloudToCloudWithRelaysResponseDTO(cloud));
		}
		
		return new CloudWithRelaysListResponseDTO(cloudDTOList, cloudDTOList.size());
	}

}
