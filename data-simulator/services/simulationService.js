const MovementSimulator = require('../utils/dataGenerators');

const BackendIntegrationService = require('./backendIntegrationService');
const config = require('../config');

class SimulationService {
  constructor() {
    this.movementSimulator = new MovementSimulator();

    this.backendIntegrationService = new BackendIntegrationService();
    this.isRunning = false;
    this.publishMode = 'KAFKA'; // 'KAFKA', 'BACKEND', or 'BOTH'
    this.intervals = {
      flight: null,
      ship: null,
      cleanup: null,
      healthCheck: null
    };
    this.stats = {
      flightsGenerated: 0,
      shipsGenerated: 0,
      flightsPublished: 0,
      shipsPublished: 0,
      errors: 0,
      startTime: null,
      backendHealth: 'UNKNOWN'
    };
  }

  async initialize() {
    try {
      console.log('🚀 Initializing Simulation Service...');
      await //this.kafkaService.connect();
      console.log('✅ Simulation Service initialized successfully');
    } catch (error) {
      console.error('❌ Failed to initialize Simulation Service:', error);
      throw error;
    }
  }

  async startSimulation(options = {}) {
    if (this.isRunning) {
      throw new Error('Simulation is already running');
    }

    try {
      console.log('🎬 Starting real-time simulation...');
      
      // Reset stats
      this.stats = {
        flightsGenerated: 0,
        shipsGenerated: 0,
        flightsPublished: 0,
        shipsPublished: 0,
        errors: 0,
        startTime: new Date()
      };

      const simulationOptions = {
        flightInterval: options.flightInterval || config.simulation.interval,
        shipInterval: options.shipInterval || config.simulation.interval * 1.5,
        maxFlights: options.maxFlights || config.simulation.maxFlights,
        maxShips: options.maxShips || config.simulation.maxShips,
        batchMode: options.batchMode || false,
        publishMode: options.publishMode || this.publishMode
      };
      
      // Set publish mode
      this.publishMode = simulationOptions.publishMode;

      this.isRunning = true;

      // Start flight simulation
      this.intervals.flight = setInterval(async () => {
        await this.generateAndPublishFlights(simulationOptions);
      }, simulationOptions.flightInterval);

      // Start ship simulation  
      this.intervals.ship = setInterval(async () => {
        await this.generateAndPublishShips(simulationOptions);
      }, simulationOptions.shipInterval);

      // Cleanup inactive vehicles every 30 seconds
      this.intervals.cleanup = setInterval(() => {
        this.movementSimulator.cleanupInactive();
      }, 30000);

      // Check backend health every 60 seconds if using backend integration
      if (this.publishMode === 'BACKEND' || this.publishMode === 'BOTH') {
        this.intervals.healthCheck = setInterval(async () => {
          try {
            const health = await this.backendIntegrationService.checkBackendHealth();
            this.stats.backendHealth = health.healthy ? 'HEALTHY' : 'UNHEALTHY';
          } catch (error) {
            this.stats.backendHealth = 'ERROR';
          }
        }, 60000);
        
        // Initial health check
        setTimeout(async () => {
          try {
            const health = await this.backendIntegrationService.checkBackendHealth();
            this.stats.backendHealth = health.healthy ? 'HEALTHY' : 'UNHEALTHY';
          } catch (error) {
            this.stats.backendHealth = 'ERROR';
          }
        }, 1000);
      }

      console.log(`✅ Simulation started with options:`, simulationOptions);
      return this.getStatus();

    } catch (error) {
      console.error('❌ Failed to start simulation:', error);
      this.isRunning = false;
      throw error;
    }
  }

