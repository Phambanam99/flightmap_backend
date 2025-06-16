const { v4: uuidv4 } = require('uuid');
const moment = require('moment');
const config = require('../config');

// Utility functions for realistic movement simulation
class MovementSimulator {
  constructor() {
    this.activeFlights = new Map();
    this.activeShips = new Map();
  }

  // Generate random number within range
  random(min, max) {
    return Math.random() * (max - min) + min;
  }

  // Generate random integer within range
  randomInt(min, max) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
  }

  // Calculate new position based on speed, heading, and time
  calculateNewPosition(lat, lon, speed, heading, timeIntervalSeconds) {
    // Convert speed from knots to meters per second
    const speedMs = speed * 0.514444;
    
    // Distance traveled in meters
    const distance = speedMs * timeIntervalSeconds;
    
    // Convert heading to radians
    const headingRad = (heading * Math.PI) / 180;
    
    // Earth's radius in meters
    const earthRadius = 6371000;
    
    // Calculate new position
    const latRad = (lat * Math.PI) / 180;
    const lonRad = (lon * Math.PI) / 180;
    
    const newLatRad = Math.asin(
      Math.sin(latRad) * Math.cos(distance / earthRadius) +
      Math.cos(latRad) * Math.sin(distance / earthRadius) * Math.cos(headingRad)
    );
    
    const newLonRad = lonRad + Math.atan2(
      Math.sin(headingRad) * Math.sin(distance / earthRadius) * Math.cos(latRad),
      Math.cos(distance / earthRadius) - Math.sin(latRad) * Math.sin(newLatRad)
    );
    
    return {
      latitude: (newLatRad * 180) / Math.PI,
      longitude: (newLonRad * 180) / Math.PI
    };
  }

  // Generate realistic callsign
  generateCallsign() {
    const prefix = config.flight.callsignPrefixes[
      this.randomInt(0, config.flight.callsignPrefixes.length - 1)
    ];
    const number = this.randomInt(100, 9999);
    return `${prefix}${number}`;
  }

  // Generate realistic hex identifier
  generateHexIdent() {
    return Math.random().toString(16).substr(2, 6).toUpperCase();
  }

  // Create new flight with realistic initial parameters
  createNewFlight() {
    const id = this.randomInt(10000, 99999);
    const aircraftId = this.randomInt(1, 1000);
    
    // Choose random start position (often near airports)
    const startPositions = [
      config.locations.tanSonNhat,
      config.locations.noiBai,
      config.locations.hcmc,
      config.locations.hanoi
    ];
    
    const startPos = startPositions[this.randomInt(0, startPositions.length - 1)];
    
    // Add some randomness to airport positions
    const latitude = startPos.lat + this.random(-0.1, 0.1);
    const longitude = startPos.lon + this.random(-0.1, 0.1);
    
    const flight = {
      Id: id,
      AircraftId: aircraftId,
      Hexident: this.generateHexIdent(),
      Callsign: this.generateCallsign(),
      Latitude: latitude,
      Longitude: longitude,
      Altitude: this.random(config.flight.altitudeRange.min, config.flight.altitudeRange.max),
      Speed: this.random(config.flight.speedRange.min, config.flight.speedRange.max),
      Heading: this.random(0, 360),
      VerticalSpeed: this.random(-2000, 2000),
      Type: config.flight.aircraftTypes[this.randomInt(0, config.flight.aircraftTypes.length - 1)],
      UpdateTime: moment().format('YYYY-MM-DDTHH:mm:ss'),
      UnixTime: moment().unix(),
      Distance: this.random(0, 500),
      Bearing: this.random(0, 360),
      Squawk: this.randomInt(1000, 7777),
      AltitudeType: 'barometric',
      SpeedType: 'ground',
      TargetAlt: null,
      IsTisb: false,
      IsMilitary: false,
      Country: 'VN',
      TransponderType: 'Mode S',
      Source: 'ADSB',
      ItemType: 1
    };

    this.activeFlights.set(id, {
      ...flight,
      lastUpdate: Date.now(),
      targetHeading: flight.Heading,
      targetSpeed: flight.Speed,
      targetAltitude: flight.Altitude
    });

    return flight;
  }

  // Update existing flight with realistic movement
  updateFlight(flightId, timeInterval = 2) {
    const flight = this.activeFlights.get(flightId);
    if (!flight) return null;

    // Calculate new position
    const newPos = this.calculateNewPosition(
      flight.Latitude,
      flight.Longitude,
      flight.Speed,
      flight.Heading,
      timeInterval
    );

    // Randomly adjust parameters for realism
    if (Math.random() < 0.1) { // 10% chance to change heading
      flight.targetHeading = this.random(0, 360);
    }
    
    if (Math.random() < 0.05) { // 5% chance to change speed
      flight.targetSpeed = this.random(config.flight.speedRange.min, config.flight.speedRange.max);
    }
    
    if (Math.random() < 0.03) { // 3% chance to change altitude
      flight.targetAltitude = this.random(config.flight.altitudeRange.min, config.flight.altitudeRange.max);
    }

    // Gradually adjust to targets (realistic aircraft behavior)
    flight.Heading += (flight.targetHeading - flight.Heading) * 0.1;
    flight.Speed += (flight.targetSpeed - flight.Speed) * 0.05;
    flight.Altitude += (flight.targetAltitude - flight.Altitude) * 0.02;
    
    // Update position
    flight.Latitude = newPos.latitude;
    flight.Longitude = newPos.longitude;
    
    // Update time
    flight.UpdateTime = moment().format('YYYY-MM-DDTHH:mm:ss');
    flight.UnixTime = moment().unix();
    flight.lastUpdate = Date.now();

    // Check bounds - remove if out of Vietnam area
    if (flight.Latitude < config.bounds.minLatitude || 
        flight.Latitude > config.bounds.maxLatitude ||
        flight.Longitude < config.bounds.minLongitude || 
        flight.Longitude > config.bounds.maxLongitude) {
      this.activeFlights.delete(flightId);
      return null;
    }

    this.activeFlights.set(flightId, flight);
    
    // Return clean copy for Kafka
    const { lastUpdate, targetHeading, targetSpeed, targetAltitude, ...cleanFlight } = flight;
    return cleanFlight;
  }

  // Create new ship with realistic parameters
  createNewShip() {
    const voyageId = this.randomInt(10000, 99999);
    
    // Ships often near coastlines or ports
    const coastalPositions = [
      { lat: 10.8, lon: 107.1 }, // Ho Chi Minh City port
      { lat: 20.9, lon: 106.9 }, // Hai Phong port
      { lat: 12.2, lon: 109.2 }, // Quy Nhon port
      { lat: 16.1, lon: 108.2 }  // Da Nang port
    ];
    
    const startPos = coastalPositions[this.randomInt(0, coastalPositions.length - 1)];
    const latitude = startPos.lat + this.random(-0.2, 0.2);
    const longitude = startPos.lon + this.random(-0.2, 0.2);
    
    const ship = {
      voyageId: voyageId,
      timestamp: moment().format('YYYY-MM-DDTHH:mm:ss'),
      latitude: latitude,
      longitude: longitude,
      speed: this.random(config.ship.speedRange.min, config.ship.speedRange.max),
      course: this.random(0, 360),
      draught: this.random(5, 15) // meters
    };

    this.activeShips.set(voyageId, {
      ...ship,
      lastUpdate: Date.now(),
      targetCourse: ship.course,
      targetSpeed: ship.speed
    });

    return ship;
  }

  // Update existing ship with realistic movement
  updateShip(voyageId, timeInterval = 2) {
    const ship = this.activeShips.get(voyageId);
    if (!ship) return null;

    // Calculate new position
    const newPos = this.calculateNewPosition(
      ship.latitude,
      ship.longitude,
      ship.speed,
      ship.course,
      timeInterval
    );

    // Ships change direction less frequently than aircraft
    if (Math.random() < 0.05) { // 5% chance to change course
      ship.targetCourse = this.random(0, 360);
    }
    
    if (Math.random() < 0.03) { // 3% chance to change speed
      ship.targetSpeed = this.random(config.ship.speedRange.min, config.ship.speedRange.max);
    }

    // Gradually adjust to targets
    ship.course += (ship.targetCourse - ship.course) * 0.05;
    ship.speed += (ship.targetSpeed - ship.speed) * 0.03;
    
    // Update position
    ship.latitude = newPos.latitude;
    ship.longitude = newPos.longitude;
    
    // Update time
    ship.timestamp = moment().format('YYYY-MM-DDTHH:mm:ss');
    ship.lastUpdate = Date.now();

    // Check bounds
    if (ship.latitude < config.bounds.minLatitude || 
        ship.latitude > config.bounds.maxLatitude ||
        ship.longitude < config.bounds.minLongitude || 
        ship.longitude > config.bounds.maxLongitude) {
      this.activeShips.delete(voyageId);
      return null;
    }

    this.activeShips.set(voyageId, ship);
    
    // Return clean copy for Kafka
    const { lastUpdate, targetCourse, targetSpeed, ...cleanShip } = ship;
    return cleanShip;
  }

  // Get all active flights
  getAllActiveFlights() {
    return Array.from(this.activeFlights.keys());
  }

  // Get all active ships
  getAllActiveShips() {
    return Array.from(this.activeShips.keys());
  }

  // Remove old inactive vehicles
  cleanupInactive(maxAge = 300000) { // 5 minutes
    const now = Date.now();
    
    for (const [id, flight] of this.activeFlights.entries()) {
      if (now - flight.lastUpdate > maxAge) {
        this.activeFlights.delete(id);
      }
    }
    
    for (const [id, ship] of this.activeShips.entries()) {
      if (now - ship.lastUpdate > maxAge) {
        this.activeShips.delete(id);
      }
    }
  }
}

module.exports = MovementSimulator; 