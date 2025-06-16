const express = require('express');
const cors = require('cors');
const SimulationService = require('./services/simulationService');
const config = require('./config');
const MockApiService = require('./services/mockApiService');

const app = express();
const simulationService = new SimulationService();
const mockApiService = new MockApiService();

// Middleware
app.use(cors());
app.use(express.json());

// Health check endpoint
app.get('/health', (req, res) => {
  res.json({
    status: 'healthy',
    timestamp: new Date().toISOString(),
    service: 'tracking-data-simulator',
    version: '1.0.0'
  });
});

// Get simulation status
app.get('/api/status', async (req, res) => {
  try {
    const status = simulationService.getStatus();
    res.json({
      success: true,
      data: status,
      timestamp: new Date().toISOString()
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: error.message,
      timestamp: new Date().toISOString()
    });
  }
});

// Get detailed simulation status
app.get('/api/status/detailed', async (req, res) => {
  try {
    const status = await simulationService.getDetailedStatus();
    res.json({
      success: true,
      data: status,
      timestamp: new Date().toISOString()
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: error.message,
      timestamp: new Date().toISOString()
    });
  }
});

// Start simulation
app.post('/api/simulation/start', async (req, res) => {
  try {
    const options = req.body || {};
    const status = await simulationService.startSimulation(options);
    
    res.json({
      success: true,
      message: 'Simulation started successfully',
      data: status,
      timestamp: new Date().toISOString()
    });
  } catch (error) {
    res.status(400).json({
      success: false,
      message: error.message,
      timestamp: new Date().toISOString()
    });
  }
});

// Stop simulation
app.post('/api/simulation/stop', (req, res) => {
  try {
    const status = simulationService.stopSimulation();
    
    res.json({
      success: true,
      message: 'Simulation stopped successfully',
      data: status,
      timestamp: new Date().toISOString()
    });
  } catch (error) {
    res.status(400).json({
      success: false,
      message: error.message,
      timestamp: new Date().toISOString()
    });
  }
});

// Start airport scenario
app.post('/api/simulation/scenarios/airport', async (req, res) => {
  try {
    const { airportLocation = 'tanSonNhat' } = req.body;
    const flights = await simulationService.startAirportScenario(airportLocation);
    
    res.json({
      success: true,
      message: `Airport scenario started at ${airportLocation}`,
      data: {
        flightsCreated: flights.length,
        flights: flights
      },
      timestamp: new Date().toISOString()
    });
  } catch (error) {
    res.status(400).json({
      success: false,
      message: error.message,
      timestamp: new Date().toISOString()
    });
  }
});

// Start port scenario
app.post('/api/simulation/scenarios/port', async (req, res) => {
  try {
    const ships = await simulationService.startPortScenario();
    
    res.json({
      success: true,
      message: 'Port scenario started',
      data: {
        shipsCreated: ships.length,
        ships: ships
      },
      timestamp: new Date().toISOString()
    });
  } catch (error) {
    res.status(400).json({
      success: false,
      message: error.message,
      timestamp: new Date().toISOString()
    });
  }
});

// Get configuration
app.get('/api/config', (req, res) => {
  res.json({
    success: true,
    data: {
      simulation: config.simulation,
      bounds: config.bounds,
      locations: config.locations,
      kafka: config.kafka
    },
    timestamp: new Date().toISOString()
  });
});

// Update configuration (runtime)
app.put('/api/config', (req, res) => {
  try {
    const updates = req.body;
    
    // Only allow updating simulation parameters
    if (updates.simulation) {
      Object.assign(config.simulation, updates.simulation);
    }
    
    res.json({
      success: true,
      message: 'Configuration updated successfully',
      data: {
        simulation: config.simulation
      },
      timestamp: new Date().toISOString()
    });
  } catch (error) {
    res.status(400).json({
      success: false,
      message: error.message,
      timestamp: new Date().toISOString()
    });
  }
});

// Get available locations/airports
app.get('/api/locations', (req, res) => {
  res.json({
    success: true,
    data: config.locations,
    timestamp: new Date().toISOString()
  });
});

// Manual flight generation
app.post('/api/manual/flight', async (req, res) => {
  try {
    const customData = req.body;
    let flight;
    
    if (Object.keys(customData).length > 0) {
      // Create flight with custom data
      flight = simulationService.movementSimulator.createNewFlight();
      Object.assign(flight, customData);
    } else {
      // Create random flight
      flight = simulationService.movementSimulator.createNewFlight();
    }
    
    await simulationService.kafkaService.publishFlightData(flight);
    
    res.json({
      success: true,
      message: 'Flight data published successfully',
      data: flight,
      timestamp: new Date().toISOString()
    });
  } catch (error) {
    res.status(400).json({
      success: false,
      message: error.message,
      timestamp: new Date().toISOString()
    });
  }
});

