# Multi-Source Data Simulator for FlightMap Backend

## 📋 Overview

The Multi-Source Data Simulator provides comprehensive testing capabilities for the `MultiSourceExternalApiService`. It generates realistic mock data from all external API sources, enabling development and testing without dependencies on external services.

## 🏗️ Architecture

```
📊 MultiSourceExternalApiService
    ↓ (uses)
🎯 MultiSourceDataSimulator
    ↓ (generates)
📈 Source-Specific Mock Data
    ↓ (consumed by)
🔄 MultiSourceApiSimulatorService
    ↓ (exposes via)
🌐 MultiSourceSimulatorController
```

## ✨ Features

### Data Source Simulation
- **Aircraft Sources**: FlightRadar24, ADS-B Exchange
- **Vessel Sources**: MarineTraffic, VesselFinder, Chinaports, MarineTrafficV2
- **Source-specific characteristics**: Each source has unique data quality and field patterns
- **Realistic geographic distribution**: Vietnam region for aircraft, Asia-Pacific for vessels

### Testing Capabilities
- **Integration Testing**: Full multi-source data collection simulation
- **Performance Testing**: Large dataset generation with configurable volumes
- **Error Scenario Testing**: Network timeouts, empty responses, malformed data
- **Data Quality Testing**: Variable quality levels per source
- **Concurrent Testing**: Parallel data generation from multiple sources

### Configuration Options
- **Data Volume Control**: Configurable min/max counts per source
- **Quality Variation**: Adjustable data quality ranges
- **Response Timing**: Simulated network delays
- **Error Rates**: Configurable failure scenarios

## 🚀 Quick Start

### 1. Configuration

Add to `application.properties`:

```properties
# Enable simulator
simulator.enabled=true
simulator.test.mode=true
simulator.performance.test.enabled=true

# Data volume configuration
simulator.aircraft.count.min=5
simulator.aircraft.count.max=50
simulator.vessel.count.min=10
simulator.vessel.count.max=100

# Quality settings
simulator.data.quality.variation=true
simulator.response.delay.min=100
simulator.response.delay.max=2000
```

### 2. Basic Usage

```java
@Autowired
private MultiSourceDataSimulator dataSimulator;

@Autowired
private MultiSourceApiSimulatorService simulatorService;

// Generate data from specific source
CompletableFuture<List<AircraftTrackingRequest>> aircraft = 
    dataSimulator.simulateFlightRadar24Data();

// Run comprehensive integration test
Map<String, Object> testResults = 
    simulatorService.testMultiSourceDataCollection();

// Performance test with custom parameters
Map<String, Object> perfResults = 
    simulatorService.runPerformanceTest(1000, 2000);
```

### 3. REST API Usage

```bash
# Get simulator status
GET /api/simulator/status

# Run multi-source integration test
POST /api/simulator/test/multi-source

# Performance test
POST /api/simulator/test/performance?aircraftCount=1000&vesselCount=2000

# Generate test data for specific source
POST /api/simulator/generate/aircraft/flightradar24
POST /api/simulator/generate/vessel/marinetraffic

# Test error scenarios
POST /api/simulator/test/error-scenarios

# Health check
GET /api/simulator/health
```

## 📊 Data Source Characteristics

### Aircraft Sources

#### FlightRadar24
- **Data Quality**: 0.85 - 1.0 (High)
- **Coverage**: Comprehensive global coverage
- **Characteristics**: All fields populated, reliable data
- **Geographic Focus**: Vietnam region (8°N-23.5°N, 102°E-109.5°E)

#### ADS-B Exchange
- **Data Quality**: 0.75 - 1.0 (Good)
- **Coverage**: Technical/amateur radio focus
- **Characteristics**: Strong technical data (vertical rate, squawk)
- **Geographic Focus**: Vietnam region with technical emphasis

### Vessel Sources

#### MarineTraffic
- **Data Quality**: 0.80 - 1.0 (High)
- **Coverage**: Commercial shipping focus
- **Characteristics**: Complete vessel information, commercial types
- **Geographic Focus**: Asia-Pacific region