  async generateAndPublishFlights(options) {
    try {
      const activeFlights = this.movementSimulator.getAllActiveFlights();
      const currentFlightCount = activeFlights.length;

      // Create new flights if below max limit
      if (currentFlightCount < options.maxFlights) {
        const newFlightsNeeded = Math.min(
          options.maxFlights - currentFlightCount,
          Math.floor(Math.random() * 3) + 1 // Add 1-3 new flights randomly
        );

        for (let i = 0; i < newFlightsNeeded; i++) {
          const newFlight = this.movementSimulator.createNewFlight();
          this.stats.flightsGenerated++;
          
          if (options.batchMode) {
            // Store for batch processing
            this.pendingFlights = this.pendingFlights || [];
            this.pendingFlights.push(newFlight);
          } else {
            await this.publishFlightData(newFlight, options.publishMode);
            this.stats.flightsPublished++;
          }
        }
      }

      // Update existing flights
      const updatedFlights = [];
      for (const flightId of activeFlights) {
        const updatedFlight = this.movementSimulator.updateFlight(
          flightId, 
          options.flightInterval / 1000
        );
        
        if (updatedFlight) {
          if (options.batchMode) {
            updatedFlights.push(updatedFlight);
          } else {
            await this.publishFlightData(updatedFlight, options.publishMode);
            this.stats.flightsPublished++;
          }
        }
      }

      // Handle batch publishing
      if (options.batchMode && updatedFlights.length > 0) {
        await //this.kafkaService.publishBatchFlightData(updatedFlights);
        this.stats.flightsPublished += updatedFlights.length;
      }

      // Handle pending new flights in batch mode
      if (options.batchMode && this.pendingFlights && this.pendingFlights.length > 0) {
        await //this.kafkaService.publishBatchFlightData(this.pendingFlights);
        this.stats.flightsPublished += this.pendingFlights.length;
        this.pendingFlights = [];
      }

    } catch (error) {
      console.error('❌ Error generating/publishing flights:', error);
      this.stats.errors++;
    }
  }

  // Unified method to publish flight data based on mode
  async publishFlightData(flightData, publishMode = 'KAFKA') {
    try {
      switch (publishMode) {
        case 'KAFKA':
          //await //this.kafkaService.publishFlightData(flightData);
          break;
        case 'BACKEND':
          await this.backendIntegrationService.publishFlightTracking(flightData);
          break;
        case 'BOTH':
          await Promise.all([
            //this.kafkaService.publishFlightData(flightData),
            this.backendIntegrationService.publishFlightTracking(flightData)
          ]);
          break;
        default:
          throw new Error(`Unknown publish mode: ${publishMode}`);
      }
    } catch (error) {
      console.error(`❌ Error publishing flight data (mode: ${publishMode}):`, error);
      this.stats.errors++;
      throw error;
    }
  }

  // Unified method to publish vessel data based on mode
  async publishVesselData(vesselData, publishMode = 'KAFKA') {
    try {
      switch (publishMode) {
        case 'KAFKA':
          //await //this.kafkaService.publishShipData(vesselData);
          break;
        case 'BACKEND':
          await this.backendIntegrationService.publishVesselTracking(vesselData);
          break;
        case 'BOTH':
          await Promise.all([
            //this.kafkaService.publishShipData(vesselData),
            this.backendIntegrationService.publishVesselTracking(vesselData)
          ]);
          break;
        default:
          throw new Error(`Unknown publish mode: ${publishMode}`);
      }
    } catch (error) {
      console.error(`❌ Error publishing vessel data (mode: ${publishMode}):`, error);
      this.stats.errors++;
      throw error;
    }
  }

  async generateAndPublishShips(options) {
    try {
      const activeShips = this.movementSimulator.getAllActiveShips();
      const currentShipCount = activeShips.length;

      // Create new ships if below max limit
      if (currentShipCount < options.maxShips) {
        const newShipsNeeded = Math.min(
          options.maxShips - currentShipCount,
          Math.floor(Math.random() * 2) + 1 // Add 1-2 new ships randomly
        );

        for (let i = 0; i < newShipsNeeded; i++) {
          const newShip = this.movementSimulator.createNewShip();
          this.stats.shipsGenerated++;
          
          if (options.batchMode) {
            this.pendingShips = this.pendingShips || [];
            this.pendingShips.push(newShip);
          } else {
            await //this.kafkaService.publishShipData(newShip);
            this.stats.shipsPublished++;
          }
        }
      }

      // Update existing ships
      const updatedShips = [];
      for (const shipId of activeShips) {
        const updatedShip = this.movementSimulator.updateShip(
          shipId, 
          options.shipInterval / 1000
        );
        
        if (updatedShip) {
          if (options.batchMode) {
            updatedShips.push(updatedShip);
          } else {
            await //this.kafkaService.publishShipData(updatedShip);
            this.stats.shipsPublished++;
          }
        }
      }

      // Handle batch publishing
      if (options.batchMode && updatedShips.length > 0) {
        await //this.kafkaService.publishBatchShipData(updatedShips);
        this.stats.shipsPublished += updatedShips.length;
      }

      // Handle pending new ships in batch mode
      if (options.batchMode && this.pendingShips && this.pendingShips.length > 0) {
        await //this.kafkaService.publishBatchShipData(this.pendingShips);
        this.stats.shipsPublished += this.pendingShips.length;
        this.pendingShips = [];
      }

    } catch (error) {
      console.error('❌ Error generating/publishing ships:', error);
      this.stats.errors++;
    }
  }

