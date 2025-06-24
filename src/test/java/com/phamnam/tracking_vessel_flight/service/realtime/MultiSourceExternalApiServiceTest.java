package com.phamnam.tracking_vessel_flight.service.realtime;

import com.phamnam.tracking_vessel_flight.dto.request.AircraftTrackingRequest;
import com.phamnam.tracking_vessel_flight.dto.request.VesselTrackingRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MultiSourceExternalApiServiceTest {

    @Mock
    private ExternalApiService externalApiService;

    @Mock
    private DataFusionService dataFusionService;

    @Mock
    private RealTimeDataProcessor dataProcessor;

    @Mock
    private RawDataStorageService rawDataStorageService;

    @Mock
    private ChinaportsApiService chinaportsApiService;

    @Mock
    private MarineTrafficV2ApiService marineTrafficV2ApiService;

    @Mock
    private AdsbExchangeApiService adsbExchangeApiService;

    @Mock
    private VesselFinderApiService vesselFinderApiService;

    @InjectMocks
    private MultiSourceExternalApiService multiSourceExternalApiService;

    private List<AircraftTrackingRequest> sampleAircraftData;
    private List<VesselTrackingRequest> sampleVesselData;

    @BeforeEach
    void setUp() {
        setupSampleAircraftData();
        setupSampleVesselData();
    }

    private void setupSampleAircraftData() {
        sampleAircraftData = Arrays.asList(
                AircraftTrackingRequest.builder()
                        .hexident("ABC123")
                        .latitude(40.7128)
                        .longitude(-74.0060)
                        .altitude(35000)
                        .groundSpeed(500)
                        .callsign("UAL123")
                        .timestamp(LocalDateTime.now())
                        .dataQuality(0.95)
                        .build(),
                AircraftTrackingRequest.builder()
                        .hexident("DEF456")
                        .latitude(34.0522)
                        .longitude(-118.2437)
                        .altitude(40000)
                        .groundSpeed(550)
                        .callsign("DAL456")
                        .timestamp(LocalDateTime.now())
                        .dataQuality(0.90)
                        .build());
    }

    private void setupSampleVesselData() {
        sampleVesselData = Arrays.asList(
                VesselTrackingRequest.builder()
                        .mmsi("123456789")
                        .latitude(40.7128)
                        .longitude(-74.0060)
                        .speed(15.5)
                        .course(180)
                        .vesselName("Container Ship 1")
                        .timestamp(LocalDateTime.now())
                        .dataQuality(0.95)
                        .build(),
                VesselTrackingRequest.builder()
                        .mmsi("987654321")
                        .latitude(34.0522)
                        .longitude(-118.2437)
                        .speed(12.0)
                        .course(90)
                        .vesselName("Cargo Ship 2")
                        .timestamp(LocalDateTime.now())
                        .dataQuality(0.88)
                        .build());
    }

    @Test
    void collectAllAircraftData_SuccessfulCollection_ReturnsData() {
        // Arrange
        when(externalApiService.fetchAircraftData())
                .thenReturn(CompletableFuture.completedFuture(sampleAircraftData.subList(0, 1)));
        when(adsbExchangeApiService.fetchAircraftData())
                .thenReturn(CompletableFuture.completedFuture(sampleAircraftData.subList(1, 2)));
        when(dataFusionService.mergeAircraftData(any()))
                .thenReturn(sampleAircraftData);

        // Act
        CompletableFuture<List<AircraftTrackingRequest>> result = multiSourceExternalApiService
                .collectAllAircraftData();

        // Assert
        assertNotNull(result);
        List<AircraftTrackingRequest> aircraftData = result.join();
        assertEquals(2, aircraftData.size());
        verify(rawDataStorageService, times(2)).storeRawAircraftData(anyString(), anyList(), anyString(), anyLong());
        verify(dataFusionService).mergeAircraftData(any());
    }

    @Test
    void collectAllAircraftData_OneSourceReturnsEmpty_ReturnsDataFromWorkingSources() {
        // Arrange
        when(externalApiService.fetchAircraftData())
                .thenReturn(CompletableFuture.completedFuture(sampleAircraftData.subList(0, 1)));
        when(adsbExchangeApiService.fetchAircraftData())
                .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));
        when(dataFusionService.mergeAircraftData(any()))
                .thenReturn(sampleAircraftData.subList(0, 1));

        // Act
        CompletableFuture<List<AircraftTrackingRequest>> result = multiSourceExternalApiService
                .collectAllAircraftData();

        // Assert
        List<AircraftTrackingRequest> aircraftData = result.join();
        assertEquals(1, aircraftData.size());
        verify(rawDataStorageService, times(1)).storeRawAircraftData(anyString(), anyList(), anyString(), anyLong());
        verify(dataFusionService).mergeAircraftData(any());
    }

    @Test
    void collectAllAircraftData_AllSourcesReturnEmpty_ReturnsEmptyList() {
        // Arrange
        when(externalApiService.fetchAircraftData())
                .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));
        when(adsbExchangeApiService.fetchAircraftData())
                .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));

        // Act
        CompletableFuture<List<AircraftTrackingRequest>> result = multiSourceExternalApiService
                .collectAllAircraftData();

        // Assert
        List<AircraftTrackingRequest> aircraftData = result.join();
        assertTrue(aircraftData.isEmpty());
        verify(rawDataStorageService, never()).storeRawAircraftData(anyString(), anyList(), anyString(), anyLong());
        verify(dataFusionService, never()).mergeAircraftData(any());
    }

    @Test
    void collectAllAircraftData_NullDataReturned_HandlesGracefully() {
        // Arrange
        when(externalApiService.fetchAircraftData())
                .thenReturn(CompletableFuture.completedFuture(null));
        when(adsbExchangeApiService.fetchAircraftData())
                .thenReturn(CompletableFuture.completedFuture(sampleAircraftData));
        when(dataFusionService.mergeAircraftData(any()))
                .thenReturn(sampleAircraftData);

        // Act
        CompletableFuture<List<AircraftTrackingRequest>> result = multiSourceExternalApiService
                .collectAllAircraftData();

        // Assert
        List<AircraftTrackingRequest> aircraftData = result.join();
        assertEquals(2, aircraftData.size());
        verify(rawDataStorageService, times(1)).storeRawAircraftData(anyString(), anyList(), anyString(), anyLong());
    }

    @Test
    void collectAllVesselData_SuccessfulCollection_ReturnsData() {
        // Arrange
        when(externalApiService.fetchVesselData())
                .thenReturn(CompletableFuture.completedFuture(sampleVesselData.subList(0, 1)));
        when(chinaportsApiService.fetchVesselData())
                .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));
        when(marineTrafficV2ApiService.fetchVesselData())
                .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));
        when(vesselFinderApiService.fetchVesselData())
                .thenReturn(CompletableFuture.completedFuture(sampleVesselData.subList(1, 2)));
        when(dataFusionService.mergeVesselData(any()))
                .thenReturn(sampleVesselData);

        // Act
        CompletableFuture<List<VesselTrackingRequest>> result = multiSourceExternalApiService.collectAllVesselData();

        // Assert
        List<VesselTrackingRequest> vesselData = result.join();
        assertEquals(2, vesselData.size());
        verify(rawDataStorageService, times(2)).storeRawVesselData(anyString(), anyList(), anyString(), anyLong());
        verify(dataFusionService).mergeVesselData(any());
    }

    @Test
    void collectAllVesselData_AllSourcesReturnEmpty_ReturnsEmptyList() {
        // Arrange
        when(externalApiService.fetchVesselData())
                .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));
        when(chinaportsApiService.fetchVesselData())
                .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));
        when(marineTrafficV2ApiService.fetchVesselData())
                .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));
        when(vesselFinderApiService.fetchVesselData())
                .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));

        // Act
        CompletableFuture<List<VesselTrackingRequest>> result = multiSourceExternalApiService.collectAllVesselData();

        // Assert
        List<VesselTrackingRequest> vesselData = result.join();
        assertTrue(vesselData.isEmpty());
        verify(dataFusionService, never()).mergeVesselData(any());
    }

    @Test
    void collectAllVesselData_SomeSourcesReturnEmpty_ProcessesSuccessfulSources() {
        // Arrange
        when(externalApiService.fetchVesselData())
                .thenReturn(CompletableFuture.completedFuture(sampleVesselData.subList(0, 1)));
        when(chinaportsApiService.fetchVesselData())
                .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));
        when(marineTrafficV2ApiService.fetchVesselData())
                .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));
        when(vesselFinderApiService.fetchVesselData())
                .thenReturn(CompletableFuture.completedFuture(sampleVesselData.subList(1, 2)));
        when(dataFusionService.mergeVesselData(any()))
                .thenReturn(sampleVesselData);

        // Act
        CompletableFuture<List<VesselTrackingRequest>> result = multiSourceExternalApiService.collectAllVesselData();

        // Assert
        List<VesselTrackingRequest> vesselData = result.join();
        assertEquals(2, vesselData.size());
        verify(rawDataStorageService, times(2)).storeRawVesselData(anyString(), anyList(), anyString(), anyLong());
    }

    @Test
    void collectAllVesselData_NullDataReturned_HandlesGracefully() {
        // Arrange
        when(externalApiService.fetchVesselData())
                .thenReturn(CompletableFuture.completedFuture(null));
        when(chinaportsApiService.fetchVesselData())
                .thenReturn(CompletableFuture.completedFuture(null));
        when(marineTrafficV2ApiService.fetchVesselData())
                .thenReturn(CompletableFuture.completedFuture(null));
        when(vesselFinderApiService.fetchVesselData())
                .thenReturn(CompletableFuture.completedFuture(sampleVesselData));
        when(dataFusionService.mergeVesselData(any()))
                .thenReturn(sampleVesselData);

        // Act
        CompletableFuture<List<VesselTrackingRequest>> result = multiSourceExternalApiService.collectAllVesselData();

        // Assert
        List<VesselTrackingRequest> vesselData = result.join();
        assertEquals(2, vesselData.size());
        verify(rawDataStorageService, times(1)).storeRawVesselData(anyString(), anyList(), anyString(), anyLong());
    }

    @Test
    void collectAndProcessMultiSourceData_SuccessfulExecution_ProcessesBothDataTypes() throws InterruptedException {
        // Arrange
        when(externalApiService.fetchAircraftData())
                .thenReturn(CompletableFuture.completedFuture(sampleAircraftData));
        when(adsbExchangeApiService.fetchAircraftData())
                .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));
        when(externalApiService.fetchVesselData())
                .thenReturn(CompletableFuture.completedFuture(sampleVesselData));
        when(chinaportsApiService.fetchVesselData())
                .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));
        when(marineTrafficV2ApiService.fetchVesselData())
                .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));
        when(vesselFinderApiService.fetchVesselData())
                .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));
        when(dataFusionService.mergeAircraftData(any())).thenReturn(sampleAircraftData);
        when(dataFusionService.mergeVesselData(any())).thenReturn(sampleVesselData);

        // Act
        multiSourceExternalApiService.collectAndProcessMultiSourceData();

        // Wait for async completion
        Thread.sleep(200);

        // Assert
        verify(dataProcessor).processAircraftData(sampleAircraftData);
        verify(dataProcessor).processVesselData(sampleVesselData);
    }

    @Test
    void collectAndProcessMultiSourceData_EmptyData_DoesNotProcess() throws InterruptedException {
        // Arrange
        when(externalApiService.fetchAircraftData())
                .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));
        when(adsbExchangeApiService.fetchAircraftData())
                .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));
        when(externalApiService.fetchVesselData())
                .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));
        when(chinaportsApiService.fetchVesselData())
                .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));
        when(marineTrafficV2ApiService.fetchVesselData())
                .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));
        when(vesselFinderApiService.fetchVesselData())
                .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));

        // Act
        multiSourceExternalApiService.collectAndProcessMultiSourceData();

        // Wait for async completion
        Thread.sleep(200);

        // Assert
        verify(dataProcessor, never()).processAircraftData(any());
        verify(dataProcessor, never()).processVesselData(any());
        verify(dataFusionService, never()).mergeAircraftData(any());
        verify(dataFusionService, never()).mergeVesselData(any());
    }

    @Test
    void getAllSourcesStatus_ReturnsCompleteStatus() {
        // Arrange
        Map<String, Object> mockApiStatus = Map.of("status", "active", "lastUpdate", "2024-01-01T00:00:00");
        Map<String, Object> mockAdsbStatus = Map.of("status", "active", "uptime", "99.9%");
        Map<String, Object> mockVesselFinderStatus = Map.of("status", "active", "dataCount", 1000);
        Map<String, Object> mockChinaportsStatus = Map.of("status", "maintenance", "nextUpdate", "2024-01-02T00:00:00");
        Map<String, Object> mockMarineTrafficV2Status = Map.of("status", "active", "region", "global");

        when(externalApiService.getApiStatus()).thenReturn(mockApiStatus);
        when(adsbExchangeApiService.getAdsbExchangeStatus()).thenReturn(mockAdsbStatus);
        when(vesselFinderApiService.getVesselFinderStatus()).thenReturn(mockVesselFinderStatus);
        when(chinaportsApiService.getChinaportsStatus()).thenReturn(mockChinaportsStatus);
        when(marineTrafficV2ApiService.getMarineTrafficV2Status()).thenReturn(mockMarineTrafficV2Status);

        // Act
        Map<String, Object> status = multiSourceExternalApiService.getAllSourcesStatus();

        // Assert
        assertNotNull(status);
        assertTrue(status.containsKey("currentSources"));
        assertTrue(status.containsKey("newSources"));
        assertTrue(status.containsKey("dataFusion"));

        assertEquals(mockApiStatus, status.get("currentSources"));

        @SuppressWarnings("unchecked")
        Map<String, Object> newSources = (Map<String, Object>) status.get("newSources");
        assertEquals(mockAdsbStatus, newSources.get("adsbexchange"));
        assertEquals(mockVesselFinderStatus, newSources.get("vesselfinder"));
        assertEquals(mockChinaportsStatus, newSources.get("chinaports"));
        assertEquals(mockMarineTrafficV2Status, newSources.get("marinetrafficv2"));

        @SuppressWarnings("unchecked")
        Map<String, Object> dataFusion = (Map<String, Object>) status.get("dataFusion");
        assertEquals(true, dataFusion.get("enabled"));
        assertEquals(true, dataFusion.get("deduplicationEnabled"));
        assertEquals(6, dataFusion.get("activeSources"));
        assertEquals(2, dataFusion.get("aircraftSources"));
        assertEquals(4, dataFusion.get("vesselSources"));
    }

    @Test
    void collectAndProcessMultiSourceData_WithMixedData_ProcessesSuccessfully() throws InterruptedException {
        // Arrange
        when(externalApiService.fetchAircraftData())
                .thenReturn(CompletableFuture.completedFuture(sampleAircraftData.subList(0, 1)));
        when(adsbExchangeApiService.fetchAircraftData())
                .thenReturn(CompletableFuture.completedFuture(sampleAircraftData.subList(1, 2)));
        when(externalApiService.fetchVesselData())
                .thenReturn(CompletableFuture.completedFuture(sampleVesselData.subList(0, 1)));
        when(chinaportsApiService.fetchVesselData())
                .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));
        when(marineTrafficV2ApiService.fetchVesselData())
                .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));
        when(vesselFinderApiService.fetchVesselData())
                .thenReturn(CompletableFuture.completedFuture(sampleVesselData.subList(1, 2)));
        when(dataFusionService.mergeAircraftData(any())).thenReturn(sampleAircraftData);
        when(dataFusionService.mergeVesselData(any())).thenReturn(sampleVesselData);

        // Act
        multiSourceExternalApiService.collectAndProcessMultiSourceData();

        // Wait for async completion
        Thread.sleep(200);

        // Assert
        verify(dataProcessor).processAircraftData(sampleAircraftData);
        verify(dataProcessor).processVesselData(sampleVesselData);
        verify(rawDataStorageService, times(2)).storeRawAircraftData(anyString(), anyList(), anyString(), anyLong());
        verify(rawDataStorageService, times(2)).storeRawVesselData(anyString(), anyList(), anyString(), anyLong());
    }

    @Test
    void getAllSourcesStatus_WithServiceUnavailable_HandlesGracefully() {
        // Arrange
        Map<String, Object> mockApiStatus = Map.of("status", "active");
        Map<String, Object> mockAdsbStatus = Map.of("status", "error", "message", "Service unavailable");
        Map<String, Object> mockVesselFinderStatus = Map.of("status", "active");
        Map<String, Object> mockChinaportsStatus = Map.of("status", "maintenance");
        Map<String, Object> mockMarineTrafficV2Status = Map.of("status", "active");

        when(externalApiService.getApiStatus()).thenReturn(mockApiStatus);
        when(adsbExchangeApiService.getAdsbExchangeStatus()).thenReturn(mockAdsbStatus);
        when(vesselFinderApiService.getVesselFinderStatus()).thenReturn(mockVesselFinderStatus);
        when(chinaportsApiService.getChinaportsStatus()).thenReturn(mockChinaportsStatus);
        when(marineTrafficV2ApiService.getMarineTrafficV2Status()).thenReturn(mockMarineTrafficV2Status);

        // Act
        Map<String, Object> status = multiSourceExternalApiService.getAllSourcesStatus();

        // Assert
        assertNotNull(status);
        @SuppressWarnings("unchecked")
        Map<String, Object> newSources = (Map<String, Object>) status.get("newSources");
        assertEquals("error", ((Map<String, Object>) newSources.get("adsbexchange")).get("status"));
        assertEquals("maintenance", ((Map<String, Object>) newSources.get("chinaports")).get("status"));
    }
}