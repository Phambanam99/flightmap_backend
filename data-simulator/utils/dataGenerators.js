const { v4: uuidv4 } = require('uuid');
const moment = require('moment');
const config = require('../config');

// Utility functions for realistic movement simulation
class MovementSimulator {
  constructor() {
    this.activeFlights = new Map();
    this.activeVessels = new Map();
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
    const airline = config.flight.airlines[this.randomInt(0, config.flight.airlines.length - 1)];
    const number = this.randomInt(100, 9999);
    return `${airline.code}${number}`;
  }

  // Generate realistic hex identifier
  generateHexIdent() {
    return Math.random().toString(16).substr(2, 6).toUpperCase();
  }

  // Generate aircraft registration
  generateRegistration() {
    const prefix = 'VN-A';
    const suffix = Math.random().toString(36).substr(2, 3).toUpperCase();
    return `${prefix}${suffix}`;
  }

  // Generate MMSI (Maritime Mobile Service Identity)
  generateMMSI() {
    // MMSI typically starts with country code (574 for Vietnam)
    return 574000000 + Math.floor(Math.random() * 999999);
  }

  // Create new flight with realistic initial parameters matching FlightTrackingRequestDTO
  createNewFlight() {
    const id = this.randomInt(10000, 99999);
    const aircraftId = this.randomInt(1, 1000);
    
    // Choose random airports for origin and destination
    const departureAirport = config.airports[this.randomInt(0, config.airports.length - 1)];
    const arrivalAirport = config.airports[this.randomInt(0, config.airports.length - 1)];
    
    // Start near departure airport
    const latitude = departureAirport.lat + this.random(-0.1, 0.1);
    const longitude = departureAirport.lon + this.random(-0.1, 0.1);
    
    // Select aircraft type
    const aircraftType = config.flight.aircraftTypes[this.randomInt(0, config.flight.aircraftTypes.length - 1)];
    const airline = config.flight.airlines[this.randomInt(0, config.flight.airlines.length - 1)];
    
    const currentTime = new Date();
    const unixTime = Math.floor(currentTime.getTime() / 1000);
    
    // Format data matching FlightTrackingRequestDTO exactly
    const flight = {
      Id: id,
      AircraftId: aircraftId,
      SecsOfTrack: this.randomInt(1, 3600), // Seconds since track started
      ReceverSourceId: this.randomInt(1, 10),
      Hexident: this.generateHexIdent(),
      Register: this.generateRegistration(),
      Altitude: parseFloat(this.random(config.flight.altitudeRange.min, config.flight.altitudeRange.max).toFixed(0)),
      AltitudeType: 'barometric',
      TargetAlt: parseFloat(this.random(25000, 40000).toFixed(0)),
      Callsign: this.generateCallsign(),
      IsTisb: false,
      Speed: parseFloat(this.random(config.flight.speedRange.min, config.flight.speedRange.max).toFixed(1)),
      SpeedType: 'ground',
      VerticalSpeed: parseFloat(this.random(config.flight.verticalSpeedRange.min, config.flight.verticalSpeedRange.max).toFixed(0)),
      Type: aircraftType.type,
      Manufacture: aircraftType.manufacturer,
      ContructorNumber: `CN${this.randomInt(10000, 99999)}`,
      FromPort: departureAirport.code,
      ToPort: arrivalAirport.code,
      Operator: airline.name,
      OperatorCode: airline.code,
      Squawk: this.randomInt(1000, 7777),
      Distance: parseFloat(this.random(0, 500).toFixed(2)),
      Bearing: parseFloat(this.random(0, 360).toFixed(1)),
      Engines: aircraftType.engines,
      EngineType: 'Jet',
      IsMilitary: false,
      Country: airline.country,
      TransponderType: 'Mode S',
      Year: this.randomInt(2010, 2023),
      UnixTime: unixTime,
      UpdateTime: currentTime.toISOString().split('.')[0], // Format: yyyy-MM-dd'T'HH:mm:ss
      Longitude: parseFloat(longitude.toFixed(6)),
      Latitude: parseFloat(latitude.toFixed(6)),
      Source: 'ADSB',
      Flight: this.generateCallsign(),
      FlightType: 'Commercial',
      LandingUnixTimes: unixTime + this.randomInt(3600, 14400), // 1-4 hours from now
      LandingTimes: new Date((unixTime + this.randomInt(3600, 14400)) * 1000).toISOString().split('.')[0],
      ItemType: 1
    };

    // Store internal state
    this.activeFlights.set(id, {
      ...flight,
      lastUpdate: Date.now(),
      targetHeading: this.random(0, 360),
      currentHeading: this.random(0, 360),
      targetSpeed: flight.Speed,
      targetAltitude: flight.TargetAlt,
      destinationLat: arrivalAirport.lat,
      destinationLon: arrivalAirport.lon
    });

    return flight;
  }

