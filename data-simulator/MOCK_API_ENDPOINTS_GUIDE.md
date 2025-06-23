# Mock API Endpoints Guide

## 📋 **Tổng quan**

**Data Simulator** đã được mở rộng để hỗ trợ **6 external API sources** với dữ liệu giả lập chân thực:

### **Aircraft Sources (2)**
- **FlightRadar24** (Priority 1, Quality: 95%)
- **ADS-B Exchange** (Priority 2, Quality: 88%)

### **Vessel Sources (4)**
- **MarineTraffic** (Priority 1, Quality: 92%)
- **VesselFinder** (Priority 2, Quality: 87%)
- **Chinaports** (Priority 3, Quality: 85%)
- **MarineTraffic V2** (Priority 4, Quality: 89%)

## 🎯 **Mock API Features**

### **📊 Realistic Data Quality Simulation**
- ✅ **Different quality scores** per source
- ✅ **Response time delays** based on real-world performance
- ✅ **Error rates** and data variance
- ✅ **Different data formats** per source

### **🔍 Source-Specific Characteristics**
- ✅ **Geographic coverage** (e.g., Chinaports focuses on China Sea)
- ✅ **Data richness** (e.g., VesselFinder has commercial details)
- ✅ **Update frequencies** (different intervals per source)
- ✅ **Priority ranking** for data fusion

## 🚀 **Starting the Mock API Server**

```bash
# Start the data simulator server
cd flightmap_backend/data-simulator
npm install
npm start

# Server will run on http://localhost:3001
```

## 📡 **Aircraft API Endpoints**

### **1. FlightRadar24 Mock API**
```bash
GET http://localhost:3001/api/mock/flightradar24

# With geographic bounds
GET http://localhost:3001/api/mock/flightradar24?bounds={"minLat":10,"maxLat":12,"minLon":106,"maxLon":108}
```

**Response Format:**
```json
{
  "success": true,
  "source": "flightradar24",
  "quality": 0.95,
  "priority": 1,
  "responseTime": 245,
  "data": {
    "full_count": 1250,
    "version": 4,
    "A12345": [
      "A12345",           // hexident
      10.762622,          // latitude
      106.660172,         // longitude
      090,                // heading
      35000,              // altitude
      450,                // speed
      "2000",             // squawk
      "T-MLAT",           // radar type
      "B737",             // aircraft type
      "VN-ABC",           // registration
      1642680900,         // timestamp
      "SGN",              // origin
      "HAN",              // destination
      "VN123",            // flight number
      0,                  // unknown
      0,                  // vertical speed
      "VN123"             // callsign
    ]
  }
}
```

### **2. ADS-B Exchange Mock API**
```bash
GET http://localhost:3001/api/mock/adsbexchange

# With bounds
GET http://localhost:3001/api/mock/adsbexchange?bounds={"minLat":10,"maxLat":12,"minLon":106,"maxLon":108}
```

**Response Format:**
```json
{
  "success": true,
  "source": "adsbexchange",
  "quality": 0.88,
  "priority": 2,
  "responseTime": 456,
  "data": {
    "ac": [
      {
        "hex": "A12345",
        "flight": "VN123",
        "lat": 10.762622,
        "lon": 106.660172,
        "alt_baro": 35000,
        "alt_geom": 35050,
        "gs": 450,
        "track": 090,
        "baro_rate": 0,
        "category": "A3",
        "nav_qnh": 1013.25,
        "nav_altitude_mcp": 35000,
        "nav_modes": ["autopilot", "althold"],
        "seen": 2.5,
        "rssi": -35.2,
        "messages": 1245,
        "seen_pos": 1.2,
        "emergency": "none",
        "spi": false,
        "alert": false
      }
    ],
    "total": 1,
    "ctime": 1642680900000,
    "ptime": 1642680870000
  }
}
```

## 🚢 **Vessel API Endpoints**

### **1. MarineTraffic Mock API**
```bash
GET http://localhost:3001/api/mock/marinetraffic

# With bounds
GET http://localhost:3001/api/mock/marinetraffic?bounds={"minLat":10,"maxLat":12,"minLon":106,"maxLon":108}
```