#### VesselFinder
- **Data Quality**: 0.60 - 1.0 (Variable)
- **Coverage**: Alternative tracking
- **Characteristics**: Some missing fields (50% chance for IMO, destination)
- **Geographic Focus**: Global coverage

#### Chinaports
- **Data Quality**: 0.70 - 1.0 (Good)
- **Coverage**: Chinese ports and region
- **Characteristics**: Chinese flags, regional ports
- **Geographic Focus**: Chinese waters (18°N-41°N, 108°E-126°E)

#### MarineTrafficV2
- **Data Quality**: 0.90 - 1.0 (Very High)
- **Coverage**: Enhanced API version
- **Characteristics**: Most complete and accurate data
- **Geographic Focus**: Global with enhanced accuracy

## 🧪 Testing Scenarios

### 1. Integration Testing

Tests the complete data flow from simulation through MultiSourceExternalApiService:

```java
@Test
void testMultiSourceIntegration() {
    Map<String, Object> results = simulatorService.testMultiSourceDataCollection();
    
    assertTrue((Boolean) results.get("success"));
    assertTrue((Integer) results.get("aircraftCount") > 0);
    assertTrue((Integer) results.get("vesselCount") > 0);
}
```

### 2. Performance Testing

Validates system performance under load:

```java
@Test
void testPerformance() {
    Map<String, Object> results = simulatorService.runPerformanceTest(1000, 2000);
    
    assertTrue((Boolean) results.get("success"));
    double recordsPerSecond = (Double) results.get("recordsPerSecond");
    assertTrue(recordsPerSecond > 100); // Expect at least 100 records/sec
}
```

### 3. Error Scenario Testing

Validates error handling:

```java
@Test
void testErrorHandling() {
    // Test network timeouts
    assertThrows(RuntimeException.class, () -> 
        dataSimulator.simulateAircraftNetworkError("test").join());
    
    // Test empty responses
    List<Object> empty = dataSimulator.simulateEmptyResponse("test").join();
    assertTrue(empty.isEmpty());
}
```

### 4. Data Quality Testing

Validates data quality per source:

```java
@Test
void testDataQuality() {
    List<AircraftTrackingRequest> flightRadarData = 
        dataSimulator.simulateFlightRadar24Data().join();
    
    double avgQuality = flightRadarData.stream()
        .mapToDouble(AircraftTrackingRequest::getDataQuality)
        .average().orElse(0.0);
    
    assertTrue(avgQuality >= 0.85); // FlightRadar24 should be high quality
}
```

## 🔧 Configuration Reference

### Core Settings

| Property | Default | Description |
|----------|---------|-------------|
| `simulator.enabled` | `true` | Enable/disable simulator |
| `simulator.test.mode` | `false` | Enable test mode features |
| `simulator.performance.test.enabled` | `false` | Enable performance testing |

### Data Volume Settings

| Property | Default | Description |
|----------|---------|-------------|
| `simulator.aircraft.count.min` | `5` | Minimum aircraft per source |
| `simulator.aircraft.count.max` | `50` | Maximum aircraft per source |
| `simulator.vessel.count.min` | `10` | Minimum vessels per source |
| `simulator.vessel.count.max` | `100` | Maximum vessels per source |

### Quality Settings

| Property | Default | Description |
|----------|---------|-------------|
| `simulator.data.quality.variation` | `true` | Enable quality variation |
| `simulator.flightradar24.quality.min` | `0.85` | FlightRadar24 min quality |
| `simulator.marinetraffic.quality.min` | `0.80` | MarineTraffic min quality |

### Response Settings

| Property | Default | Description |
|----------|---------|-------------|
| `simulator.response.delay.min` | `100` | Min response delay (ms) |
| `simulator.response.delay.max` | `2000` | Max response delay (ms) |
| `simulator.error.rate` | `0.05` | General error rate (5%) |

## 📈 Performance Benchmarks

### Typical Performance (Development Environment)

- **Single Source Generation**: 50-500 records in 100-2000ms
- **Multi-Source Parallel**: 6 sources in 1-3 seconds
- **Large Dataset**: 10,000 records in 5-15 seconds
- **Concurrent Testing**: 6 parallel sources with minimal contention

