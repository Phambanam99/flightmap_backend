require('dotenv').config();

const config = {
  // Server Configuration
  port: process.env.PORT || 3001,
  
  // Kafka Configuration
  kafka: {
    brokers: (process.env.KAFKA_BROKERS || 'localhost:29092').split(','),
    clientId: process.env.KAFKA_CLIENT_ID || 'tracking-data-simulator',
    topics: {
      flight: process.env.KAFKA_FLIGHT_TOPIC || 'flight-tracking',
      ship: process.env.KAFKA_SHIP_TOPIC || 'ship-tracking'
    }
  },
  
  // Simulation Configuration
  simulation: {
    interval: parseInt(process.env.SIMULATION_INTERVAL) || 2000, // 2 seconds
    maxFlights: parseInt(process.env.MAX_FLIGHTS) || 50000,
    maxShips: parseInt(process.env.MAX_SHIPS) || 50000
  },
  
  // Geographic Bounds (Vietnam Area)
  bounds: {
    minLatitude: parseFloat(process.env.MIN_LATITUDE) || 8.5,
    maxLatitude: parseFloat(process.env.MAX_LATITUDE) || 23.5,
    minLongitude: parseFloat(process.env.MIN_LONGITUDE) || 102.0,
    maxLongitude: parseFloat(process.env.MAX_LONGITUDE) || 109.5
  },
  
  // Major Cities and Airports
  locations: {
    hcmc: {
      lat: parseFloat(process.env.HCMC_LAT) || 10.823,
      lon: parseFloat(process.env.HCMC_LON) || 106.629
    },
    hanoi: {
      lat: parseFloat(process.env.HANOI_LAT) || 21.0285,
      lon: parseFloat(process.env.HANOI_LON) || 105.8542
    },
    tanSonNhat: {
      lat: parseFloat(process.env.TAN_SON_NHAT_LAT) || 10.8187,
      lon: parseFloat(process.env.TAN_SON_NHAT_LON) || 106.6524
    },
    noiBai: {
      lat: parseFloat(process.env.NOI_BAI_LAT) || 21.2214,
      lon: parseFloat(process.env.NOI_BAI_LON) || 105.8077
    }
  },
  
  // Flight simulation parameters
  flight: {
    altitudeRange: { min: 1000, max: 42000 },
    speedRange: { min: 150, max: 900 }, // knots
    headingRange: { min: 0, max: 360 },
    callsignPrefixes: ['VN', 'VJ', 'BL', 'QH', 'SQ', 'TG', 'CX', 'NH'],
    aircraftTypes: ['B737', 'A320', 'A321', 'B777', 'A350', 'B787', 'ATR72']
  },
  
  // Ship simulation parameters  
  ship: {
    speedRange: { min: 0, max: 25 }, // knots
    headingRange: { min: 0, max: 360 },
    shipTypes: ['Container', 'Bulk Carrier', 'Tanker', 'Fishing', 'Cargo', 'Passenger']
  }
};

module.exports = config; 