**Response Format:**
```json
{
  "success": true,
  "source": "marinetraffic",
  "quality": 0.92,
  "priority": 1,
  "responseTime": 312,
  "data": {
    "data": [
      {
        "MMSI": 574123456,
        "LAT": 10.762622,
        "LON": 106.660172,
        "SPEED": 12.5,
        "COURSE": 180,
        "HEADING": 180,
        "STATUS": "Under way using engine",
        "DRAUGHT": 8.5,
        "SHIPNAME": "SAIGON STAR",
        "SHIPTYPE": "Container",
        "FLAG": "VN",
        "LENGTH": 200,
        "WIDTH": 25,
        "TIMESTAMP": "2024-01-20T10:30:00.000Z"
      }
    ],
    "meta": {
      "total": 1,
      "last_update": "2024-01-20T10:30:00.000Z"
    }
  }
}
```

### **2. VesselFinder Mock API**
```bash
GET http://localhost:3001/api/mock/vesselfinder
```

**Response Format:**
```json
{
  "success": true,
  "source": "vesselfinder",
  "quality": 0.87,
  "priority": 2,
  "responseTime": 567,
  "data": {
    "vessels": [
      {
        "mmsi": 574123456,
        "imo": 9876543,
        "name": "SAIGON STAR",
        "lat": 10.762622,
        "lng": 106.660172,
        "speed": 12.5,
        "course": 180,
        "heading": 180,
        "nav_status": "Under way using engine",
        "ship_type": "Container",
        "flag": "VN",
        "length": 200,
        "width": 25,
        "eta": "2024-01-25T08:00:00.000Z",
        "destination": "HONG KONG",
        "callsign": "XV1234",
        "draught": 8.5,
        "year_built": 2015,
        "gross_tonnage": 25000,
        "dwt": 35000,
        "last_port": "VNSGN",
        "next_port": "HKHKG",
        "photos_count": 3,
        "last_update": "2024-01-20T10:30:00.000Z"
      }
    ],
    "count": 1,
    "status": "success",
    "timestamp": "2024-01-20T10:30:00.000Z"
  }
}
```

### **3. Chinaports Mock API**
```bash
GET http://localhost:3001/api/mock/chinaports
```

**Response Format:**
```json
{
  "success": true,
  "source": "chinaports",
  "quality": 0.85,
  "priority": 3,
  "responseTime": 789,
  "data": {
    "code": 200,
    "message": "success",
    "data": {
      "ships": [
        {
          "vessel_id": 574123456,
          "vessel_name_cn": "海港之星",
          "vessel_name_en": "SAIGON STAR",
          "position": {
            "latitude": 10.762622,
            "longitude": 106.660172,
            "coordinate_system": "WGS84"
          },
          "navigation": {
            "speed_knots": 12.5,
            "course_degrees": 180,
            "heading_degrees": 180,
            "nav_status_code": "UWE"
          },
          "vessel_info": {
            "ship_type_code": "CON",
            "flag_state": "VN",
            "length_m": 200,
            "beam_m": 25,
            "draught_m": 8.5,
            "grt": 25000
          },
          "port_info": {
            "last_port_code": "CNSHA",
            "next_port_code": "CNQIN",
            "eta_local": "2024-01-25T08:00:00.000Z"
          },
          "update_time": "2024-01-20T10:30:00.000Z",
          "data_source": "chinaports",
          "reliability": "medium"
        }
      ],
      "total": 1,
      "region": "China Sea",
      "update_time": "2024-01-20T10:30:00.000Z"
    }
  }
}
```

### **4. MarineTraffic V2 Mock API**
```bash
GET http://localhost:3001/api/mock/marinetrafficv2
```