### Memory Usage

- **Small Dataset** (10-100 records): ~1-5 MB
- **Medium Dataset** (1,000 records): ~10-20 MB
- **Large Dataset** (10,000 records): ~50-100 MB

## 🚨 Error Simulation

### Network Errors
```java
// Simulate timeout for specific source
CompletableFuture<List<AircraftTrackingRequest>> future = 
    dataSimulator.simulateAircraftNetworkError("flightradar24");
```

### Empty Responses
```java
// Simulate empty response
CompletableFuture<List<Object>> future = 
    dataSimulator.simulateEmptyResponse("source");
```

### Data Quality Issues
- Missing required fields (configurable rate)
- Low quality scores
- Invalid coordinates
- Malformed identifiers

## 🔍 Monitoring and Debugging

### Logging
The simulator provides detailed logging at different levels:

```properties
# Enable detailed simulator logging
logging.level.com.phamnam.tracking_vessel_flight.service.simulator=DEBUG
```

### Status Endpoints
- `GET /api/simulator/status` - Comprehensive status
- `GET /api/simulator/config` - Configuration details
- `GET /api/simulator/health` - Health check

### Metrics
The simulator tracks:
- Generation times per source
- Data quality distributions
- Error rates
- Concurrent execution statistics

## 🛠️ Development and Extension

### Adding New Sources

1. Add simulation method to `MultiSourceDataSimulator`:
```java
public CompletableFuture<List<AircraftTrackingRequest>> simulateNewSourceData() {
    return CompletableFuture.supplyAsync(() -> {
        // Generate source-specific data
        return generateNewSourceData();
    });
}
```

2. Add configuration properties:
```properties
simulator.newsource.quality.min=0.70
simulator.newsource.quality.max=0.95
```

3. Add REST endpoint in `MultiSourceSimulatorController`:
```java
@PostMapping("/generate/aircraft/newsource")
public ResponseEntity<Object> generateNewSourceData() {
    // Implementation
}
```

4. Add tests in `MultiSourceDataSimulatorTest`:
```java
@Test
void testNewSourceSimulation() {
    // Test implementation
}
```

### Custom Data Generators

Create custom generators for specific testing scenarios:

```java
public class CustomDataGenerator {
    public static AircraftTrackingRequest generateCustomScenario() {
        return AircraftTrackingRequest.builder()
            .hexident("CUSTOM1")
            .source("custom")
            .dataQuality(0.95)
            // Custom fields...
            .build();
    }
}
```

## 📚 Related Documentation

- [MultiSourceExternalApiService Documentation](../service/realtime/README.md)
- [Data Fusion Guide](../../MULTI_SOURCE_DATA_FUSION_GUIDE.md)
- [Testing Guide](../../TESTING_GUIDE.md)
- [API Documentation](../../FRONTEND_API_GUIDE.md)

## 🤝 Contributing

When contributing to the simulator:

1. **Add Tests**: Every new feature should include comprehensive tests
2. **Update Documentation**: Keep this README updated with new features
3. **Follow Patterns**: Use existing patterns for consistency
4. **Performance**: Consider performance impact of new simulations
5. **Configuration**: Make new features configurable

## 🐛 Troubleshooting

### Common Issues

**Simulator Not Generating Data**
- Check `simulator.enabled=true` in configuration
- Verify no exceptions in logs
- Check data count configuration (min/max values)

**Performance Issues**
- Reduce data volume for testing
- Check concurrent execution limits
- Monitor memory usage

**Quality Issues**
- Verify quality range configuration
- Check source-specific quality settings
- Review data validation logic

**Network Simulation Issues**
- Verify delay configuration
- Check error rate settings
- Review timeout simulation logic

### Debug Mode

Enable debug mode for detailed logging:

```properties
simulator.test.mode=true
logging.level.com.phamnam.tracking_vessel_flight.service.simulator=DEBUG
```

This provides detailed information about:
- Data generation processes
- Quality assignments
- Error simulations
- Performance metrics

---

**🎯 The Multi-Source Data Simulator enables comprehensive testing of the FlightMap backend without external dependencies, ensuring robust and reliable data collection from multiple sources.**
