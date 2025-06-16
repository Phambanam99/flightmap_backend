# 🛩️ Tracking Data Simulator

Một server Node.js + Express để giả lập dữ liệu thực tế cho hệ thống tracking máy bay và tàu thuyền sử dụng Kafka.

## 🎯 Mục đích

- **Tạo dữ liệu giả lập realistic** cho flight và vessel tracking
- **Publish dữ liệu qua Kafka** để test backend system
- **Simulations movement thực tế** với physics-based calculations
- **Multiple scenarios** (airport, port, custom)
- **RESTful API** để control simulation

## 🚀 Cài đặt & Chạy

### 1. Install Dependencies

```bash
cd data-simulator
npm install
```

### 2. Cấu hình Environment (Optional)

Tạo file `.env` (hoặc sử dụng config mặc định):

```env
PORT=3001
KAFKA_BROKERS=localhost:29092
KAFKA_CLIENT_ID=tracking-data-simulator
KAFKA_FLIGHT_TOPIC=flight-tracking
KAFKA_SHIP_TOPIC=ship-tracking

# Simulation Configuration
SIMULATION_INTERVAL=2000
MAX_FLIGHTS=50
MAX_SHIPS=20
```

### 3. Khởi động Kafka (nếu chưa chạy)

```bash
# Từ thư mục root của project
docker-compose up -d kafka zookeeper
```

### 4. Chạy Data Simulator

```bash
# Development mode
npm run dev

# Production mode
npm start
```

Server sẽ chạy trên: `http://localhost:3001`

## 📊 API Endpoints

### Health Check

```bash
GET /health
```

### Status Monitoring

```bash
# Basic status
GET /api/status

# Detailed status (bao gồm Kafka metadata)
GET /api/status/detailed
```

### Simulation Control

```bash
# Bắt đầu simulation
POST /api/simulation/start
Content-Type: application/json

{
  "flightInterval": 2000,
  "shipInterval": 3000,
  "maxFlights": 30,
  "maxShips": 15,
  "batchMode": false
}

# Dừng simulation
POST /api/simulation/stop
```

### Scenarios

```bash
# Airport scenario - tạo nhiều flights quanh sân bay
POST /api/simulation/scenarios/airport
Content-Type: application/json

{
  "airportLocation": "tanSonNhat"
  // Options: tanSonNhat, noiBai, hcmc, hanoi
}

# Port scenario - tạo nhiều ships quanh cảng
POST /api/simulation/scenarios/port
```

### Manual Data Generation

```bash
# Tạo 1 flight manually
POST /api/manual/flight
Content-Type: application/json

{
  "Callsign": "VN123",
  "Latitude": 10.823,
  "Longitude": 106.629,
  "Altitude": 35000
}

# Tạo 1 ship manually
POST /api/manual/ship
Content-Type: application/json

{
  "voyageId": 12345,
  "latitude": 10.8,
  "longitude": 107.1,
  "speed": 15
}
```

### Configuration

```bash
# Xem config hiện tại
GET /api/config

# Update config runtime
PUT /api/config
Content-Type: application/json

{
  "simulation": {
    "interval": 1500,
    "maxFlights": 40
  }
}

# Xem các locations có sẵn
GET /api/locations
```

### API Documentation

```bash
GET /api/docs
```

## 📡 Kafka Integration

### Topics được sử dụng:

- **flight-tracking**: Dữ liệu tracking máy bay
- **ship-tracking**: Dữ liệu tracking tàu thuyền

### Data Format

#### Flight Tracking Data

```json
{
  "Id": 12345,
  "AircraftId": 789,
  "Hexident": "AB1234",
  "Callsign": "VN123",
  "Latitude": 10.823,
  "Longitude": 106.629,
  "Altitude": 35000,
  "Speed": 450,
  "Heading": 270,
  "VerticalSpeed": 0,
  "Type": "A320",
  "UpdateTime": "2025-01-16T10:30:45",
  "UnixTime": 1705317045,
  "Distance": 150.5,
  "Bearing": 45,
  "Squawk": 2000,
  "AltitudeType": "barometric",
  "SpeedType": "ground"
}
```

#### Ship Tracking Data

```json
{
  "voyageId": 54321,
  "timestamp": "2025-01-16T10:30:45",
  "latitude": 10.8,
  "longitude": 107.1,
  "speed": 15.5,
  "course": 180,
  "draught": 12.5
}
```

## 🎮 Cách sử dụng

### Workflow cơ bản:

1. **Khởi động server**:

   ```bash
   npm run dev
   ```

2. **Check health**:

   ```bash
   curl http://localhost:3001/health
   ```

