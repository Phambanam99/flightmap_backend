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
    
    // Generate flights for Vietnam area
    for (let i = 0; i < 300; i++) {
      const flight = this.movementSimulator.createNewFlight();
      const hexident = this.generateHexIdent();
      
      // Store mapping between hexident and internal flight ID
      this.flightIdMap.set(hexident, flight.Id);
      this.flightData.set(hexident, this.convertToFlightRadar24Format(flight, hexident));
    }
    
    // Generate ships for Vietnam coastal area
    for (let i = 0; i < 300; i++) {
      const ship = this.movementSimulator.createNewVessel();
      const mmsi = this.generateMMSI();
      
      // Store mapping between mmsi and internal voyage ID
      this.shipIdMap.set(mmsi, ship.voyageId);
      this.shipData.set(mmsi, this.convertToMarineTrafficFormat(ship, mmsi));
    }
    
    console.log(`✅ Generated ${this.flightData.size} flights and ${this.shipData.size} ships`);
  }

  startDataUpdates() {
    // Debug config
    console.log('🔧 Config check:', {
      flightradar24: config.mockApis?.flightradar24?.updateInterval,
      marinetraffic: config.mockApis?.marinetraffic?.updateInterval,
      configExists: !!config,
      mockApisExists: !!config.mockApis
    });

    // Update flight data every 30 seconds
    setInterval(() => {
      this.updateFlightData();
    }, config.mockApis?.flightradar24?.updateInterval || 30000);

    // Update ship data every 60 seconds
    setInterval(() => {
      this.updateShipData();
    }, config.mockApis?.marinetraffic?.updateInterval || 60000);
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
    while (this.flightData.size < 300) {
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
      const updatedShip = this.movementSimulator.updateVessel(voyageId, 60);
      
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
    while (this.shipData.size < 3000) {
      const newShip = this.movementSimulator.createNewVessel();
      const mmsi = this.generateMMSI();
      this.shipIdMap.set(mmsi, newShip.voyageId);
      this.shipData.set(mmsi, this.convertToMarineTrafficFormat(newShip, mmsi));
    }
    
    this.lastUpdate.ships = Date.now();
    console.log(`🚢 Updated ${this.shipData.size} ship positions (removed ${toRemove.length})`);
  }

  // =================
  // AIRCRAFT API MOCKS
  // =================

  // FlightRadar24 API Mock (Priority 1, Quality: 95%)
  getFlightRadar24Data(bounds) {
    const result = {
      full_count: this.flightData.size,
      version: 4
    };

    this.flightData.forEach((flight, hexident) => {
      // Check if flight is within bounds
      if (this.isWithinBounds({ Latitude: flight[1], Longitude: flight[2] }, bounds)) {
        // Add some data quality variance - FlightRadar24 is very reliable
        if (Math.random() > (config.mockApis?.flightradar24?.errorRate || 0.02)) {
          result[hexident] = this.addDataQualityVariance(flight, 'flightradar24');
        }
      }
    });

    return result;
  }

  // ADS-B Exchange API Mock (Priority 2, Quality: 88%)
  getAdsbExchangeData(bounds) {
    const aircraft = [];
    
    this.flightData.forEach((flight, hexident) => {
      if (this.isWithinBounds({ Latitude: flight[1], Longitude: flight[2] }, bounds)) {
        // ADS-B Exchange has community-driven data with slightly lower quality
        if (Math.random() > (config.mockApis?.adsbexchange?.errorRate || 0.05)) {
          aircraft.push(this.convertToAdsbExchangeFormat(flight, hexident));
        }
      }
    });

    return {
      ac: aircraft,
      total: aircraft.length,
      ctime: Date.now(),
      ptime: Date.now() - 30000
    };
  }

  // =================
  // VESSEL API MOCKS
  // =================

  // MarineTraffic API Mock (Priority 1, Quality: 92%)
  getMarineTrafficData(bounds) {
    const ships = [];
    
    this.shipData.forEach((ship, mmsi) => {
      // Check if ship is within bounds
      if (this.isWithinBounds({ Latitude: ship.LAT, Longitude: ship.LON }, bounds)) {
        if (Math.random() > (config.mockApis?.marinetraffic?.errorRate || 0.03)) {
          ships.push(this.addVesselDataQualityVariance(ship, 'marinetraffic'));
        }
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

  // VesselFinder API Mock (Priority 2, Quality: 87%)
  getVesselFinderData(bounds) {
    const vessels = [];

    this.shipData.forEach((ship, mmsi) => {
      if (this.isWithinBounds({ Latitude: ship.LAT, Longitude: ship.LON }, bounds)) {
        if (Math.random() > (config.mockApis?.vesselfinder?.errorRate || 0.06)) {
          vessels.push(this.convertToVesselFinderFormat(ship, mmsi));
        }
      }
    });

    return {
      vessels: vessels,
      count: vessels.length,
      status: "success",
      timestamp: new Date().toISOString()
    };
  }

  // Chinaports API Mock (Priority 3, Quality: 85%) - China Sea focus
  getChinaportsData(bounds) {
    const ships = [];
    const chinaBounds = config.mockApis?.chinaports?.geoBounds || {
      minLatitude: 3.0,
      maxLatitude: 25.0,
      minLongitude: 99.0,
      maxLongitude: 125.0
    };

    this.shipData.forEach((ship, mmsi) => {
      const shipLat = ship.LAT;
      const shipLon = ship.LON;
      
      // Only return vessels in China Sea region
      if (shipLat >= chinaBounds.minLatitude && shipLat <= chinaBounds.maxLatitude &&
          shipLon >= chinaBounds.minLongitude && shipLon <= chinaBounds.maxLongitude) {
        
        if (bounds && this.isWithinBounds({ Latitude: ship.LAT, Longitude: ship.LON }, bounds)) {
          if (Math.random() > (config.mockApis?.chinaports?.errorRate || 0.08)) {
            ships.push(this.convertToChinaportsFormat(ship, mmsi));
          }
        } else if (!bounds) {
          if (Math.random() > (config.mockApis?.chinaports?.errorRate || 0.08)) {
            ships.push(this.convertToChinaportsFormat(ship, mmsi));
          }
        }
      }
    });

    return {
      code: 200,
      message: "success",
      data: {
        ships: ships,
        total: ships.length,
        region: "China Sea",
        update_time: new Date().toISOString()
      }
    };
  }

  // MarineTraffic V2 API Mock (Priority 4, Quality: 89%)
  getMarineTrafficV2Data(bounds) {
    const positions = [];

    this.shipData.forEach((ship, mmsi) => {
      if (this.isWithinBounds({ Latitude: ship.LAT, Longitude: ship.LON }, bounds)) {
        if (Math.random() > (config.mockApis?.marinetrafficv2?.errorRate || 0.04)) {
          positions.push(this.convertToMarineTrafficV2Format(ship, mmsi));
        }
      }
    });

    return {
      response_code: 200,
      response_text: "OK",
      data: {
        positions: positions,
        meta: {
          total_count: positions.length,
          api_version: "v2",
          request_timestamp: new Date().toISOString()
        }
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
    const prefixes = config.flight?.callsignPrefixes || ['VN', 'VJ', 'BL'];
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
    return config.vessel?.vesselTypes?.[Math.floor(Math.random() * config.vessel.vesselTypes.length)]?.type || 'Container';
  }

  getRandomAircraftType() {
    return config.flight?.aircraftTypes?.[Math.floor(Math.random() * config.flight.aircraftTypes.length)]?.type || 'B737';
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
    const newShip = this.movementSimulator.createNewVessel();
    // Override with custom data if provided
    Object.assign(newShip, shipData);
    
    const mmsi = this.generateMMSI();
    this.shipIdMap.set(mmsi, newShip.voyageId);
    this.shipData.set(mmsi, this.convertToMarineTrafficFormat(newShip, mmsi));
    return mmsi;
  }

  // =================
  // FORMAT CONVERTERS
  // =================

  // ADS-B Exchange format (JSON object format)
  convertToAdsbExchangeFormat(flight, hexident) {
    return {
      hex: hexident,
      flight: flight[16] || flight[13], // callsign
      lat: this.addPositionNoise(flight[1], 0.0001), // Slightly less precise
      lon: this.addPositionNoise(flight[2], 0.0001),
      alt_baro: flight[4],
      alt_geom: flight[4] + Math.floor(Math.random() * 100 - 50),
      gs: this.addSpeedNoise(flight[5], 2),
      track: flight[3],
      baro_rate: flight[15],
      category: "A3", // Heavy aircraft
      nav_qnh: 1013.25,
      nav_altitude_mcp: flight[4],
      nav_modes: ["autopilot", "althold"],
      seen: Math.random() * 10,
      rssi: -20 - Math.random() * 30,
      messages: Math.floor(Math.random() * 1000) + 100,
      seen_pos: Math.random() * 5,
      emergency: flight[6] === "7700" ? "emergency" : "none",
      spi: false,
      alert: false
    };
  }

  // VesselFinder format (Commercial focus)
  convertToVesselFinderFormat(ship, mmsi) {
    return {
      mmsi: mmsi,
      imo: this.generateIMO(),
      name: ship.SHIPNAME,
      lat: this.addPositionNoise(ship.LAT, 0.0005),
      lng: this.addPositionNoise(ship.LON, 0.0005),
      speed: this.addSpeedNoise(ship.SPEED, 0.5),
      course: ship.COURSE,
      heading: ship.HEADING,
      nav_status: ship.STATUS,
      ship_type: ship.SHIPTYPE,
      flag: ship.FLAG,
      length: ship.LENGTH,
      width: ship.WIDTH,
      eta: this.generateETA(),
      destination: this.generateDestination(),
      callsign: this.generateCallsign(),
      draught: ship.DRAUGHT,
      year_built: 1990 + Math.floor(Math.random() * 30),
      gross_tonnage: Math.floor(Math.random() * 50000) + 5000,
      dwt: Math.floor(Math.random() * 100000) + 10000,
      last_port: this.generatePort(),
      next_port: this.generatePort(),
      photos_count: Math.floor(Math.random() * 5),
      last_update: new Date().toISOString()
    };
  }

  // Chinaports format (XML-converted, region-specific)
  convertToChinaportsFormat(ship, mmsi) {
    return {
      vessel_id: mmsi,
      vessel_name_cn: this.generateChineseVesselName(),
      vessel_name_en: ship.SHIPNAME,
      position: {
        latitude: this.addPositionNoise(ship.LAT, 0.001),
        longitude: this.addPositionNoise(ship.LON, 0.001),
        coordinate_system: "WGS84"
      },
      navigation: {
        speed_knots: this.addSpeedNoise(ship.SPEED, 1.0),
        course_degrees: ship.COURSE,
        heading_degrees: ship.HEADING,
        nav_status_code: this.getChineseNavStatus(ship.STATUS)
      },
      vessel_info: {
        ship_type_code: this.getChineseShipTypeCode(ship.SHIPTYPE),
        flag_state: ship.FLAG,
        length_m: ship.LENGTH,
        beam_m: ship.WIDTH,
        draught_m: ship.DRAUGHT,
        grt: Math.floor(Math.random() * 20000) + 1000
      },
      port_info: {
        last_port_code: this.getChinesePortCode(),
        next_port_code: this.getChinesePortCode(),
        eta_local: this.generateETA()
      },
      update_time: new Date().toISOString(),
      data_source: "chinaports",
      reliability: "medium"
    };
  }

  // MarineTraffic V2 format (Extended details)
  convertToMarineTrafficV2Format(ship, mmsi) {
    return {
      MMSI: mmsi,
      IMO: this.generateIMO(),
      SHIPNAME: ship.SHIPNAME,
      LAT: this.addPositionNoise(ship.LAT, 0.0002),
      LON: this.addPositionNoise(ship.LON, 0.0002),
      SPEED: this.addSpeedNoise(ship.SPEED, 0.3),
      COURSE: ship.COURSE,
      HEADING: ship.HEADING,
      STATUS: ship.STATUS,
      SHIPTYPE: ship.SHIPTYPE,
      FLAG: ship.FLAG,
      LENGTH: ship.LENGTH,
      WIDTH: ship.WIDTH,
      DRAUGHT: ship.DRAUGHT,
      CALLSIGN: this.generateVesselCallsign(),
      DESTINATION: this.generateDestination(),
      ETA: this.generateETA(),
      // Extended V2 fields
      AIS_VERSION: "2",
      ROT: Math.floor(Math.random() * 10 - 5), // Rate of turn
      NAV_STATUS_ID: Math.floor(Math.random() * 15),
      TYPE_NAME: this.getExtendedShipTypeName(ship.SHIPTYPE),
      DWT: Math.floor(Math.random() * 100000) + 5000,
      YEAR_BUILT: 1980 + Math.floor(Math.random() * 40),
      GT: Math.floor(Math.random() * 50000) + 1000,
      OWNER: this.generateShipOwner(),
      MANAGER: this.generateShipManager(),
      LAST_PORT: this.generatePort(),
      LAST_PORT_TIME: new Date(Date.now() - Math.random() * 7 * 24 * 60 * 60 * 1000).toISOString(),
      NEXT_PORT: this.generatePort(),
      CURRENT_PORT: null,
      PHOTOS: Math.floor(Math.random() * 3),
      NOTES: "",
      TIMESTAMP: new Date().toISOString()
    };
  }

  // =================
  // DATA QUALITY VARIANCE
  // =================

  addDataQualityVariance(flight, source) {
    const quality = config.mockApis?.[source]?.quality || 0.9;
    
    if (Math.random() > quality) {
      // Add some noise based on source quality
      const noiseFactor = (1 - quality) * 10;
      return [
        flight[0], // hexident
        this.addPositionNoise(flight[1], 0.0001 * noiseFactor), // lat
        this.addPositionNoise(flight[2], 0.0001 * noiseFactor), // lon
        flight[3] + (Math.random() - 0.5) * noiseFactor, // heading
        flight[4] + Math.floor((Math.random() - 0.5) * 500 * noiseFactor), // altitude
        this.addSpeedNoise(flight[5], noiseFactor), // speed
        flight[6], // squawk
        flight[7], // radar type
        flight[8], // aircraft type
        flight[9], // registration
        flight[10], // timestamp
        flight[11], // origin
        flight[12], // destination
        flight[13], // flight number
        flight[14], // unknown
        flight[15] + Math.floor((Math.random() - 0.5) * 200 * noiseFactor), // vertical speed
        flight[16] // callsign
      ];
    }
    
    return flight; // Return original if quality check passes
  }

  addVesselDataQualityVariance(ship, source) {
    const quality = config.mockApis?.[source]?.quality || 0.9;
    
    if (Math.random() > quality) {
      const noiseFactor = (1 - quality) * 5;
      return {
        ...ship,
        LAT: this.addPositionNoise(ship.LAT, 0.001 * noiseFactor),
        LON: this.addPositionNoise(ship.LON, 0.001 * noiseFactor),
        SPEED: this.addSpeedNoise(ship.SPEED, noiseFactor),
        COURSE: (ship.COURSE + (Math.random() - 0.5) * noiseFactor * 10) % 360
      };
    }
    
    return ship;
  }

  addPositionNoise(value, noiseFactor) {
    return value + (Math.random() - 0.5) * noiseFactor;
  }

  addSpeedNoise(value, noiseFactor) {
    return Math.max(0, value + (Math.random() - 0.5) * noiseFactor);
  }

  // =================
  // HELPER GENERATORS
  // =================

  generateIMO() {
    return Math.floor(Math.random() * 9000000) + 1000000;
  }

  generateETA() {
    const now = new Date();
    const eta = new Date(now.getTime() + Math.random() * 7 * 24 * 60 * 60 * 1000);
    return eta.toISOString();
  }

  generateDestination() {
    const destinations = ['HONG KONG', 'SINGAPORE', 'SHANGHAI', 'TOKYO', 'BUSAN', 'KAOHSIUNG', 'MANILA', 'JAKARTA'];
    return destinations[Math.floor(Math.random() * destinations.length)];
  }

  generatePort() {
    const ports = ['VNSGN', 'VNHPH', 'VNDNG', 'VNVUT', 'VNQNI', 'HKHKG', 'SGSIN', 'CNSHA'];
    return ports[Math.floor(Math.random() * ports.length)];
  }

  generateVesselCallsign() {
    const prefixes = ['3F', '9V', '3E', 'HL', 'XV'];
    return prefixes[Math.floor(Math.random() * prefixes.length)] + Math.floor(Math.random() * 9999);
  }

  generateChineseVesselName() {
    const names = ['海港之星', '东方明珠', '南海风帆', '长江货运', '珠江快航', '渤海之光'];
    return names[Math.floor(Math.random() * names.length)];
  }

  getChineseNavStatus(status) {
    const statusMap = {
      'Under way using engine': 'UWE',
      'At anchor': 'ANC',
      'Not under command': 'NUC',
      'Restricted maneuverability': 'RMA',
      'Moored': 'MOR'
    };
    return statusMap[status] || 'UWE';
  }

  getChineseShipTypeCode(shipType) {
    const typeMap = {
      'Container': 'CON',
      'Bulk Carrier': 'BUL',
      'Tanker': 'TAN',
      'Fishing': 'FIS',
      'Cargo': 'CAR',
      'Passenger': 'PAS'
    };
    return typeMap[shipType] || 'GEN';
  }

  getChinesePortCode() {
    const ports = ['CNSHA', 'CNQIN', 'CNNGB', 'CNTIA', 'CNDALG', 'CNXIA', 'CNYTN'];
    return ports[Math.floor(Math.random() * ports.length)];
  }

  getExtendedShipTypeName(shipType) {
    const extendedNames = {
      'Container': 'Container Ship (Cellular)',
      'Bulk Carrier': 'Bulk Carrier (Dry Cargo)',
      'Tanker': 'Chemical/Oil Tanker',
      'Fishing': 'Fishing Vessel',
      'Cargo': 'General Cargo Ship',
      'Passenger': 'Passenger Vessel'
    };
    return extendedNames[shipType] || shipType;
  }

  generateShipOwner() {
    const owners = ['COSCO Shipping', 'OOCL', 'Evergreen Marine', 'Yang Ming', 'MOL', 'NYK Line'];
    return owners[Math.floor(Math.random() * owners.length)];
  }

  generateShipManager() {
    const managers = ['Fleet Management Ltd', 'Marine Services Co', 'Ship Management Inc', 'Ocean Logistics'];
    return managers[Math.floor(Math.random() * managers.length)];
  }

  // =================
  // MULTI-SOURCE API
  // =================

  // Get all available aircraft data from all sources
  getAllAircraftSources(bounds) {
    return {
      flightradar24: this.getFlightRadar24Data(bounds),
      adsbexchange: this.getAdsbExchangeData(bounds)
    };
  }

  // Get all available vessel data from all sources
  getAllVesselSources(bounds) {
    return {
      marinetraffic: this.getMarineTrafficData(bounds),
      vesselfinder: this.getVesselFinderData(bounds),
      chinaports: this.getChinaportsData(bounds),
      marinetrafficv2: this.getMarineTrafficV2Data(bounds)
    };
  }

  // Get statistics with all sources
  getStats() {
    return {
      flights: {
        total: this.flightData.size,
        lastUpdate: new Date(this.lastUpdate.flights).toISOString(),
        sources: {
          flightradar24: {
            quality: config.mockApis?.flightradar24?.quality || 0.95,
            priority: config.mockApis?.flightradar24?.priority || 1,
            updateInterval: config.mockApis?.flightradar24?.updateInterval || 30000
          },
          adsbexchange: {
            quality: config.mockApis?.adsbexchange?.quality || 0.88,
            priority: config.mockApis?.adsbexchange?.priority || 2,
            updateInterval: config.mockApis?.adsbexchange?.updateInterval || 35000
          }
        }
      },
      ships: {
        total: this.shipData.size,
        lastUpdate: new Date(this.lastUpdate.ships).toISOString(),
        sources: {
          marinetraffic: {
            quality: config.mockApis?.marinetraffic?.quality || 0.92,
            priority: config.mockApis?.marinetraffic?.priority || 1,
            updateInterval: config.mockApis?.marinetraffic?.updateInterval || 60000
          },
          vesselfinder: {
            quality: config.mockApis?.vesselfinder?.quality || 0.87,
            priority: config.mockApis?.vesselfinder?.priority || 2,
            updateInterval: config.mockApis?.vesselfinder?.updateInterval || 70000
          },
          chinaports: {
            quality: config.mockApis?.chinaports?.quality || 0.85,
            priority: config.mockApis?.chinaports?.priority || 3,
            updateInterval: config.mockApis?.chinaports?.updateInterval || 90000
          },
          marinetrafficv2: {
            quality: config.mockApis?.marinetrafficv2?.quality || 0.89,
            priority: config.mockApis?.marinetrafficv2?.priority || 4,
            updateInterval: config.mockApis?.marinetrafficv2?.updateInterval || 80000
          }
        }
      },
      movementSimulator: {
        activeFlights: this.flightData.size,
        activeShips: this.shipData.size
      }
    };
  }
}

module.exports = MockApiService; 