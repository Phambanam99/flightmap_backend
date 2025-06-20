# Flight Map History API Guide

## Overview
This guide provides comprehensive documentation for the history tracking APIs that allow you to access historical and real-time tracking data for both flights and vessels.

## Base URL
All APIs are available at: `http://localhost:9090/api/tracking/history`

## Authentication
Currently, no authentication is required for these endpoints.

---

## Flight History APIs

### 1. Get Flight Tracking History
Retrieves historical tracking data for a specific flight within a time range.

**Endpoint:** `GET /api/tracking/history/flight/{hexident}`

**Parameters:**
- `hexident` (path) - Aircraft hexident (ICAO24 code), e.g., "A12345"
- `fromTime` (query, required) - Start time in ISO format, e.g., "2024-12-01T00:00:00"
- `toTime` (query, required) - End time in ISO format, e.g., "2024-12-01T23:59:59"

**Example Request:**
```bash
curl "http://localhost:9090/api/tracking/history/flight/A12345?fromTime=2024-12-01T00:00:00&toTime=2024-12-01T23:59:59"
```

**Example Response:**
```json
{
  "success": true,
  "message": "Found 245 tracking points for flight A12345",
  "data": [
    {
      "trackingId": 1001,
      "hexident": "A12345",
      "timestamp": "2024-12-01T10:30:00",
      "latitude": 10.7623,
      "longitude": 106.6821,
      "altitude": 35000,
      "speed": 450,
      "callsign": "VN334"
    }
  ],
  "timestamp": "2024-12-01T12:00:00"
}
```

### 2. Get Recent Flight Data
Retrieves recent tracking data for a specific flight (last 24 hours).

**Endpoint:** `GET /api/tracking/history/flight/{hexident}/recent`

**Parameters:**
- `hexident` (path) - Aircraft hexident (ICAO24 code)

**Example Request:**
```bash
curl "http://localhost:9090/api/tracking/history/flight/A12345/recent"
```

**Example Response:**
```json
{
  "success": true,
  "message": "Recent flight data retrieved successfully",
  "data": {
    "hexident": "A12345",
    "totalPoints": 124,
    "timeRange": "24 hours",
    "trackingPoints": [
      {
        "latitude": 10.7623,
        "longitude": 106.6821,
        "altitude": 35000,
        "speed": 450,
        "timestamp": "2024-12-01T10:30:00"
      }
    ]
  }
}
```

---

## Ship History APIs

### 3. Get Ship Tracking History
Retrieves historical tracking data for a specific ship within a time range.

**Endpoint:** `GET /api/tracking/history/ship/{mmsi}`

**Parameters:**
- `mmsi` (path) - Ship MMSI (Maritime Mobile Service Identity), e.g., "574123456"
- `fromTime` (query, required) - Start time in ISO format
- `toTime` (query, required) - End time in ISO format

**Example Request:**
```bash
curl "http://localhost:9090/api/tracking/history/ship/574123456?fromTime=2024-12-01T00:00:00&toTime=2024-12-01T23:59:59"
```

**Example Response:**
```json
{
  "success": true,
  "message": "Found 186 tracking points for ship 574123456",
  "data": [
    {
      "id": 2001,
      "mmsi": "574123456",
      "timestamp": "2024-12-01T14:20:00",
      "latitude": 10.3456,
      "longitude": 107.1234,
      "speed": 12.5,
      "course": 285.0,
      "navigationStatus": "Under way using engine"
    }
  ]
}
```

### 4. Get Recent Ship Data
Retrieves recent tracking data for a specific ship (last 24 hours).

**Endpoint:** `GET /api/tracking/history/ship/{mmsi}/recent`

**Parameters:**
- `mmsi` (path) - Ship MMSI

**Example Request:**
```bash
curl "http://localhost:9090/api/tracking/history/ship/574123456/recent"
```

---

## Geographic Query APIs

### 5. Get Flights in Area
Retrieves current flights within a specified geographic area.

**Endpoint:** `GET /api/tracking/history/flights/area`

**Parameters:**
- `minLat` (query, required) - Minimum latitude, e.g., 10.0
- `maxLat` (query, required) - Maximum latitude, e.g., 11.0
- `minLon` (query, required) - Minimum longitude, e.g., 106.0
- `maxLon` (query, required) - Maximum longitude, e.g., 107.0

**Example Request:**
```bash
curl "http://localhost:9090/api/tracking/history/flights/area?minLat=10.0&maxLat=11.0&minLon=106.0&maxLon=107.0"
```

