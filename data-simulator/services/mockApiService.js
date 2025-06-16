const config = require('../config');
const MovementSimulator = require('../utils/dataGenerators');

class MockApiService {
  constructor() {
    this.movementSimulator = new MovementSimulator();
    this.flightData = new Map(); // hexident -> flightRadar24 format
    this.shipData = new Map();   // mmsi -> marineTraffic format
    this.flightIdMap = new Map(); // hexident -> internal flight ID
    this.shipIdMap = new Map();   // mmsi -> internal voyage ID
    this.lastUpdate = {
      flights: Date.now(),
      ships: Date.now()
    };
    
    // Generate initial data
    this.generateInitialData();
    
    // Start update intervals
    this.startDataUpdates();
  }

  generateInitialData() {
    console.log('🎭 Generating initial mock data...');
    
    // Generate 50 flights for Vietnam area
    for (let i = 0; i < 50000; i++) {
      const flight = this.movementSimulator.createNewFlight();
      const hexident = this.generateHexIdent();
      
      // Store mapping between hexident and internal flight ID
      this.flightIdMap.set(hexident, flight.Id);
      this.flightData.set(hexident, this.convertToFlightRadar24Format(flight, hexident));
    }
    
    // Generate 30 ships for Vietnam coastal area
    for (let i = 0; i < 30000; i++) {
      const ship = this.movementSimulator.createNewShip();
      const mmsi = this.generateMMSI();
      
      // Store mapping between mmsi and internal voyage ID
      this.shipIdMap.set(mmsi, ship.voyageId);
      this.shipData.set(mmsi, this.convertToMarineTrafficFormat(ship, mmsi));
    }
    
    console.log(`✅ Generated ${this.flightData.size} flights and ${this.shipData.size} ships`);
  }

  startDataUpdates() {
    // Update flight data every 30 seconds
    setInterval(() => {
      this.updateFlightData();
    }, config.mockApis.flightRadar24.updateInterval);

    // Update ship data every 60 seconds
    setInterval(() => {
      this.updateShipData();
    }, config.mockApis.marineTraffic.updateInterval);
  }

  updateFlightData() {
    const updatedCount = 0;
    const toRemove = [];
    
    this.flightIdMap.forEach((flightId, hexident) => {
      // Update flight using MovementSimulator
      const updatedFlight = this.movementSimulator.updateFlight(flightId, 30);
      
      if (updatedFlight) {
        // Update the display data with new position
        this.flightData.set(hexident, this.convertToFlightRadar24Format(updatedFlight, hexident));
      } else {
        // Flight was removed (out of bounds), clean up
        toRemove.push(hexident);
      }
    });
    
    // Remove flights that went out of bounds
    toRemove.forEach(hexident => {
      this.flightData.delete(hexident);
      this.flightIdMap.delete(hexident);
    });
    
    // Add new flights to replace removed ones
    while (this.flightData.size < 50000) {
      const newFlight = this.movementSimulator.createNewFlight();
      const hexident = this.generateHexIdent();
      this.flightIdMap.set(hexident, newFlight.Id);
      this.flightData.set(hexident, this.convertToFlightRadar24Format(newFlight, hexident));
    }
    
    this.lastUpdate.flights = Date.now();
    console.log(`🛫 Updated ${this.flightData.size} flight positions (removed ${toRemove.length})`);
  }

  updateShipData() {
    const toRemove = [];
    
    this.shipIdMap.forEach((voyageId, mmsi) => {
      // Update ship using MovementSimulator
      const updatedShip = this.movementSimulator.updateShip(voyageId, 60);
      
      if (updatedShip) {
        // Update the display data with new position
        this.shipData.set(mmsi, this.convertToMarineTrafficFormat(updatedShip, mmsi));
      } else {
        // Ship was removed (out of bounds), clean up
        toRemove.push(mmsi);
      }
    });
    
    // Remove ships that went out of bounds
    toRemove.forEach(mmsi => {
      this.shipData.delete(mmsi);
      this.shipIdMap.delete(mmsi);
    });
    
    // Add new ships to replace removed ones
    while (this.shipData.size < 30000) {
      const newShip = this.movementSimulator.createNewShip();
      const mmsi = this.generateMMSI();
      this.shipIdMap.set(mmsi, newShip.voyageId);
      this.shipData.set(mmsi, this.convertToMarineTrafficFormat(newShip, mmsi));
    }
    
    this.lastUpdate.ships = Date.now();
    console.log(`🚢 Updated ${this.shipData.size} ship positions (removed ${toRemove.length})`);
  }

  // FlightRadar24 API Mock
  getFlightRadar24Data(bounds) {
    const result = {
      full_count: this.flightData.size,
      version: 4
    };

    this.flightData.forEach((flight, hexident) => {
      // Check if flight is within bounds
      if (this.isWithinBounds({ Latitude: flight[1], Longitude: flight[2] }, bounds)) {
        result[hexident] = flight;
      }
    });

    return result;
  }

  // MarineTraffic API Mock
  getMarineTrafficData(bounds) {
    const ships = [];
    
    this.shipData.forEach((ship, mmsi) => {
      // Check if ship is within bounds
      if (this.isWithinBounds({ Latitude: ship.LAT, Longitude: ship.LON }, bounds)) {
        ships.push(ship);
      }
    });

    return {
      data: ships,
      meta: {
        total: ships.length,
        last_update: new Date(this.lastUpdate.ships).toISOString()
      }
    };
  }