  // Update existing flight with realistic movement
  updateFlight(flightId, timeInterval = 1) {
    const flight = this.activeFlights.get(flightId);
    if (!flight) return null;

    // Calculate new position
    const newPos = this.calculateNewPosition(
      flight.Latitude,
      flight.Longitude,
      flight.Speed,
      flight.currentHeading,
      timeInterval
    );

    // Calculate heading toward destination
    const deltaLon = flight.destinationLon - flight.Longitude;
    const deltaLat = flight.destinationLat - flight.Latitude;
    const desiredHeading = (Math.atan2(deltaLon, deltaLat) * 180 / Math.PI + 360) % 360;

    // Gradually adjust heading toward destination
    flight.currentHeading += (desiredHeading - flight.currentHeading) * 0.05;
    flight.currentHeading = (flight.currentHeading + 360) % 360;

    // Update altitude based on phase of flight
    const distanceToDestination = Math.sqrt(
      Math.pow(flight.destinationLat - flight.Latitude, 2) + 
      Math.pow(flight.destinationLon - flight.Longitude, 2)
    );

    if (distanceToDestination < 0.5) {
      // Descending phase
      flight.Altitude = Math.max(1000, flight.Altitude - this.random(100, 300));
      flight.VerticalSpeed = -this.random(500, 1500);
    } else if (flight.Altitude < flight.TargetAlt) {
      // Climbing phase
      flight.Altitude = Math.min(flight.TargetAlt, flight.Altitude + this.random(100, 300));
      flight.VerticalSpeed = this.random(500, 1500);
    } else {
      // Cruise phase
      flight.VerticalSpeed = this.random(-100, 100);
    }

    // Update position
    flight.Latitude = parseFloat(newPos.latitude.toFixed(6));
    flight.Longitude = parseFloat(newPos.longitude.toFixed(6));
    flight.Altitude = parseFloat(flight.Altitude.toFixed(0));
    flight.Bearing = parseFloat(flight.currentHeading.toFixed(1));
    
    // Update timestamps
    const currentTime = new Date();
    flight.UpdateTime = currentTime.toISOString().split('.')[0];
    flight.UnixTime = Math.floor(currentTime.getTime() / 1000);
    flight.SecsOfTrack += timeInterval;
    flight.lastUpdate = Date.now();

    // Check bounds - remove if out of Vietnam area
    if (flight.Latitude < config.bounds.vietnam.minLatitude || 
        flight.Latitude > config.bounds.vietnam.maxLatitude ||
        flight.Longitude < config.bounds.vietnam.minLongitude || 
        flight.Longitude > config.bounds.vietnam.maxLongitude) {
      this.activeFlights.delete(flightId);
      return null;
    }

    this.activeFlights.set(flightId, flight);
    
    // Return clean copy for publishing (remove internal state fields)
    const { lastUpdate, targetHeading, currentHeading, targetSpeed, targetAltitude, destinationLat, destinationLon, ...cleanFlight } = flight;
    return cleanFlight;
  }

  // Create new vessel with realistic parameters matching ShipTrackingRequest
  createNewVessel() {
    const voyageId = this.randomInt(10000, 99999);
    
    // Choose random port
    const port = config.ports[this.randomInt(0, config.ports.length - 1)];
    const latitude = port.lat + this.random(-0.2, 0.2);
    const longitude = port.lon + this.random(-0.2, 0.2);
    
    const currentTime = new Date();
    
    // Format data matching ShipTrackingRequest exactly
    const vessel = {
      timestamp: currentTime.toISOString().split('.')[0], // LocalDateTime format
      latitude: parseFloat(latitude.toFixed(6)),
      longitude: parseFloat(longitude.toFixed(6)),
      mmsi: this.generateMMSI().toString(), // Add MMSI field
      speed: parseFloat(this.random(config.vessel.speedRange.min, config.vessel.speedRange.max).toFixed(1)),
      course: parseFloat(this.random(0, 360).toFixed(1)),
      draught: parseFloat(this.random(config.vessel.draughtRange.min, config.vessel.draughtRange.max).toFixed(1)),
      voyageId: voyageId
    };

    // Store internal state for realistic movement
    this.activeVessels.set(voyageId, {
      ...vessel,
      lastUpdate: Date.now(),
      targetHeading: vessel.course,
      targetSpeed: vessel.speed,
      vesselType: config.vessel.vesselTypes[this.randomInt(0, config.vessel.vesselTypes.length - 1)],
      destinationPort: config.ports[this.randomInt(0, config.ports.length - 1)]
    });

    return vessel;
  }