**Example Response:**
```json
{
  "success": true,
  "message": "Found 3 flights in specified area",
  "data": [
    {
      "hexIdent": "A12345",
      "callsign": "VN334",
      "latitude": 10.7623,
      "longitude": 106.6821,
      "altitude": 35000,
      "groundSpeed": 450,
      "lastSeen": "2024-12-01T10:30:00"
    }
  ]
}
```

### 6. Get Ships in Area
Retrieves current ships within a specified geographic area.

**Endpoint:** `GET /api/tracking/history/ships/area`

**Parameters:** Same as flights in area

**Example Request:**
```bash
curl "http://localhost:9090/api/tracking/history/ships/area?minLat=10.0&maxLat=11.0&minLon=106.0&maxLon=107.0"
```

---

## System Statistics API

### 7. Get System Tracking Statistics
Retrieves overall system statistics including active flights, ships, and recent activity.

**Endpoint:** `GET /api/tracking/history/statistics`

**Example Request:**
```bash
curl "http://localhost:9090/api/tracking/history/statistics"
```

**Example Response:**
```json
{
  "success": true,
  "message": "System statistics retrieved successfully",
  "data": {
    "activeFlightsInRedis": 15,
    "activeShipsInRedis": 8,
    "recentFlightRecords": 1247,
    "recentShipRecords": 432,
    "timestamp": "2024-12-01T12:00:00"
  }
}
```

---

## Error Handling

All APIs return a consistent error format:

```json
{
  "success": false,
  "message": "Error description here",
  "timestamp": "2024-12-01T12:00:00"
}
```

**Common HTTP Status Codes:**
- `200` - Success
- `400` - Bad Request (invalid parameters)
- `404` - Not Found (no data available)
- `500` - Internal Server Error

---

## Data Types and Formats

### Time Format
All timestamps use ISO 8601 format: `YYYY-MM-DDTHH:mm:ss`

### Coordinates
- Latitude: Decimal degrees (-90 to 90)
- Longitude: Decimal degrees (-180 to 180)

### Flight Data Fields
- `hexident`: ICAO24 code (6-character hex)
- `callsign`: Flight callsign
- `altitude`: Feet above sea level
- `speed`: Ground speed in knots

### Ship Data Fields
- `mmsi`: 9-digit Maritime Mobile Service Identity
- `speed`: Speed over ground in knots
- `course`: Course over ground in degrees (0-359)
- `heading`: True heading in degrees

---

## Integration Examples

### Frontend Integration (JavaScript)
```javascript
// Get recent flight data
async function getRecentFlightData(hexident) {
  try {
    const response = await fetch(
      `http://localhost:9090/api/tracking/history/flight/${hexident}/recent`
    );
    const data = await response.json();
    
    if (data.success) {
      console.log('Tracking points:', data.data.trackingPoints);
      return data.data;
    } else {
      console.error('API Error:', data.message);
    }
  } catch (error) {
    console.error('Network Error:', error);
  }
}

// Get ships in area
async function getShipsInArea(bounds) {
  const { minLat, maxLat, minLon, maxLon } = bounds;
  const params = new URLSearchParams({
    minLat, maxLat, minLon, maxLon
  });
  
  try {
    const response = await fetch(
      `http://localhost:9090/api/tracking/history/ships/area?${params}`
    );
    const data = await response.json();
    return data.data || [];
  } catch (error) {
    console.error('Error fetching ships:', error);
    return [];
  }
}
```

### Python Integration
```python
import requests
from datetime import datetime, timedelta

def get_flight_history(hexident, hours_back=24):
    end_time = datetime.now()
    start_time = end_time - timedelta(hours=hours_back)
    
    params = {
        'fromTime': start_time.isoformat(),
        'toTime': end_time.isoformat()
    }
    
    response = requests.get(
        f'http://localhost:9090/api/tracking/history/flight/{hexident}',
        params=params
    )
    
    if response.status_code == 200:
        data = response.json()
        return data['data'] if data['success'] else []
    return []
```

---

## Performance Notes

- Historical queries are optimized with database indexes on timestamp fields
- Real-time data queries use Redis for faster response times
- Geographic area queries have coordinate validation
- Large time ranges may take longer to process

---

## Swagger/OpenAPI Documentation

Complete API documentation is available at: `http://localhost:9090/swagger-ui.html`

The OpenAPI spec includes:
- Interactive API testing
- Parameter descriptions and examples
- Response schema definitions
- Error code explanations 