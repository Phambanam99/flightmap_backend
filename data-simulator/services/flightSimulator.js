const MovementSimulator = require('../utils/dataGenerators');
const apiClient = require('../utils/apiClient');
const config = require('../config');
const { flightLogger } = require('../utils/logger');

class FlightSimulator {
  constructor() {
    this.simulator = new MovementSimulator();
    this.isRunning = false;
    this.interval = null;
    this.statistics = {
      totalGenerated: 0,
      totalPublished: 0,
      totalFailed: 0,
      startTime: null,
      lastUpdate: null
    };
  }

  async start() {
    if (this.isRunning) {
      flightLogger.warn('Flight simulator is already running');
      return;
    }

    this.isRunning = true;
    this.statistics.startTime = new Date();
    flightLogger.info('Starting flight simulator', {
      maxFlights: config.simulation.maxFlights,
      interval: config.simulation.flightInterval
    });

    // Initialize with some flights
    const initialFlights = Math.min(config.simulation.maxFlights / 2, 20);
    for (let i = 0; i < initialFlights; i++) {
      await this.generateAndPublishFlight();
      // Small delay to avoid overwhelming the API
      await new Promise(resolve => setTimeout(resolve, 100));
    }

    // Start the simulation loop
    this.interval = setInterval(async () => {
      if (!this.isRunning) return;

      try {
        await this.simulateFlights();
      } catch (error) {
        flightLogger.error('Error in flight simulation loop', error);
      }
    }, config.simulation.flightInterval);

    // Cleanup old flights periodically
    setInterval(() => {
      this.simulator.cleanupInactive();
    }, 60000); // Every minute
  }

  async stop() {
    this.isRunning = false;
    if (this.interval) {
      clearInterval(this.interval);
      this.interval = null;
    }
    flightLogger.info('Flight simulator stopped', this.getStatistics());
  }

  async simulateFlights() {
    const activeFlights = this.simulator.getAllActiveFlights();
    const stats = this.simulator.getStatistics();

    // Update existing flights
    const updatePromises = activeFlights.map(async (flightId) => {
      try {
        const updatedFlight = this.simulator.updateFlight(flightId, config.simulation.flightInterval / 1000);
        if (updatedFlight) {
          await this.publishFlight(updatedFlight);
        }
      } catch (error) {
        flightLogger.error(`Error updating flight ${flightId}`, error);
        this.statistics.totalFailed++;
      }
    });

    // Process updates in batches to avoid overwhelming the API
    const batchSize = config.simulation.batchSize;
    for (let i = 0; i < updatePromises.length; i += batchSize) {
      const batch = updatePromises.slice(i, i + batchSize);
      await Promise.all(batch);
    }

    // Add new flights if below maximum
    if (stats.activeFlights < config.simulation.maxFlights) {
      const flightsToAdd = Math.min(
        config.simulation.maxFlights - stats.activeFlights,
        Math.random() < 0.3 ? 1 : 0 // 30% chance to add a new flight
      );

      for (let i = 0; i < flightsToAdd; i++) {
        await this.generateAndPublishFlight();
      }
    }

    this.statistics.lastUpdate = new Date();
  }

  async generateAndPublishFlight() {
    try {
      const newFlight = this.simulator.createNewFlight();
      this.statistics.totalGenerated++;
      await this.publishFlight(newFlight);
    } catch (error) {
      flightLogger.error('Error generating new flight', error);
      this.statistics.totalFailed++;
    }
  }

  async publishFlight(flight) {
    try {
      await apiClient.publishFlightTracking(flight);
      this.statistics.totalPublished++;

      flightLogger.debug('Published flight data', {
        flightId: flight.Id,
        callsign: flight.Callsign,
        position: { lat: flight.Latitude, lon: flight.Longitude },
        altitude: flight.Altitude,
        speed: flight.Speed
      });
    } catch (error) {
      flightLogger.error('Failed to publish flight', {
        flightId: flight.Id,
        error: error.message
      });
      this.statistics.totalFailed++;
      throw error;
    }
  }

  getStatistics() {
    const stats = this.simulator.getStatistics();
    const runtime = this.statistics.startTime 
      ? (Date.now() - this.statistics.startTime.getTime()) / 1000 
      : 0;

    return {
      ...this.statistics,
      runtime: `${Math.floor(runtime / 60)}m ${Math.floor(runtime % 60)}s`,
      publishRate: runtime > 0 ? (this.statistics.totalPublished / runtime).toFixed(2) + '/s' : '0/s',
      successRate: this.statistics.totalPublished > 0 
        ? ((this.statistics.totalPublished / (this.statistics.totalPublished + this.statistics.totalFailed)) * 100).toFixed(2) + '%' 
        : '0%',
      activeFlights: stats.activeFlights,
      flightDetails: {
        count: stats.activeFlights,
        ids: stats.flightIds.slice(0, 10) // Show first 10 IDs
      }
    };
  }

  // Get specific flight details
  getFlightDetails(flightId) {
    const flights = this.simulator.activeFlights;
    return flights.get(flightId) || null;
  }

  // Force update all flights
  async forceUpdateAll() {
    const activeFlights = this.simulator.getAllActiveFlights();
    flightLogger.info(`Force updating ${activeFlights.length} flights`);

    for (const flightId of activeFlights) {
      const updatedFlight = this.simulator.updateFlight(flightId, 0);
      if (updatedFlight) {
        await this.publishFlight(updatedFlight);
      }
    }
  }
}

module.exports = FlightSimulator; 