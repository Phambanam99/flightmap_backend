require('dotenv').config();

const config = {
  // Server Configuration
  port: process.env.PORT || 3001,
  
  // Backend API Configuration
  api: {
    baseUrl: process.env.API_BASE_URL || 'http://localhost:9090',
    endpoints: {
      publishFlight: '/api/tracking/publish/flight',
      publishVessel: '/api/tracking/publish/vessel',
      consumerStatus: '/api/tracking/consumer/status'
    },
    timeout: parseInt(process.env.API_TIMEOUT) || 30000,
    retryAttempts: parseInt(process.env.API_RETRY_ATTEMPTS) || 3,
    retryDelay: parseInt(process.env.API_RETRY_DELAY) || 1000
  },
  
  // Simulation Configuration
  simulation: {
    flightInterval: parseInt(process.env.FLIGHT_SIMULATION_INTERVAL) || 1000, // 1 second
    vesselInterval: parseInt(process.env.VESSEL_SIMULATION_INTERVAL) || 2000, // 2 seconds
    maxFlights: parseInt(process.env.MAX_FLIGHTS) || 100,
    maxVessels: parseInt(process.env.MAX_VESSELS) || 50,
    batchSize: parseInt(process.env.BATCH_SIZE) || 10,
    enableFlights: process.env.ENABLE_FLIGHTS !== 'false',
    enableVessels: process.env.ENABLE_VESSELS !== 'false'
  },
  
  // Geographic Bounds (Vietnam Area)
  bounds: {
    vietnam: {
      minLatitude: 8.5,
      maxLatitude: 23.5,
      minLongitude: 102.0,
      maxLongitude: 109.5
    },
    // Major shipping routes
    shippingLanes: [
      { name: 'North-South Route', startLat: 20.5, startLon: 107.5, endLat: 10.5, endLon: 107.0 },
      { name: 'East-West Route', startLat: 16.0, startLon: 108.5, endLat: 16.5, endLon: 103.5 }
    ]
  },
  
  // Major Airports
  airports: [
    { code: 'SGN', name: 'Tan Son Nhat', lat: 10.8187, lon: 106.6524 },
    { code: 'HAN', name: 'Noi Bai', lat: 21.2214, lon: 105.8077 },
    { code: 'DAD', name: 'Da Nang', lat: 16.0439, lon: 108.1993 },
    { code: 'CXR', name: 'Cam Ranh', lat: 11.9982, lon: 109.2194 },
    { code: 'PQC', name: 'Phu Quoc', lat: 10.1625, lon: 103.9931 }
  ],
  
  // Major Ports
  ports: [
    { code: 'VNSGN', name: 'Ho Chi Minh Port', lat: 10.7500, lon: 106.7500 },
    { code: 'VNHPH', name: 'Hai Phong Port', lat: 20.8650, lon: 106.6838 },
    { code: 'VNDNG', name: 'Da Nang Port', lat: 16.0833, lon: 108.2167 },
    { code: 'VNVUT', name: 'Vung Tau Port', lat: 10.3500, lon: 107.0667 },
    { code: 'VNQNI', name: 'Quy Nhon Port', lat: 13.7667, lon: 109.2333 }
  ],
  
  // Flight simulation parameters
  flight: {
    altitudeRange: { min: 1000, max: 42000 },
    speedRange: { min: 150, max: 900 }, // knots
    verticalSpeedRange: { min: -2000, max: 2000 }, // feet per minute
    callsignPrefixes: ['VN', 'VJ', 'BL', 'QH', 'SQ', 'TG', 'CX', 'NH', 'KE', 'OZ'],
    aircraftTypes: [
      { type: 'B737', manufacturer: 'Boeing', engines: '2' },
      { type: 'A320', manufacturer: 'Airbus', engines: '2' },
      { type: 'A321', manufacturer: 'Airbus', engines: '2' },
      { type: 'B777', manufacturer: 'Boeing', engines: '2' },
      { type: 'A350', manufacturer: 'Airbus', engines: '2' },
      { type: 'B787', manufacturer: 'Boeing', engines: '2' },
      { type: 'ATR72', manufacturer: 'ATR', engines: '2' }
    ],
    airlines: [
      { code: 'VN', name: 'Vietnam Airlines', country: 'Vietnam' },
      { code: 'VJ', name: 'VietJet Air', country: 'Vietnam' },
      { code: 'BL', name: 'Bamboo Airways', country: 'Vietnam' },
      { code: 'QH', name: 'Qantas', country: 'Australia' }
    ]
  },
  
  // Vessel simulation parameters  
  vessel: {
    speedRange: { min: 0, max: 25 }, // knots
    draughtRange: { min: 5, max: 20 }, // meters
    vesselTypes: [
      { type: 'Container', sizeRange: { min: 100, max: 400 } },
      { type: 'Bulk Carrier', sizeRange: { min: 150, max: 350 } },
      { type: 'Tanker', sizeRange: { min: 200, max: 400 } },
      { type: 'Fishing', sizeRange: { min: 20, max: 100 } },
      { type: 'Cargo', sizeRange: { min: 100, max: 300 } },
      { type: 'Passenger', sizeRange: { min: 50, max: 350 } }
    ],
    flags: ['Vietnam', 'Singapore', 'Panama', 'Liberia', 'Marshall Islands', 'Hong Kong']
  },
  
  // Mock External APIs Configuration
  mockApis: {
    // Aircraft Sources
    flightradar24: {
      updateInterval: 30000, // 30 seconds
      quality: 0.95,
      priority: 1,
      coverage: 'global',
      responseDelay: { min: 100, max: 500 },
      errorRate: 0.02
    },
    adsbexchange: {
      updateInterval: 30000, // 35 seconds
      quality: 0.88,
      priority: 2,
      coverage: 'community',
      responseDelay: { min: 200, max: 800 },
      errorRate: 0.05
    },
    
    // Vessel Sources
    marinetraffic: {
      updateInterval: 30000, // 60 seconds
      quality: 0.92,
      priority: 1,
      coverage: 'global',
      responseDelay: { min: 150, max: 600 },
      errorRate: 0.03
    },
    vesselfinder: {
      updateInterval: 30000, // 70 seconds
      quality: 0.87,
      priority: 2,
      coverage: 'commercial',
      responseDelay: { min: 300, max: 1000 },
      errorRate: 0.06
    },
    chinaports: {
      updateInterval: 30000, // 90 seconds
      quality: 0.85,
      priority: 3,
      coverage: 'china_sea',
      responseDelay: { min: 500, max: 1200 },
      errorRate: 0.08,
      geoBounds: {
        minLatitude: 3.0,
        maxLatitude: 25.0,
        minLongitude: 99.0,
        maxLongitude: 125.0
      }
    },
    marinetrafficv2: {
      updateInterval: 30000, // 80 seconds
      quality: 0.89,
      priority: 4,
      coverage: 'extended',
      responseDelay: { min: 200, max: 700 },
      errorRate: 0.04
    }
  },

  // Data Source Simulation Settings
  dataSourceSettings: {
    // Different formats for different sources
    flightradar24: {
      format: 'array_indexed',
      includeGroundStatus: true,
      includeEmergency: true,
      altitudeUnit: 'feet'
    },
    adsbexchange: {
      format: 'json_object',
      includeGroundStatus: true,
      includeEmergency: true,
      altitudeUnit: 'feet',
      communityReported: true
    },
    marinetraffic: {
      format: 'json_object',
      includeDestination: true,
      includeDraught: true,
      includeVesselDetails: true
    },
    vesselfinder: {
      format: 'json_object',
      includeDestination: true,
      includeEta: true,
      commercialFocus: true
    },
    chinaports: {
      format: 'xml_converted',
      regionSpecific: true,
      includePortInfo: true,
      language: 'mixed'
    },
    marinetrafficv2: {
      format: 'json_extended',
      includePhotos: false,
      includeReviews: false,
      extendedDetails: true
    }
  },

  // Logging Configuration
  logging: {
    level: process.env.LOG_LEVEL || 'info',
    file: process.env.LOG_FILE || './simulator.log',
    console: process.env.LOG_CONSOLE !== 'false'
  }
};

module.exports = config;