  // Update existing vessel with realistic movement
  updateVessel(voyageId, timeInterval = 2) {
    const vessel = this.activeVessels.get(voyageId);
    if (!vessel) return null;

    // Calculate new position
    const newPos = this.calculateNewPosition(
      vessel.latitude,
      vessel.longitude,
      vessel.speed,
      vessel.course,
      timeInterval
    );

    // Vessels change direction less frequently than aircraft
    if (Math.random() < 0.02) { // 2% chance to adjust course
      vessel.targetHeading = this.random(0, 360);
    }
    
    if (Math.random() < 0.01) { // 1% chance to change speed
      vessel.targetSpeed = this.random(config.vessel.speedRange.min, config.vessel.speedRange.max);
    }

    // Gradually adjust to targets (vessels change course very slowly)
    vessel.course += (vessel.targetHeading - vessel.course) * 0.02;
    vessel.course = (vessel.course + 360) % 360;
    vessel.speed += (vessel.targetSpeed - vessel.speed) * 0.01;
    
    // Update position
    vessel.latitude = parseFloat(newPos.latitude.toFixed(6));
    vessel.longitude = parseFloat(newPos.longitude.toFixed(6));
    vessel.speed = parseFloat(vessel.speed.toFixed(1));
    vessel.course = parseFloat(vessel.course.toFixed(1));
    
    // Update timestamp
    vessel.timestamp = new Date().toISOString().split('.')[0];
    vessel.lastUpdate = Date.now();

    // Slight draught variations
    vessel.draught += this.random(-0.1, 0.1);
    vessel.draught = parseFloat(Math.max(config.vessel.draughtRange.min, 
                                        Math.min(config.vessel.draughtRange.max, vessel.draught)).toFixed(1));

    // Check bounds - remove if out of Vietnam coastal area
    if (vessel.latitude < config.bounds.vietnam.minLatitude || 
        vessel.latitude > config.bounds.vietnam.maxLatitude ||
        vessel.longitude < config.bounds.vietnam.minLongitude || 
        vessel.longitude > config.bounds.vietnam.maxLongitude) {
      this.activeVessels.delete(voyageId);
      return null;
    }

    this.activeVessels.set(voyageId, vessel);
    
    // Return clean copy for publishing (only ShipTrackingRequest fields)
    return {
      timestamp: vessel.timestamp,
      latitude: vessel.latitude,
      longitude: vessel.longitude,
      mmsi: vessel.mmsi, // Include MMSI in update
      speed: vessel.speed,
      course: vessel.course,
      draught: vessel.draught,
      voyageId: vessel.voyageId
    };
  }

  // Get all active flights
  getAllActiveFlights() {
    return Array.from(this.activeFlights.keys());
  }

  // Get all active vessels
  getAllActiveVessels() {
    return Array.from(this.activeVessels.keys());
  }

  // Remove old inactive vehicles
  cleanupInactive(maxAge = 300000) { // 5 minutes
    const now = Date.now();
    
    for (const [id, flight] of this.activeFlights.entries()) {
      if (now - flight.lastUpdate > maxAge) {
        this.activeFlights.delete(id);
      }
    }
    
    for (const [id, vessel] of this.activeVessels.entries()) {
      if (now - vessel.lastUpdate > maxAge) {
        this.activeVessels.delete(id);
      }
    }
  }

  // Get statistics
  getStatistics() {
    return {
      activeFlights: this.activeFlights.size,
      activeVessels: this.activeVessels.size,
      flightIds: Array.from(this.activeFlights.keys()),
      vesselIds: Array.from(this.activeVessels.keys())
    };
  }
}

module.exports = MovementSimulator; 