// Manual ship generation
app.post('/api/manual/ship', async (req, res) => {
  try {
    const customData = req.body;
    let ship;
    
    if (Object.keys(customData).length > 0) {
      // Create ship with custom data
      ship = simulationService.movementSimulator.createNewShip();
      Object.assign(ship, customData);
    } else {
      // Create random ship
      ship = simulationService.movementSimulator.createNewShip();
    }
    
    await simulationService.kafkaService.publishShipData(ship);
    
    res.json({
      success: true,
      message: 'Ship data published successfully',
      data: ship,
      timestamp: new Date().toISOString()
    });
  } catch (error) {
    res.status(400).json({
      success: false,
      message: error.message,
      timestamp: new Date().toISOString()
    });
  }
});

// API documentation endpoint
app.get('/api/docs', (req, res) => {
  const apiDocs = {
    title: 'Tracking Data Simulator API',
    version: '1.0.0',
    description: 'Real-time data simulator for flight and vessel tracking',
    endpoints: {
      health: {
        method: 'GET',
        path: '/health',
        description: 'Health check endpoint'
      },
      status: {
        method: 'GET',
        path: '/api/status',
        description: 'Get current simulation status'
      },
      detailedStatus: {
        method: 'GET',
        path: '/api/status/detailed',
        description: 'Get detailed simulation status including Kafka metadata'
      },
      startSimulation: {
        method: 'POST',
        path: '/api/simulation/start',
        description: 'Start real-time simulation',
        body: {
          flightInterval: 'number (ms)',
          shipInterval: 'number (ms)',
          maxFlights: 'number',
          maxShips: 'number',
          batchMode: 'boolean'
        }
      },
      stopSimulation: {
        method: 'POST',
        path: '/api/simulation/stop',
        description: 'Stop simulation'
      },
      airportScenario: {
        method: 'POST',
        path: '/api/simulation/scenarios/airport',
        description: 'Start airport scenario',
        body: {
          airportLocation: 'string (tanSonNhat, noiBai, hcmc, hanoi)'
        }
      },
      portScenario: {
        method: 'POST',
        path: '/api/simulation/scenarios/port',
        description: 'Start port scenario'
      },
      config: {
        method: 'GET',
        path: '/api/config',
        description: 'Get current configuration'
      },
      updateConfig: {
        method: 'PUT',
        path: '/api/config',
        description: 'Update simulation configuration'
      },
      locations: {
        method: 'GET',
        path: '/api/locations',
        description: 'Get available locations/airports'
      },
      manualFlight: {
        method: 'POST',
        path: '/api/manual/flight',
        description: 'Manually generate and publish flight data'
      },
      manualShip: {
        method: 'POST',
        path: '/api/manual/ship',
        description: 'Manually generate and publish ship data'
      },
      mockApis: {
        flightRadar24Mock: {
          method: 'GET',
          path: '/mock/flightradar24/zones/fcgi/feed.js',
          description: 'Mock FlightRadar24 API endpoint',
          params: {
            bounds: 'maxLat,minLat,minLon,maxLon'
          }
        },
        marineTrafficMock: {
          method: 'GET', 
          path: '/mock/marinetraffic/api/exportvessels/{apikey}/v:2/MINLAT:{lat}/MAXLAT:{lat}/MINLON:{lon}/MAXLON:{lon}/protocol:jsono',
          description: 'Mock MarineTraffic API endpoint'
        },
        mockStats: {
          method: 'GET',
          path: '/api/mock/stats',
          description: 'Get mock API statistics'
        },
        addMockFlight: {
          method: 'POST',
          path: '/api/mock/flights',
          description: 'Add flight to mock data'
        },
        addMockShip: {
          method: 'POST',
          path: '/api/mock/ships', 
          description: 'Add ship to mock data'
        }
      }
    }
  };
  
  res.json({
    success: true,
    data: apiDocs,
    timestamp: new Date().toISOString()
  });
});

// =============================================================================
// MOCK EXTERNAL APIs - FlightRadar24 & MarineTraffic
// =============================================================================

