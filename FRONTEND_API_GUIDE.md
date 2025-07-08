# 🚀 Frontend API Usage Guide

## Flight & Vessel Tracking APIs

> **Phiên bản**: 2.0.0  
> **Cập nhật**: Tháng 6, 2025  
> **Dành cho**: Frontend Developers

---

## 📋 **Mục lục**

1. [Tổng quan](#tổng-quan)
2. [Authentication](#authentication)
3. [REST APIs](#rest-apis)
4. [WebSocket APIs](#websocket-apis)
5. [Error Handling](#error-handling)
6. [Code Examples](#code-examples)
7. [Best Practices](#best-practices)

---

## 🎯 **Tổng quan**

### **Server Information**

- **Base URL**: `http://localhost:9090`
- **Production**: `https://api.tracking.example.com`
- **WebSocket**: `ws://localhost:9090/ws`

### **Available APIs**

- **REST API**: HTTP endpoints cho CRUD operations
- **WebSocket**: Real-time updates cho aircraft/vessel tracking
- **Kafka**: Backend event streaming (không trực tiếp từ frontend)

### **Documentation Links**

- **Swagger UI**: http://localhost:9090/swagger-ui.html
- **AsyncAPI UI**: http://localhost:9090/asyncapi-ui
- **API Overview**: http://localhost:9090/docs

---

## 🔐 **Authentication**

### **JWT Token Authentication**

#### **1. User Registration**

```javascript
const register = async (userData) => {
  const response = await fetch("http://localhost:9090/api/v1/auth/register", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      username: userData.username,
      email: userData.email,
      password: userData.password,
      fullName: userData.fullName,
    }),
  });

  if (!response.ok) {
    throw new Error("Registration failed");
  }

  const result = await response.json();
  return result.data; // { accessToken, refreshToken, user }
};
```

#### **2. User Login**

```javascript
const login = async (credentials) => {
  const response = await fetch("http://localhost:9090/api/v1/auth/login", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      username: credentials.username,
      password: credentials.password,
    }),
  });

  const result = await response.json();

  if (result.success) {
    // Lưu tokens
    localStorage.setItem("accessToken", result.data.accessToken);
    localStorage.setItem("refreshToken", result.data.refreshToken);
    return result.data;
  } else {
    throw new Error(result.message);
  }
};
```

#### **3. Token Refresh**

```javascript
const refreshToken = async () => {
  const refreshToken = localStorage.getItem("refreshToken");

  const response = await fetch(
    "http://localhost:9090/api/v1/auth/refresh-token",
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ refreshToken }),
    }
  );

  const result = await response.json();

  if (result.success) {
    localStorage.setItem("accessToken", result.data.accessToken);
    return result.data.accessToken;
  } else {
    // Redirect to login
    localStorage.clear();
    window.location.href = "/login";
  }
};
```

#### **4. Authenticated Request Helper**

```javascript
const apiRequest = async (url, options = {}) => {
  const token = localStorage.getItem("accessToken");

  const config = {
    ...options,
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
      ...options.headers,
    },
  };

  let response = await fetch(url, config);

  // Handle token expiration
  if (response.status === 401) {
    try {
      await refreshToken();
      // Retry with new token
      config.headers.Authorization = `Bearer ${localStorage.getItem(
        "accessToken"
      )}`;
      response = await fetch(url, config);
    } catch (error) {
      // Redirect to login
      window.location.href = "/login";
      return;
    }
  }

  return response;
};
```

---

## 🌐 **REST APIs**

### **Flight Tracking APIs**

#### **1. Get All Flight Tracking Data (với Pagination)**

```javascript
const getFlightTrackings = async (
  page = 0,
  size = 10,
  sortBy = "updateTime",
  direction = "desc"
) => {
  const url = `http://localhost:9090/api/flight-tracking/paginated?page=${page}&size=${size}&sortBy=${sortBy}&direction=${direction}`;

  const response = await apiRequest(url);
  const result = await response.json();

  return result.data; // { content: [], totalElements, totalPages, ... }
};
```

#### **2. Search Flights by Location (Radius)**

```javascript
const searchFlightsByRadius = async (longitude, latitude, radiusInMeters) => {
  const url = `http://localhost:9090/api/flight-tracking/radius?longitude=${longitude}&latitude=${latitude}&radiusInMeters=${radiusInMeters}`;

  const response = await apiRequest(url);
  const result = await response.json();

  return result.data; // Array of flight tracking data
};

// Example usage
const nearbyFlights = await searchFlightsByRadius(106.629, 10.823, 50000); // 50km radius from Ho Chi Minh City
```

#### **3. Get Flight Tracking by ID**

```javascript
const getFlightById = async (flightId) => {
  const response = await apiRequest(
    `http://localhost:9090/api/flight-tracking/${flightId}`
  );
  const result = await response.json();

  return result.data;
};
```

#### **4. Create New Flight Tracking**

```javascript
const createFlightTracking = async (trackingData) => {
  const response = await apiRequest(
    "http://localhost:9090/api/flight-tracking",
    {
      method: "POST",
      body: JSON.stringify({
        callsign: trackingData.callsign,
        latitude: trackingData.latitude,
        longitude: trackingData.longitude,
        altitude: trackingData.altitude,
        speed: trackingData.speed,
        heading: trackingData.heading,
        // ... other fields
      }),
    }
  );

  const result = await response.json();
  return result.data;
};
```

### **Ship Tracking APIs**

#### **1. Get Ship Tracking Data**

```javascript
const getShipTrackings = async (page = 0, size = 10) => {
  const url = `http://localhost:9090/api/ship-tracking/paginated?page=${page}&size=${size}`;

  const response = await apiRequest(url);
  const result = await response.json();

  return result.data;
};
```

#### **2. Search Ships by Area**

```javascript
const searchShipsByArea = async (minLat, maxLat, minLon, maxLon) => {
  const url = `http://localhost:9090/api/ship-tracking/area?minLat=${minLat}&maxLat=${maxLat}&minLon=${minLon}&maxLon=${maxLon}`;

  const response = await apiRequest(url);
  const result = await response.json();

  return result.data;
};
```

### **Aircraft & Ship Management**

#### **1. Get Aircraft Information**

```javascript
const getAircraftList = async () => {
  const response = await apiRequest("http://localhost:9090/api/aircraft");
  const result = await response.json();

  return result.data;
};

const getAircraftById = async (aircraftId) => {
  const response = await apiRequest(
    `http://localhost:9090/api/aircraft/${aircraftId}`
  );
  const result = await response.json();

  return result.data;
};
```

#### **2. Ship Management**

```javascript
const getShipList = async () => {
  const response = await apiRequest("http://localhost:9090/api/ship");
  const result = await response.json();

  return result.data;
};
```

---

## 📡 **WebSocket APIs**

### **WebSocket Connection Setup**

#### **1. Install Dependencies**

```bash
npm install sockjs-client stompjs
# hoặc
npm install @stomp/stompjs
```

#### **2. WebSocket Connection Class**

```javascript
import SockJS from "sockjs-client";
import { Stomp } from "@stomp/stompjs";

class TrackingWebSocket {
  constructor() {
    this.stompClient = null;
    this.connected = false;
    this.subscriptions = new Map();
  }

  connect() {
    return new Promise((resolve, reject) => {
      const socket = new SockJS("http://localhost:9090/ws");
      this.stompClient = Stomp.over(socket);

      // Disable debug logging
      this.stompClient.debug = null;

      this.stompClient.connect(
        {},
        (frame) => {
          console.log("Connected to WebSocket:", frame);
          this.connected = true;
          resolve(frame);
        },
        (error) => {
          console.error("WebSocket connection error:", error);
          this.connected = false;
          reject(error);
        }
      );
    });
  }

  disconnect() {
    if (this.stompClient && this.connected) {
      this.stompClient.disconnect();
      this.connected = false;
      this.subscriptions.clear();
    }
  }

  isConnected() {
    return this.connected;
  }
}
```

### **Real-time Aircraft Tracking**

#### **1. Subscribe to Geographic Area Updates**

```javascript
class AircraftTracker extends TrackingWebSocket {
  subscribeToArea(minLat, maxLat, minLon, maxLon, callback) {
    if (!this.connected) {
      throw new Error("WebSocket not connected");
    }

    // Subscribe to area updates
    const areaId = `${minLat}_${maxLat}_${minLon}_${maxLon}`;
    const subscription = this.stompClient.subscribe(
      `/topic/area-updates/${areaId}`,
      (message) => {
        const data = JSON.parse(message.body);
        callback(data);
      }
    );

    this.subscriptions.set(`area_${areaId}`, subscription);

    // Send subscription request
    this.stompClient.send(
      "/app/subscribe-area",
      {},
      JSON.stringify({
        minLat: minLat,
        maxLat: maxLat,
        minLon: minLon,
        maxLon: maxLon,
      })
    );

    return areaId;
  }

  unsubscribeFromArea(areaId) {
    const subscription = this.subscriptions.get(`area_${areaId}`);
    if (subscription) {
      subscription.unsubscribe();
      this.subscriptions.delete(`area_${areaId}`);

      // Send unsubscribe request
      const [minLat, maxLat, minLon, maxLon] = areaId.split("_");
      this.stompClient.send(
        "/app/unsubscribe-area",
        {},
        JSON.stringify({
          minLat: parseFloat(minLat),
          maxLat: parseFloat(maxLat),
          minLon: parseFloat(minLon),
          maxLon: parseFloat(maxLon),
        })
      );
    }
  }
}
```

#### **2. Subscribe to Specific Aircraft**

```javascript
subscribeToAircraft(hexIdent, callback) {
  if (!this.connected) {
    throw new Error('WebSocket not connected');
  }

  // Subscribe to aircraft updates
  const subscription = this.stompClient.subscribe('/topic/aircraft-updates', (message) => {
    const data = JSON.parse(message.body);
    // Filter for specific aircraft
    if (data.hexIdent === hexIdent) {
      callback(data);
    }
  });

  this.subscriptions.set(`aircraft_${hexIdent}`, subscription);

  // Send subscription request
  this.stompClient.send('/app/subscribe-aircraft', {}, JSON.stringify({
    hexIdent: hexIdent
  }));

  return hexIdent;
}

unsubscribeFromAircraft(hexIdent) {
  const subscription = this.subscriptions.get(`aircraft_${hexIdent}`);
  if (subscription) {
    subscription.unsubscribe();
    this.subscriptions.delete(`aircraft_${hexIdent}`);

    this.stompClient.send('/app/unsubscribe-aircraft', {}, JSON.stringify({
      hexIdent: hexIdent
    }));
  }
}
```

### **Usage Example - React Component**

```javascript
import React, { useEffect, useState } from "react";

const FlightMap = () => {
  const [aircraftTracker, setAircraftTracker] = useState(null);
  const [aircraftData, setAircraftData] = useState([]);
  const [connected, setConnected] = useState(false);

  useEffect(() => {
    const tracker = new AircraftTracker();

    // Connect to WebSocket
    tracker
      .connect()
      .then(() => {
        setConnected(true);
        setAircraftTracker(tracker);

        // Subscribe to Ho Chi Minh City area
        tracker.subscribeToArea(10.0, 11.0, 106.0, 107.0, (data) => {
          setAircraftData((prev) => {
            // Update aircraft data
            const updated = [...prev];
            data.aircraft.forEach((aircraft) => {
              const index = updated.findIndex(
                (a) => a.hexIdent === aircraft.hexIdent
              );
              if (index >= 0) {
                updated[index] = aircraft;
              } else {
                updated.push(aircraft);
              }
            });
            return updated;
          });
        });
      })
      .catch((error) => {
        console.error("Connection failed:", error);
        setConnected(false);
      });

    // Cleanup on unmount
    return () => {
      if (tracker) {
        tracker.disconnect();
      }
    };
  }, []);

  return (
    <div>
      <h2>Real-time Flight Tracking</h2>
      <p>Status: {connected ? "✅ Connected" : "❌ Disconnected"}</p>
      <p>Aircraft Count: {aircraftData.length}</p>

      {aircraftData.map((aircraft) => (
        <div
          key={aircraft.hexIdent}
          style={{ border: "1px solid #ccc", margin: "10px", padding: "10px" }}
        >
          <h4>{aircraft.callsign || aircraft.hexIdent}</h4>
          <p>
            Position: {aircraft.latitude.toFixed(4)},{" "}
            {aircraft.longitude.toFixed(4)}
          </p>
          <p>Altitude: {aircraft.altitude} ft</p>
          <p>Speed: {aircraft.speed} knots</p>
          <p>
            Last Update: {new Date(aircraft.timestamp).toLocaleTimeString()}
          </p>
        </div>
      ))}
    </div>
  );
};

export default FlightMap;
```

---

## ❗ **Error Handling**

### **1. API Response Format**

```javascript
// Success Response
{
  "success": true,
  "message": "Operation completed successfully",
  "data": { ... },
  "timestamp": [2025, 6, 16, 10, 30, 45, 123456789]
}

// Error Response
{
  "success": false,
  "message": "Error description",
  "data": null,
  "timestamp": [2025, 6, 16, 10, 30, 45, 123456789]
}
```

### **2. Error Handling Utility**

```javascript
const handleApiResponse = async (response) => {
  const result = await response.json();

  if (!result.success) {
    throw new Error(result.message || "API request failed");
  }

  return result.data;
};

// Usage
try {
  const data = await handleApiResponse(
    await apiRequest("http://localhost:9090/api/flight-tracking")
  );
  console.log("Success:", data);
} catch (error) {
  console.error("Error:", error.message);
  // Show error to user
}
```

### **3. WebSocket Error Handling**

```javascript
class RobustWebSocket extends TrackingWebSocket {
  constructor() {
    super();
    this.reconnectAttempts = 0;
    this.maxReconnectAttempts = 5;
    this.reconnectInterval = 1000; // 1 second
  }

  connect() {
    return super.connect().catch((error) => {
      console.error("Initial connection failed:", error);
      this.attemptReconnect();
      throw error;
    });
  }

  attemptReconnect() {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      console.log(
        `Reconnection attempt ${this.reconnectAttempts}/${this.maxReconnectAttempts}`
      );

      setTimeout(() => {
        this.connect()
          .then(() => {
            this.reconnectAttempts = 0;
            console.log("Reconnected successfully");
          })
          .catch(() => {
            this.attemptReconnect();
          });
      }, this.reconnectInterval * this.reconnectAttempts);
    } else {
      console.error("Max reconnection attempts reached");
    }
  }
}
```

---

## 💡 **Best Practices**

### **1. Performance Optimization**

```javascript
// Debounce map updates
const debounce = (func, wait) => {
  let timeout;
  return function executedFunction(...args) {
    const later = () => {
      clearTimeout(timeout);
      func(...args);
    };
    clearTimeout(timeout);
    timeout = setTimeout(later, wait);
  };
};

const debouncedMapUpdate = debounce((aircraftData) => {
  updateMapMarkers(aircraftData);
}, 500);
```

### **2. Memory Management**

```javascript
// Limit stored aircraft data
const MAX_AIRCRAFT_HISTORY = 100;

const addAircraftData = (newData) => {
  setAircraftData((prev) => {
    const updated = [...prev, newData];
    if (updated.length > MAX_AIRCRAFT_HISTORY) {
      return updated.slice(-MAX_AIRCRAFT_HISTORY);
    }
    return updated;
  });
};
```

### **3. Caching Strategy**

```javascript
// Simple cache for API responses
class ApiCache {
  constructor(ttl = 60000) {
    // 1 minute TTL
    this.cache = new Map();
    this.ttl = ttl;
  }

  set(key, data) {
    this.cache.set(key, {
      data,
      timestamp: Date.now(),
    });
  }

  get(key) {
    const item = this.cache.get(key);
    if (!item) return null;

    if (Date.now() - item.timestamp > this.ttl) {
      this.cache.delete(key);
      return null;
    }

    return item.data;
  }
}

const apiCache = new ApiCache(30000); // 30 seconds

const getCachedFlightData = async (flightId) => {
  const cacheKey = `flight_${flightId}`;
  let data = apiCache.get(cacheKey);

  if (!data) {
    data = await getFlightById(flightId);
    apiCache.set(cacheKey, data);
  }

  return data;
};
```

---

## 🔧 **Configuration**

### **Environment Variables**

```javascript
// config.js
const config = {
  development: {
    API_BASE_URL: "http://localhost:9090",
    WS_URL: "ws://localhost:9090/ws",
    MAX_RECONNECT_ATTEMPTS: 5,
    CACHE_TTL: 30000,
  },
  production: {
    API_BASE_URL: "https://api.tracking.example.com",
    WS_URL: "wss://api.tracking.example.com/ws",
    MAX_RECONNECT_ATTEMPTS: 3,
    CACHE_TTL: 60000,
  },
};

export default config[process.env.NODE_ENV || "development"];
```

---

## 📞 **Support & Resources**

### **API Documentation**

- **Swagger UI**: http://localhost:9090/swagger-ui.html
- **AsyncAPI**: http://localhost:9090/asyncapi-ui
- **GitHub**: https://github.com/Phambanam99/flightmap_backend

### **Contact**

- **Email**: phambanam99@gmail.com
- **GitHub Issues**: For bug reports and feature requests

### **Update Notes**

- **v2.0.0**: Added WebSocket real-time updates, improved authentication
- **v1.x.x**: Basic REST API implementation

---

> **📝 Lưu ý**: Documentation này được cập nhật thường xuyên. Hãy check phiên bản mới nhất trên repository.