  convertToFlightRadar24Format(flight, hexident) {
    // FlightRadar24 format: array with specific indices
    return [
      hexident,                    // 0: hexident
      flight.Latitude,             // 1: latitude
      flight.Longitude,            // 2: longitude
      flight.Heading,              // 3: heading
      flight.Altitude,             // 4: altitude
      flight.Speed,                // 5: speed
      flight.Squawk || "2000",     // 6: squawk
      "T-MLAT",                    // 7: radar type
      flight.Type || this.getRandomAircraftType(), // 8: aircraft type
      this.generateRegistration(), // 9: registration
      Math.floor(Date.now() / 1000), // 10: timestamp
      "SGN",                       // 11: origin airport
      "HAN",                       // 12: destination airport
      flight.Callsign || this.generateCallsign(), // 13: flight number
      0,                           // 14: unknown
      flight.VerticalSpeed || 0,   // 15: vertical speed
      flight.Callsign || this.generateCallsign() // 16: callsign
    ];
  }

  convertToMarineTrafficFormat(ship, mmsi) {
    return {
      MMSI: mmsi,
      LAT: ship.latitude,
      LON: ship.longitude,
      SPEED: ship.speed,
      COURSE: ship.course,
      HEADING: ship.course,
      STATUS: "Under way using engine",
      DRAUGHT: ship.draught || (5 + Math.random() * 15),
      SHIPNAME: this.generateShipName(),
      SHIPTYPE: this.getRandomShipType(),
      FLAG: "VN",
      LENGTH: Math.floor(Math.random() * 200) + 50,
      WIDTH: Math.floor(Math.random() * 30) + 10,
      TIMESTAMP: new Date().toISOString()
    };
  }

  isWithinBounds(vehicle, bounds) {
    if (!bounds) return true;
    
    const lat = vehicle.Latitude || vehicle.LAT || vehicle.latitude;
    const lon = vehicle.Longitude || vehicle.LON || vehicle.longitude;
    
    return lat >= bounds.minLat && 
           lat <= bounds.maxLat && 
           lon >= bounds.minLon && 
           lon <= bounds.maxLon;
  }

  generateHexIdent() {
    const chars = '0123456789ABCDEF';
    let result = '';
    for (let i = 0; i < 6; i++) {
      result += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    // Make sure it's unique
    while (this.flightData.has(result)) {
      result = '';
      for (let i = 0; i < 6; i++) {
        result += chars.charAt(Math.floor(Math.random() * chars.length));
      }
    }
    return result;
  }

  generateMMSI() {
    // MMSI typically starts with country code (574 for Vietnam)
    let mmsi = 574000000 + Math.floor(Math.random() * 999999);
    // Make sure it's unique
    while (this.shipData.has(mmsi)) {
      mmsi = 574000000 + Math.floor(Math.random() * 999999);
    }
    return mmsi;
  }

  generateRegistration() {
    return 'VN-' + Math.random().toString(36).substr(2, 3).toUpperCase();
  }

  generateCallsign() {
    const prefixes = config.flight.callsignPrefixes;
    const prefix = prefixes[Math.floor(Math.random() * prefixes.length)];
    const number = Math.floor(Math.random() * 999) + 1;
    return prefix + number;
  }

  generateShipName() {
    const prefixes = ['HAI PHONG', 'SAIGON', 'DA NANG', 'QUY NHON', 'VUNG TAU'];
    const suffixes = ['STAR', 'OCEAN', 'WIND', 'WAVE', 'GLORY', 'PRIDE'];
    return prefixes[Math.floor(Math.random() * prefixes.length)] + ' ' + 
           suffixes[Math.floor(Math.random() * suffixes.length)];
  }

  getRandomShipType() {
    return config.ship.shipTypes[Math.floor(Math.random() * config.ship.shipTypes.length)];
  }

  getRandomAircraftType() {
    return config.flight.aircraftTypes[Math.floor(Math.random() * config.flight.aircraftTypes.length)];
  }

  // Add new flight manually
  addFlight(flightData) {
    const newFlight = this.movementSimulator.createNewFlight();
    // Override with custom data if provided
    Object.assign(newFlight, flightData);
    
    const hexident = this.generateHexIdent();
    this.flightIdMap.set(hexident, newFlight.Id);
    this.flightData.set(hexident, this.convertToFlightRadar24Format(newFlight, hexident));
    return hexident;
  }

  // Add new ship manually
  addShip(shipData) {
    const newShip = this.movementSimulator.createNewShip();
    // Override with custom data if provided
    Object.assign(newShip, shipData);
    
    const mmsi = this.generateMMSI();
    this.shipIdMap.set(mmsi, newShip.voyageId);
    this.shipData.set(mmsi, this.convertToMarineTrafficFormat(newShip, mmsi));
    return mmsi;
  }

  // Get statistics
  getStats() {
    return {
      flights: {
        total: this.flightData.size,
        lastUpdate: new Date(this.lastUpdate.flights).toISOString()
      },
      ships: {
        total: this.shipData.size,
        lastUpdate: new Date(this.lastUpdate.ships).toISOString()
      },
      movementSimulator: {
        activeFlights: this.movementSimulator.getAllActiveFlights().length,
        activeShips: this.movementSimulator.getAllActiveShips().length
      }
    };
  }
}

module.exports = MockApiService; 