// Mock FlightRadar24 API
app.get('/mock/flightradar24/zones/fcgi/feed.js', (req, res) => {
  try {
    console.log('🎭 FlightRadar24 Mock API called');
    
    const bounds = parseBoundsFromQuery(req.query.bounds);
    const flightData = mockApiService.getFlightRadar24Data(bounds);
    
    // FlightRadar24 returns JSONP by default, but we'll return JSON
    res.setHeader('Content-Type', 'application/json');
    res.json(flightData);
    
  } catch (error) {
    console.error('❌ Mock FlightRadar24 API error:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Mock MarineTraffic API - flexible route to handle various formats
app.get('/mock/marinetraffic/api/exportvessels/*', (req, res) => {
  try {
    console.log('🎭 MarineTraffic Mock API called');
    console.log('URL params:', req.params[0]);
    
    const bounds = parseMarineTrafficBounds(req.params[0]);
    const marineData = mockApiService.getMarineTrafficData(bounds);
    
    res.json(marineData);
    
  } catch (error) {
    console.error('❌ Mock MarineTraffic API error:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Mock API Statistics
app.get('/api/mock/stats', (req, res) => {
  try {
    const stats = mockApiService.getStats();
    res.json({
      success: true,
      data: stats,
      timestamp: new Date().toISOString()
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: error.message,
      timestamp: new Date().toISOString()
    });
  }
});

// Add flight to mock data
app.post('/api/mock/flights', (req, res) => {
  try {
    const flightData = req.body;
    const hexident = mockApiService.addFlight(flightData);
    
    res.json({
      success: true,
      message: 'Flight added to mock data',
      data: { hexident, flightData },
      timestamp: new Date().toISOString()
    });
  } catch (error) {
    res.status(400).json({
      success: false,
      message: error.message,
      timestamp: new Date().toISOString()
    });
  }
});

// Add ship to mock data
app.post('/api/mock/ships', (req, res) => {
  try {
    const shipData = req.body;
    const mmsi = mockApiService.addShip(shipData);
    
    res.json({
      success: true,
      message: 'Ship added to mock data',
      data: { mmsi, shipData },
      timestamp: new Date().toISOString()
    });
  } catch (error) {
    res.status(400).json({
      success: false,
      message: error.message,
      timestamp: new Date().toISOString()
    });
  }
});

// Helper functions
function parseBoundsFromQuery(boundsParam) {
  if (!boundsParam) {
    return {
      maxLat: config.bounds.maxLatitude,
      minLat: config.bounds.minLatitude,
      minLon: config.bounds.minLongitude,
      maxLon: config.bounds.maxLongitude
    };
  }
  
  // FlightRadar24 format: "maxLat,minLat,minLon,maxLon"
  const parts = boundsParam.split(',').map(parseFloat);
  if (parts.length !== 4) {
    throw new Error('Invalid bounds format');
  }
  
  return {
    maxLat: parts[0],
    minLat: parts[1],
    minLon: parts[2],
    maxLon: parts[3]
  };
}

function parseMarineTrafficBounds(urlParts) {
  // Extract from URL parameters like MINLAT:8.5/MAXLAT:23.5/MINLON:102.0/MAXLON:109.5
  const bounds = {
    minLat: config.bounds.minLatitude,
    maxLat: config.bounds.maxLatitude,
    minLon: config.bounds.minLongitude,
    maxLon: config.bounds.maxLongitude
  };
  
  if (!urlParts) return bounds;
  
  const patterns = {
    MINLAT: /MINLAT:([\d.-]+)/,
    MAXLAT: /MAXLAT:([\d.-]+)/,
    MINLON: /MINLON:([\d.-]+)/,
    MAXLON: /MAXLON:([\d.-]+)/
  };
  
  Object.keys(patterns).forEach(key => {
    const match = urlParts.match(patterns[key]);
    if (match) {
      const boundKey = key.toLowerCase().replace('lat', 'Lat').replace('lon', 'Lon');
      bounds[boundKey] = parseFloat(match[1]);
    }
  });
  
  return bounds;
}

// Error handling middleware
app.use((err, req, res, next) => {
  console.error('Unhandled error:', err);
  res.status(500).json({
    success: false,
    message: 'Internal server error',
    timestamp: new Date().toISOString()
  });
});

// 404 handler
app.use((req, res) => {
  res.status(404).json({
    success: false,
    message: 'Endpoint not found',
    timestamp: new Date().toISOString()
  });
});

// Initialize and start server
async function startServer() {
  try {
    console.log('🚀 Starting Tracking Data Simulator...');
    console.log(`📍 Configuration: ${JSON.stringify(config.kafka, null, 2)}`);
    
    // Initialize simulation service
    await simulationService.initialize();
    
    // Start Express server
    const server = app.listen(config.port, () => {
      console.log(`✅ Server running on port ${config.port}`);
      console.log(`📊 Health check: http://localhost:${config.port}/health`);
      console.log(`📚 API documentation: http://localhost:${config.port}/api/docs`);
      console.log(`📈 Status endpoint: http://localhost:${config.port}/api/status`);
    });

    // Graceful shutdown
    process.on('SIGINT', async () => {
      console.log('\n🔄 Received SIGINT, shutting down gracefully...');
      
      server.close(() => {
        console.log('🔌 HTTP server closed');
      });
      
      await simulationService.shutdown();
      process.exit(0);
    });

    process.on('SIGTERM', async () => {
      console.log('\n🔄 Received SIGTERM, shutting down gracefully...');
      
      server.close(() => {
        console.log('🔌 HTTP server closed');
      });
      
      await simulationService.shutdown();
      process.exit(0);
    });

  } catch (error) {
    console.error('❌ Failed to start server:', error);
    process.exit(1);
  }
}

// Start the server
startServer();