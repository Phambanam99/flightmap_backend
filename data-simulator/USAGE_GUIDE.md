# 🚀 Quick Start Guide - Data Simulator

## Bước 1: Chuẩn bị Kafka

```bash
# Từ thư mục root project
docker-compose up -d kafka zookeeper redis
```

Kiểm tra Kafka UI: http://localhost:8080

## Bước 2: Chạy Data Simulator

```bash
cd data-simulator
npm install
npm run dev
```

Server chạy tại: http://localhost:3001

## Bước 3: Test cơ bản

### Health Check

```bash
curl http://localhost:3001/health
```

### Tạo dữ liệu manually

```bash
# Tạo 1 flight
curl -X POST http://localhost:3001/api/manual/flight \
  -H "Content-Type: application/json" \
  -d '{
    "Callsign": "VN123",
    "Latitude": 10.823,
    "Longitude": 106.629,
    "Altitude": 35000
  }'

# Tạo 1 ship
curl -X POST http://localhost:3001/api/manual/ship \
  -H "Content-Type: application/json" \
  -d '{
    "voyageId": 12345,
    "latitude": 10.8,
    "longitude": 107.1,
    "speed": 15
  }'
```

## Bước 4: Chạy scenarios

### Airport Scenario (10 flights quanh TSN)

```bash
curl -X POST http://localhost:3001/api/simulation/scenarios/airport \
  -H "Content-Type: application/json" \
  -d '{"airportLocation": "tanSonNhat"}'
```

### Port Scenario (10 ships quanh cảng)

```bash
curl -X POST http://localhost:3001/api/simulation/scenarios/port
```

## Bước 5: Real-time Simulation

### Start simulation

```bash
curl -X POST http://localhost:3001/api/simulation/start \
  -H "Content-Type: application/json" \
  -d '{
    "flightInterval": 2000,
    "shipInterval": 3000,
    "maxFlights": 20,
    "maxShips": 10,
    "batchMode": false
  }'
```

### Monitor status

```bash
curl http://localhost:3001/api/status
```

### Stop simulation

```bash
curl -X POST http://localhost:3001/api/simulation/stop
```

## Bước 6: Verify trong Kafka

1. Mở Kafka UI: http://localhost:8080
2. Check topics:
   - `flight-tracking`
   - `ship-tracking`
3. Xem messages realtime

## Bước 7: Test với Backend

1. Start backend tracking system
2. Backend sẽ tự động consume data từ Kafka
3. Check Redis cache
4. Test WebSocket endpoints

## 🎯 Use Cases

### Testing High Volume

```bash
# Start với nhiều vehicles
curl -X POST http://localhost:3001/api/simulation/start \
  -H "Content-Type: application/json" \
  -d '{
    "maxFlights": 100,
    "maxShips": 50,
    "flightInterval": 1000,
    "batchMode": true
  }'
```

### Testing Specific Location

```bash
# Hanoi airport
curl -X POST http://localhost:3001/api/simulation/scenarios/airport \
  -H "Content-Type: application/json" \
  -d '{"airportLocation": "noiBai"}'
```

### Custom Flight Data

```bash
curl -X POST http://localhost:3001/api/manual/flight \
  -H "Content-Type: application/json" \
  -d '{
    "Callsign": "SPECIAL1",
    "Latitude": 21.0285,
    "Longitude": 105.8542,
    "Altitude": 41000,
    "Speed": 850,
    "Type": "B777"
  }'
```

## 🔧 Configuration

### Runtime config update

```bash
curl -X PUT http://localhost:3001/api/config \
  -H "Content-Type: application/json" \
  -d '{
    "simulation": {
      "interval": 1500,
      "maxFlights": 30
    }
  }'
```

### Check current config

```bash
curl http://localhost:3001/api/config
```

## 📊 API Documentation

Xem full API docs:

```bash
curl http://localhost:3001/api/docs
```

## 🧪 Auto Testing

Chạy test script để verify tất cả chức năng:

```bash
npm test
```

Test sẽ chạy qua tất cả endpoints và verify functionality.