3. **Start simulation**:

   ```bash
   curl -X POST http://localhost:3001/api/simulation/start \
     -H "Content-Type: application/json" \
     -d '{"maxFlights": 20, "maxShips": 10}'
   ```

4. **Monitor status**:

   ```bash
   curl http://localhost:3001/api/status
   ```

5. **Tạo airport scenario**:

   ```bash
   curl -X POST http://localhost:3001/api/simulation/scenarios/airport \
     -H "Content-Type: application/json" \
     -d '{"airportLocation": "tanSonNhat"}'
   ```

6. **Stop simulation**:
   ```bash
   curl -X POST http://localhost:3001/api/simulation/stop
   ```

### Test với Kafka UI:

1. Mở Kafka UI: http://localhost:8080
2. Check topics: `flight-tracking`, `ship-tracking`
3. Xem messages real-time khi simulation đang chạy

## 🌍 Geographic Coverage

Simulator tập trung vào khu vực **Việt Nam**:

### Airports:

- **Tân Sơn Nhất** (Ho Chi Minh City)
- **Nội Bài** (Hanoi)

### Ports:

- **TP.HCM Port** (10.8°N, 107.1°E)
- **Hải Phòng Port** (20.9°N, 106.9°E)
- **Quy Nhon Port** (12.2°N, 109.2°E)
- **Đà Nẵng Port** (16.1°N, 108.2°E)

### Bounds:

- **Latitude**: 8.5°N - 23.5°N
- **Longitude**: 102.0°E - 109.5°E

## ⚙️ Configuration Options

```javascript
{
  simulation: {
    interval: 2000,        // Interval giữa các updates (ms)
    maxFlights: 50,        // Số flights tối đa
    maxShips: 20          // Số ships tối đa
  },
  flight: {
    altitudeRange: { min: 1000, max: 42000 },    // feet
    speedRange: { min: 150, max: 900 },          // knots
    callsignPrefixes: ['VN', 'VJ', 'BL', 'QH']   // Vietnam airlines
  },
  ship: {
    speedRange: { min: 0, max: 25 },             // knots
    shipTypes: ['Container', 'Tanker', 'Fishing']
  }
}
```

## 🔧 Development

### Structure:

```
data-simulator/
├── config.js              # Configuration
├── server.js              # Express server
├── package.json           # Dependencies
├── services/
│   ├── kafkaService.js     # Kafka producer
│   └── simulationService.js # Simulation logic
└── utils/
    └── dataGenerators.js   # Data generation & movement
```

### Features:

- **Realistic movement simulation** với physics calculations
- **Automatic cleanup** của inactive vehicles
- **Batch publishing** support
- **Graceful shutdown** handling
- **Error handling** comprehensive
- **Multiple scenarios** support

## 🐳 Docker Support

Tạo file `Dockerfile`:

```dockerfile
FROM node:18-alpine

WORKDIR /app
COPY package*.json ./
RUN npm install --only=production

COPY . .

EXPOSE 3001
CMD ["npm", "start"]
```

Build & Run:

```bash
docker build -t tracking-simulator .
docker run -p 3001:3001 -e KAFKA_BROKERS=host.docker.internal:29092 tracking-simulator
```

## 📈 Monitoring & Debugging

### Logs:

- ✅ **Connection status**
- ✈️ **Flight data published**
- 🚢 **Ship data published**
- ❌ **Error messages**
- 📊 **Statistics**

### Metrics Available:

- Active flights/ships count
- Messages published count
- Error count
- Uptime
- Kafka connection status

## 🚨 Troubleshooting

### Common Issues:

1. **Kafka Connection Failed**:

   ```bash
   # Check Kafka is running
   docker-compose ps kafka

   # Check port is accessible
   telnet localhost 29092
   ```

2. **Permission Errors**:

   ```bash
   # Make sure Kafka topics are created
   # Check Kafka UI: http://localhost:8080
   ```

3. **Data Not Appearing**:

   ```bash
   # Check simulation is running
   curl http://localhost:3001/api/status

   # Check Kafka topics have messages
   # Via Kafka UI or CLI
   ```

## 🤝 Integration với Backend

Server này tương thích hoàn toàn với backend tracking system đang sử dụng:

- **Topics**: `flight-tracking`, `ship-tracking`
- **Message format**: Giống với `FlightTrackingRequestDTO` và `ShipTrackingRequest`
- **Kafka configuration**: Tương thích với Spring Boot Kafka setup

### Next Steps:

1. Start data simulator
2. Start backend tracking system
3. Backend sẽ automatically consume data từ Kafka
4. Check Redis cache để verify data flow
5. Test WebSocket real-time updates

## 📝 License

MIT License - Free to use and modify.
