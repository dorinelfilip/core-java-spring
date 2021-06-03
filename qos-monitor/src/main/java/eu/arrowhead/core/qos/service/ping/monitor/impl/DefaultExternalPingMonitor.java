package eu.arrowhead.core.qos.service.ping.monitor.impl;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import javax.annotation.Resource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.Assert;
import org.springframework.web.util.UriComponents;

import eu.arrowhead.common.CommonConstants;
import eu.arrowhead.common.CoreCommonConstants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.dto.shared.IcmpPingRequestACK;
import eu.arrowhead.common.dto.shared.IcmpPingRequestDTO;
import eu.arrowhead.common.dto.shared.SystemRequestDTO;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.core.qos.QosMonitorConstants;
import eu.arrowhead.core.qos.dto.IcmpPingResponse;
import eu.arrowhead.core.qos.service.QoSMonitorDriver;
import eu.arrowhead.core.qos.service.ping.monitor.AbstractPingMonitor;
import eu.arrowhead.core.qos.service.ping.monitor.PingEventCollectorTask;
import eu.arrowhead.core.qos.service.ping.monitor.PingEventProcessor;

public class DefaultExternalPingMonitor extends AbstractPingMonitor{

	//=================================================================================================
	// members

	//-------------------------------------------------------------------------------------------------
	private static final int ICMP_TTL = 255;
	private static final int OVERHEAD_MULTIPLIER = 2;

	private final ThreadPoolExecutor threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);

	@Autowired
	private QoSMonitorDriver driver;

	@Autowired
	private PingEventProcessor processor;

	@Resource(name = QosMonitorConstants.EVENT_COLLECTOR)
	private PingEventCollectorTask eventCollector; 

	@Value(CoreCommonConstants.$QOS_MONITOR_PROVIDER_NAME_WD)
	private String externalPingMonitorName;

	@Value(CoreCommonConstants.$QOS_MONITOR_PROVIDER_ADDRESS_WD)
	private String externalPingMonitorAddress;

	@Value(CoreCommonConstants.$QOS_MONITOR_PROVIDER_PORT_WD)
	private int externalPingMonitorPort;

	@Value(CoreCommonConstants.$QOS_MONITOR_PROVIDER_PATH_WD)
	private String externalPingMonitorPath;

	@Value(CoreCommonConstants.$QOS_MONITOR_PROVIDER_SECURE_WD)
	private boolean pingMonitorSecure;

	private SystemRequestDTO pingMonitorSystem;

	private final Logger logger = LogManager.getLogger(DefaultExternalPingMonitor.class);

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public List<IcmpPingResponse> ping(final String address) {
		logger.debug("ping statred...");

		final int timeOut = calculateTimeOut();
		final UUID measurementProcessId = requestExternalMeasurement(address);

		final long startTime = System.currentTimeMillis();
		final long measurementExpiryTime = startTime + timeOut;

		try {

			return processor.processEvents(measurementProcessId, measurementExpiryTime);

		} catch (final Exception ex) {

			logger.debug(ex.getMessage());
		}

		return null;
	}

	//-------------------------------------------------------------------------------------------------
	public void init() {
		logger.debug("initPingMonitorProvider started...");

		pingMonitorSystem = getPingMonitorSystemRequestDTO();

		threadPool.execute(eventCollector);

		driver.checkPingMonitorProviderEchoUri(createPingMonitorProviderEchoUri());
		driver.subscribeToExternalPingMonitorEvents(pingMonitorSystem);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private UUID requestExternalMeasurement(final String address) {
		logger.debug("requestExternalMeasurement started...");

		try {

			final IcmpPingRequestACK acknowledgedMeasurmentRequest = driver.requestExternalPingMonitorService(createPingMonitorProviderUri(), createIcmpPingRequest(address));
			validateAcknowledgedMeasurmentRequest(acknowledgedMeasurmentRequest);

			final UUID startedExternalMeasurementProcessId = acknowledgedMeasurmentRequest.getExternalMeasurementUuid();
			logger.info("IcmpPingRequestACK received, with process id: " + startedExternalMeasurementProcessId);

			return startedExternalMeasurementProcessId;

		} catch (final Exception ex) {
			logger.info(ex);

			throw new ArrowheadException("External Ping Monitor is not available at: " + address );
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateAcknowledgedMeasurmentRequest(final IcmpPingRequestACK acknowledgedMeasurmentRequest) {
		logger.debug("validateAcknowledgedMeasurmentRequest started...");

		try {
			Assert.notNull(acknowledgedMeasurmentRequest, "IcmpPingRequestACK is null");
			Assert.notNull(acknowledgedMeasurmentRequest.getAckOk(), "IcmpPingRequestACK.ackOk is null");
			Assert.isTrue(acknowledgedMeasurmentRequest.getAckOk().equalsIgnoreCase("OK"), "IcmpPingRequestACK is null");

		} catch (final Exception ex) {
			logger.warn("External pingMonitorProvider replied invalid ack : " + ex);

			throw new ArrowheadException("External pingMonitorProvider replied invalid ack", ex);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private IcmpPingRequestDTO createIcmpPingRequest(final String address) {
		logger.debug("createIcmpPingRequest started...");

		final IcmpPingRequestDTO request = new IcmpPingRequestDTO();
		request.setHost(address);
		request.setPacketSize(pingMeasurementProperties.getPacketSize());
		request.setTimeout(Long.valueOf(pingMeasurementProperties.getTimeout()));
		request.setTimeToRepeat(pingMeasurementProperties.getTimeToRepeat());
		request.setTtl(ICMP_TTL);

		return request;
	}

	//-------------------------------------------------------------------------------------------------
	private int calculateTimeOut() {

		final int singlePingTimeOut = pingMeasurementProperties.getTimeout();
		final int timesToRepeatPing = pingMeasurementProperties.getTimeToRepeat();

		return singlePingTimeOut * timesToRepeatPing * OVERHEAD_MULTIPLIER;
	}

	//-------------------------------------------------------------------------------------------------
	private UriComponents createPingMonitorProviderUri() {
		logger.debug("createPingMonitorProviderUri started...");

		return Utilities.createURI(pingMonitorSecure ? CommonConstants.HTTPS : CommonConstants.HTTP, externalPingMonitorAddress, externalPingMonitorPort, externalPingMonitorPath);
	}

	//-------------------------------------------------------------------------------------------------
	private UriComponents createPingMonitorProviderEchoUri() {
		logger.debug("createPingMonitorProviderEchoUri started...");

		return Utilities.createURI(pingMonitorSecure ? CommonConstants.HTTPS : CommonConstants.HTTP, externalPingMonitorAddress, externalPingMonitorPort, CommonConstants.ECHO_URI);
	}

	//-------------------------------------------------------------------------------------------------
	private SystemRequestDTO getPingMonitorSystemRequestDTO() {
		logger.debug("getPingMonitorSystemRequestDTO started...");

		final SystemRequestDTO system = new SystemRequestDTO();
		system.setSystemName(externalPingMonitorName);
		system.setAddress(externalPingMonitorAddress);
		system.setPort(externalPingMonitorPort);
		system.setMetadata(null);

		return system;
	}

}