**Response Format:**
```json
{
  "success": true,
  "source": "marinetrafficv2",
  "quality": 0.89,
  "priority": 4,
  "responseTime": 345,
  "data": {
    "response_code": 200,
    "response_text": "OK",
    "data": {
      "positions": [
        {
          "MMSI": 574123456,
          "IMO": 9876543,
          "SHIPNAME": "SAIGON STAR",
          "LAT": 10.762622,
          "LON": 106.660172,
          "SPEED": 12.5,
          "COURSE": 180,
          "HEADING": 180,
          "STATUS": "Under way using engine",
          "SHIPTYPE": "Container",
          "FLAG": "VN",
          "LENGTH": 200,
          "WIDTH": 25,
          "DRAUGHT": 8.5,
          "CALLSIGN": "XV1234",
          "DESTINATION": "HONG KONG",
          "ETA": "2024-01-25T08:00:00.000Z",
          "AIS_VERSION": "2",
          "ROT": 0,
          "NAV_STATUS_ID": 0,
          "TYPE_NAME": "Container Ship (Cellular)",
          "DWT": 35000,
          "YEAR_BUILT": 2015,
          "GT": 25000,
          "OWNER": "COSCO Shipping",
          "MANAGER": "Fleet Management Ltd",
          "LAST_PORT": "VNSGN",
          "LAST_PORT_TIME": "2024-01-18T15:30:00.000Z",
          "NEXT_PORT": "HKHKG",
          "CURRENT_PORT": null,
          "PHOTOS": 2,
          "NOTES": "",
          "TIMESTAMP": "2024-01-20T10:30:00.000Z"
        }
      ],
      "meta": {
        "total_count": 1,
        "api_version": "v2",
        "request_timestamp": "2024-01-20T10:30:00.000Z"
      }
    }
  }
}
```

## 🔄 **Multi-Source Endpoints**

### **Get All Aircraft Sources**
```bash
GET http://localhost:3001/api/mock/aircraft/all

# With bounds
GET http://localhost:3001/api/mock/aircraft/all?bounds={"minLat":10,"maxLat":12,"minLon":106,"maxLon":108}
```

**Response:**
```json
{
  "success": true,
  "sources": ["flightradar24", "adsbexchange"],
  "timestamp": "2024-01-20T10:30:00.000Z",
  "data": {
    "flightradar24": { /* FlightRadar24 data */ },
    "adsbexchange": { /* ADS-B Exchange data */ }
  }
}
```

### **Get All Vessel Sources**
```bash
GET http://localhost:3001/api/mock/vessels/all

# With bounds
GET http://localhost:3001/api/mock/vessels/all?bounds={"minLat":10,"maxLat":12,"minLon":106,"maxLon":108}
```

**Response:**
```json
{
  "success": true,
  "sources": ["marinetraffic", "vesselfinder", "chinaports", "marinetrafficv2"],
  "timestamp": "2024-01-20T10:30:00.000Z",
  "data": {
    "marinetraffic": { /* MarineTraffic data */ },
    "vesselfinder": { /* VesselFinder data */ },
    "chinaports": { /* Chinaports data */ },
    "marinetrafficv2": { /* MarineTraffic V2 data */ }
  }
}
```

## 📊 **Statistics Endpoint**

### **Get Mock API Statistics**
```bash
GET http://localhost:3001/api/mock/stats
```

**Response:**
```json
{
  "success": true,
  "timestamp": "2024-01-20T10:30:00.000Z",
  "stats": {
    "flights": {
      "total": 1250,
      "lastUpdate": "2024-01-20T10:29:45.000Z",
      "sources": {
        "flightradar24": {
          "quality": 0.95,
          "priority": 1,
          "updateInterval": 30000
        },
        "adsbexchange": {
          "quality": 0.88,
          "priority": 2,
          "updateInterval": 35000
        }
      }
    },
    "ships": {
      "total": 850,
      "lastUpdate": "2024-01-20T10:28:30.000Z",
      "sources": {
        "marinetraffic": {
          "quality": 0.92,
          "priority": 1,
          "updateInterval": 60000
        },
        "vesselfinder": {
          "quality": 0.87,
          "priority": 2,
          "updateInterval": 70000
        },
        "chinaports": {
          "quality": 0.85,
          "priority": 3,
          "updateInterval": 90000
        },
        "marinetrafficv2": {
          "quality": 0.89,
          "priority": 4,
          "updateInterval": 80000
        }
      }
    },
    "movementSimulator": {
      "activeFlights": 1250,
      "activeShips": 850
    }
  }
}
```

