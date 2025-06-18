# Tracking Data Simulator

Real-time tracking data simulator for flights and vessels that publishes data to backend REST API endpoints.

## Features

- ✈️ **Flight Simulation**: Generates realistic flight tracking data with proper movement patterns
- 🚢 **Vessel Simulation**: Simulates vessel movements along shipping lanes
- 📊 **Real-time Dashboard**: Web-based monitoring dashboard
- 🔄 **REST API Integration**: Publishes data through backend REST endpoints
- 📈 **Configurable Parameters**: Adjust simulation rates, maximum entities, and more
- 🎯 **Realistic Movement**: Simulates actual flight paths and vessel routes
- 📝 **Comprehensive Logging**: Winston-based logging with multiple levels

## Prerequisites

- Node.js >= 14.x
- Backend service running at `http://localhost:8080` (default)
- Kafka infrastructure configured in the backend

## Installation

```bash
cd flightmap_backend/data-simulator
npm install
```

## Configuration

Create a `.env` file based on the following configuration options:

```bash
# Server Configuration
PORT=3001

# Backend API Configuration
API_BASE_URL=http://localhost:8080
API_TIMEOUT=30000
API_RETRY_ATTEMPTS=3
API_RETRY_DELAY=1000

# Simulation Configuration
FLIGHT_SIMULATION_INTERVAL=1000    # milliseconds
VESSEL_SIMULATION_INTERVAL=2000    # milliseconds
MAX_FLIGHTS=100
MAX_VESSELS=50
BATCH_SIZE=10
ENABLE_FLIGHTS=true
ENABLE_VESSELS=true

# Logging Configuration
LOG_LEVEL=info
LOG_FILE=./simulator.log
LOG_CONSOLE=true
```

## Usage

### Start the Simulator Server

```bash
npm start
```

Or for development with auto-reload:

```bash
npm run dev
```

### Access the Dashboard

Open your browser and navigate to:
```
http://localhost:3001
```

### API Endpoints

#### Health Check
```
GET /health
```

#### Get Status
```
GET /status
```

#### Control Endpoints

Start simulators:
```
POST /control/start
Body: { "simulators": ["flight", "vessel"] }
```

Stop simulators:
```
POST /control/stop
Body: { "simulators": ["flight", "vessel"] }
```

Force update all data:
```
POST /control/force-update
Body: { "flight": true, "vessel": true }
```

#### Data Endpoints

Get specific flight:
```
GET /flights/:id
```

Get specific vessel:
```
GET /vessels/:id
```

Get vessels in area:
```
POST /vessels/in-area
Body: {
  "minLat": 8.5,
  "maxLat": 23.5,
  "minLon": 102.0,
  "maxLon": 109.5
}
```

## Architecture

### Data Flow

1. **Data Generation**: Simulators create realistic tracking data
2. **API Publishing**: Data is sent to backend REST endpoints
3. **Kafka Processing**: Backend publishes to Kafka topics
4. **WebSocket Updates**: Real-time updates sent to connected clients

### Components

- **FlightSimulator**: Manages flight generation and updates
- **VesselSimulator**: Handles vessel tracking simulation
- **MovementSimulator**: Core logic for realistic movement patterns
- **ApiClient**: Handles REST API communication with retry logic
- **Logger**: Winston-based logging system

### Backend Integration

The simulator publishes data to these backend endpoints:
- `/api/tracking/publish/flight` - Flight tracking data
- `/api/tracking/publish/vessel` - Vessel tracking data

## Data Models

### Flight Data (FlightTrackingRequestDTO)

```javascript
{
  Id: number,
  AircraftId: number,
  Hexident: string,
  Callsign: string,
  Latitude: number,
  Longitude: number,
  Altitude: number,
  Speed: number,
  // ... and more fields
}
```

### Vessel Data (ShipTrackingRequest)

```javascript
{
  timestamp: string,
  latitude: number,
  longitude: number,
  speed: number,
  course: number,
  draught: number,
  voyageId: number
}
```

## Monitoring

The web dashboard provides real-time monitoring of:
- Active flights and vessels
- Publication rates
- Success/failure rates
- Consumer health status
- Runtime statistics

## Troubleshooting

### Common Issues

1. **Connection Refused**: Ensure backend is running at configured URL
2. **High Failure Rate**: Check API timeout settings and backend health
3. **No Data Updates**: Verify Kafka consumer is running in backend

### Logs

Check `simulator.log` for detailed error information and debugging.

## Development

### Project Structure

```
data-simulator/
├── config.js           # Configuration management
├── server.js           # Express server and API routes
├── services/
│   ├── flightSimulator.js   # Flight simulation logic
│   └── vesselSimulator.js   # Vessel simulation logic
└── utils/
    ├── apiClient.js    # REST API client with retry
    ├── dataGenerators.js # Movement simulation algorithms
    └── logger.js       # Winston logger configuration
```

### Adding New Features

1. Extend `MovementSimulator` for new entity types
2. Create new simulator service following existing patterns
3. Add corresponding API endpoints in `server.js`
4. Update dashboard UI for new metrics

## License

ISC
