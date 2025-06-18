# Data Simulator - Backend Integration Guide

## 🎯 Overview

Data-simulator đã được tích hợp với `TrackingDataPublisherController` và `TrackingDataConsumerController` để tạo ra một workflow hoàn chỉnh cho việc tạo và xử lý dữ liệu tracking realtime.

## 🔄 Workflow Architecture

```
Data Simulator → BackendIntegrationService → TrackingDataPublisherController → Kafka → KafkaConsumerService → TrackingDataConsumerController
```

## ⚙️ Configuration

### Environment Variables

```bash
# Backend Integration
BACKEND_BASE_URL=http://localhost:8080

# Publish Mode
PUBLISH_MODE=BOTH # KAFKA | BACKEND | BOTH
```

### Publish Modes

1. **KAFKA**: Chỉ gửi data tới Kafka (workflow cũ)
2. **BACKEND**: Chỉ gửi data tới REST API backend
3. **BOTH**: Gửi data tới cả Kafka và REST API

## 🚀 Usage

### 1. Start Simulation với Backend Integration

```bash
# Start simulation với BACKEND mode
curl -X POST http://localhost:3001/api/simulation/start \
  -H "Content-Type: application/json" \
  -d '{
    "publishMode": "BACKEND",
    "maxFlights": 1000,
    "maxShips": 500,
    "flightInterval": 2000,
    "shipInterval": 3000
  }'
```

### 2. Change Publish Mode Runtime

```bash
# Switch to BOTH mode during runtime
curl -X POST http://localhost:3001/api/simulation/set-publish-mode \
  -H "Content-Type: application/json" \
  -d '{"publishMode": "BOTH"}'
```

### 3. Monitor Backend Integration

```bash
# Check backend health and integration status
curl http://localhost:3001/api/backend/status

# Get consumer metrics from backend
curl http://localhost:3001/api/backend/consumer/metrics

# Reset consumer counters
curl -X POST http://localhost:3001/api/backend/consumer/reset-counters

# Trigger manual batch update
curl -X POST http://localhost:3001/api/backend/consumer/trigger-batch
```

## 📊 API Endpoints

### Data Simulator Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/simulation/start` | Start simulation with options |
| POST | `/api/simulation/set-publish-mode` | Change publish mode |
| GET | `/api/backend/status` | Get backend integration status |
| GET | `/api/backend/consumer/metrics` | Get consumer metrics |
| POST | `/api/backend/consumer/reset-counters` | Reset consumer counters |
| POST | `/api/backend/consumer/trigger-batch` | Trigger batch update |

### Backend Java Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/tracking/publish/flight` | Publish flight data |
| POST | `/api/tracking/publish/vessel` | Publish vessel data |
| GET | `/api/tracking/consumer/status` | Get consumer status |
| POST | `/api/tracking/consumer/reset-counters` | Reset counters |
| POST | `/api/tracking/consumer/trigger-batch-update` | Trigger batch |

## 🛠 Data Format Compatibility

### Flight Data (FlightTrackingRequestDTO)

```json
{
  "id": 12345,
  "aircraftId": 678,
  "hexident": "ABC123",
  "callsign": "VN1234",
  "latitude": 21.028511,
  "longitude": 105.804817,
  "altitude": 35000,
  "speed": 450.5,
  "heading": 90.5,
  "verticalSpeed": 100,
  "aircraftType": "B737",
  "timestamp": "2024-01-01T10:00:00Z",
  "trackingSource": "SIMULATOR",
  "confidence": 0.95
}
```

### Vessel Data (ShipTrackingRequest)

```json
{
  "voyageId": 54321,
  "vesselId": 9876,
  "vesselName": "Ho Container 123",
  "imo": "1234567",
  "mmsi": "123456789",
  "callSign": "VN123",
  "vesselType": "Container",
  "latitude": 10.823099,
  "longitude": 106.629662,
  "speed": 15.5,
  "heading": 180.0,
  "destination": "Ho Chi Minh City Port",
  "timestamp": "2024-01-01T10:00:00Z",
  "trackingSource": "SIMULATOR"
}
```

## 📈 Monitoring & Statistics

### Integration Statistics

```bash
# Get detailed statistics
curl http://localhost:3001/api/status/detailed
```

Response includes:
- Flights/vessels generated and published
- Success/error rates
- Backend health status
- Consumer metrics

### Health Checks

- **Simulator Health**: `GET /health`
- **Backend Health**: Automatic checking every 60 seconds
- **Consumer Health**: Via backend integration service

## 🔧 Troubleshooting

### Common Issues

1. **Backend Connection Failed**
   ```bash
   # Check backend is running
   curl http://localhost:8080/actuator/health
   
   # Check network connectivity
   telnet localhost 8080
   ```

2. **Data Format Errors**
   - Ensure Java DTOs match data-simulator format
   - Check field naming conventions (camelCase)
   - Validate required fields

3. **Performance Issues**
   - Adjust intervals in simulation options
   - Use batch mode for high-volume scenarios
   - Monitor memory usage

### Debug Mode

```bash
# Enable debug logging
export DEBUG=true
npm start
```

## 🎨 Example Integration Workflow

```bash
# 1. Start backend
cd flightmap_backend
./gradlew bootRun

# 2. Start data-simulator
cd data-simulator
npm start

# 3. Start simulation with backend integration
curl -X POST http://localhost:3001/api/simulation/start \
  -H "Content-Type: application/json" \
  -d '{
    "publishMode": "BOTH",
    "maxFlights": 100,
    "maxShips": 50,
    "flightInterval": 3000
  }'

# 4. Monitor integration
curl http://localhost:3001/api/backend/status
curl http://localhost:8080/api/tracking/consumer/status

# 5. Control consumer
curl -X POST http://localhost:3001/api/backend/consumer/trigger-batch
```

## 🔮 Next Steps

1. Add WebSocket integration for real-time updates
2. Implement data validation and error recovery
3. Add metrics dashboard
4. Integrate with monitoring systems (Prometheus, Grafana)
5. Add load testing capabilities

---

**Note**: Đảm bảo cả Java backend và Node.js data-simulator đều running trước khi test integration. 