## 🎛️ **Testing Multi-Source Data Fusion**

### **Simulate Backend Integration Test**
```bash
# Test FlightRadar24 vs ADS-B Exchange data differences
curl "http://localhost:3001/api/mock/flightradar24?bounds={\"minLat\":10,\"maxLat\":12,\"minLon\":106,\"maxLon\":108}"
curl "http://localhost:3001/api/mock/adsbexchange?bounds={\"minLat\":10,\"maxLat\":12,\"minLon\":106,\"maxLon\":108}"

# Test all vessel sources for same area
curl "http://localhost:3001/api/mock/vessels/all?bounds={\"minLat\":10,\"maxLat\":12,\"minLon\":106,\"maxLon\":108}"
```

### **Verify Data Quality Variance**
```bash
# Multiple calls to same endpoint - should show slight data variations
curl "http://localhost:3001/api/mock/flightradar24" # Call 1
curl "http://localhost:3001/api/mock/flightradar24" # Call 2 - slight differences due to quality simulation
```

### **Test Response Time Simulation**
```bash
# Different sources should have different response times
time curl "http://localhost:3001/api/mock/flightradar24"     # ~100-500ms
time curl "http://localhost:3001/api/mock/adsbexchange"      # ~200-800ms
time curl "http://localhost:3001/api/mock/chinaports"       # ~500-1200ms
```

## 🔧 **Configuration**

### **Adjusting Data Quality & Response Times**
Edit `config.js`:
```javascript
mockApis: {
  flightradar24: {
    quality: 0.95,           // 95% data accuracy
    responseDelay: { min: 100, max: 500 },  // 100-500ms response time
    errorRate: 0.02          // 2% error rate
  },
  // ... other sources
}
```

### **Geographic Coverage Settings**
```javascript
chinaports: {
  geoBounds: {
    minLatitude: 3.0,    // Only China Sea region
    maxLatitude: 25.0,
    minLongitude: 99.0,
    maxLongitude: 125.0
  }
}
```

## 💡 **Integration with Backend**

### **Update Backend External API URLs**
In `application.properties`:
```properties
# Point to mock API server for testing
external.api.flightradar24.url=http://localhost:3001/api/mock/flightradar24
external.api.adsbexchange.url=http://localhost:3001/api/mock/adsbexchange
external.api.marinetraffic.url=http://localhost:3001/api/mock/marinetraffic
external.api.vesselfinder.url=http://localhost:3001/api/mock/vesselfinder
external.api.chinaports.url=http://localhost:3001/api/mock/chinaports
external.api.marinetrafficv2.url=http://localhost:3001/api/mock/marinetrafficv2
```

### **Test Data Fusion**
```bash
# Start mock API server
cd data-simulator && npm start

# Start backend server with mock API URLs
cd .. && ./gradlew bootRun

# Test multi-source data collection
curl "http://localhost:9090/api/data-sources/collect/aircraft"
curl "http://localhost:9090/api/data-sources/collect/vessels"
```

## 📋 **Summary**

**Mock API Server cung cấp:**

| **Feature** | **Description** |
|-------------|-----------------|
| **6 External Sources** | FlightRadar24, ADS-B Exchange, MarineTraffic, VesselFinder, Chinaports, MarineTraffic V2 |
| **Realistic Data Quality** | Different quality scores and error rates per source |
| **Response Time Simulation** | Variable delays to simulate real-world API performance |
| **Geographic Coverage** | Source-specific coverage areas (e.g., Chinaports for China Sea) |
| **Data Format Diversity** | Different response formats per source for testing |
| **Multi-Source Testing** | Endpoints to test all sources simultaneously |

**Perfect for:** Testing data fusion algorithms, quality scoring, priority handling, and multi-source integration!

**Result:** Complete mock environment cho việc test và develop multi-source data fusion system! 🎉 