  stopSimulation() {
    if (!this.isRunning) {
      throw new Error('Simulation is not running');
    }

    console.log('⏹️ Stopping simulation...');

    // Clear all intervals
    Object.values(this.intervals).forEach(interval => {
      if (interval) clearInterval(interval);
    });

    this.intervals = {
      flight: null,
      ship: null,
      cleanup: null
    };

    this.isRunning = false;
    console.log('✅ Simulation stopped successfully');
    
    return this.getStatus();
  }

  getStatus() {
    const activeFlights = this.movementSimulator.getAllActiveFlights();
    const activeShips = this.movementSimulator.getAllActiveShips();
    
    return {
      activeVehicles: {
        flights: activeFlights.length,
        ships: activeShips.length
      },
      isRunning: this.isRunning,
      kafka:,
      stats: {
        ...this.stats,
        uptime: this.stats.startTime ?
            Math.floor((new Date() - this.stats.startTime) / 1000) : 0
      } //this.kafkaService.getConnectionStatus()
    };
  }

  async getDetailedStatus() {
    const basicStatus = this.getStatus();
    
    try {
//      const topicMetadata = await //this.kafkaService.getTopicMetadata();
  //    const topics = await //this.kafkaService.listTopics();
      
      return {
        ...basicStatus,
        kafka: {
          ...basicStatus.kafka,
          topicMetadata,
          availableTopics: topics
        }
      };
    } catch (error) {
      console.error('❌ Error getting detailed status:', error);
      return basicStatus;
    }
  }

  // Scenario-based simulation methods
  async startAirportScenario(airportLocation = 'tanSonNhat') {
    const location = config.locations[airportLocation];
    if (!location) {
      throw new Error(`Unknown airport location: ${airportLocation}`);
    }

    console.log(`🛫 Starting airport scenario at ${airportLocation}...`);
    
    // Generate multiple flights around the airport
    const flights = [];
    for (let i = 0; i < 10; i++) {
      const flight = this.movementSimulator.createNewFlight();
      // Override position to be around the airport
      flight.Latitude = location.lat + this.movementSimulator.random(-0.05, 0.05);
      flight.Longitude = location.lon + this.movementSimulator.random(-0.05, 0.05);
      flights.push(flight);
    }

    await //this.kafkaService.publishBatchFlightData(flights);
    this.stats.flightsPublished += flights.length;
    
    console.log(`✅ Airport scenario started with ${flights.length} flights`);
    return flights;
  }

  async startPortScenario() {
    console.log('🚢 Starting port scenario...');
    
    // Generate ships around major ports
    const ships = [];
    const ports = [
      { lat: 10.8, lon: 107.1 }, // Ho Chi Minh City port
      { lat: 20.9, lon: 106.9 }, // Hai Phong port
    ];

    for (const port of ports) {
      for (let i = 0; i < 5; i++) {
        const ship = this.movementSimulator.createNewShip();
        ship.latitude = port.lat + this.movementSimulator.random(-0.1, 0.1);
        ship.longitude = port.lon + this.movementSimulator.random(-0.1, 0.1);
        ships.push(ship);
      }
    }

    await //this.kafkaService.publishBatchShipData(ships);
    this.stats.shipsPublished += ships.length;
    
    console.log(`✅ Port scenario started with ${ships.length} ships`);
    return ships;
  }

  async shutdown() {
    try {
      console.log('🔄 Shutting down Simulation Service...');
      
      if (this.isRunning) {
        this.stopSimulation();
      }
      
      await //this.kafkaService.disconnect();
      console.log('✅ Simulation Service shutdown completed');
    } catch (error) {
      console.error('❌ Error during shutdown:', error);
      throw error;
    }
  }
}

module.exports = SimulationService; 