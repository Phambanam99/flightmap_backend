const MovementSimulator = require('../utils/dataGenerators');
const apiClient = require('../utils/apiClient');
const config = require('../config');
const { vesselLogger } = require('../utils/logger');

class VesselSimulator {
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
      vesselLogger.warn('Vessel simulator is already running');
      return;
    }

    this.isRunning = true;
    this.statistics.startTime = new Date();
    vesselLogger.info('Starting vessel simulator', {
      maxVessels: config.simulation.maxVessels,
      interval: config.simulation.vesselInterval
    });

    // Initialize with some vessels
    const initialVessels = Math.min(config.simulation.maxVessels / 2, 10);
    for (let i = 0; i < initialVessels; i++) {
      await this.generateAndPublishVessel();
      // Small delay to avoid overwhelming the API
      await new Promise(resolve => setTimeout(resolve, 200));
    }

    // Start the simulation loop
    this.interval = setInterval(async () => {
      if (!this.isRunning) return;

      try {
        await this.simulateVessels();
      } catch (error) {
        vesselLogger.error('Error in vessel simulation loop', error);
      }
    }, config.simulation.vesselInterval);

    // Cleanup old vessels periodically
    setInterval(() => {
      this.simulator.cleanupInactive();
    }, 120000); // Every 2 minutes
  }

  async stop() {
    this.isRunning = false;
    if (this.interval) {
      clearInterval(this.interval);
      this.interval = null;
    }
    vesselLogger.info('Vessel simulator stopped', this.getStatistics());
  }

  async simulateVessels() {
    const activeVessels = this.simulator.getAllActiveVessels();
    const stats = this.simulator.getStatistics();

    // Update existing vessels
    const updatePromises = activeVessels.map(async (voyageId) => {
      try {
        const updatedVessel = this.simulator.updateVessel(voyageId, config.simulation.vesselInterval / 1000);
        if (updatedVessel) {
          await this.publishVessel(updatedVessel);
        }
      } catch (error) {
        vesselLogger.error(`Error updating vessel ${voyageId}`, error);
        this.statistics.totalFailed++;
      }
    });

    // Process updates in batches to avoid overwhelming the API
    const batchSize = config.simulation.batchSize;
    for (let i = 0; i < updatePromises.length; i += batchSize) {
      const batch = updatePromises.slice(i, i + batchSize);
      await Promise.all(batch);
    }

    // Add new vessels if below maximum
    if (stats.activeVessels < config.simulation.maxVessels) {
      const vesselsToAdd = Math.min(
        config.simulation.maxVessels - stats.activeVessels,
        Math.random() < 0.1 ? 1 : 0 // 10% chance to add a new vessel
      );

      for (let i = 0; i < vesselsToAdd; i++) {
        await this.generateAndPublishVessel();
      }
    }

    this.statistics.lastUpdate = new Date();
  }

  async generateAndPublishVessel() {
    try {
      const newVessel = this.simulator.createNewVessel();
      this.statistics.totalGenerated++;
      await this.publishVessel(newVessel);
    } catch (error) {
      vesselLogger.error('Error generating new vessel', error);
      this.statistics.totalFailed++;
    }
  }

  async publishVessel(vessel) {
    try {
      await apiClient.publishVesselTracking(vessel);
      this.statistics.totalPublished++;

      vesselLogger.debug('Published vessel data', {
        voyageId: vessel.voyageId,
        position: { lat: vessel.latitude, lon: vessel.longitude },
        speed: vessel.speed,
        course: vessel.course
      });
    } catch (error) {
      vesselLogger.error('Failed to publish vessel', {
        voyageId: vessel.voyageId,
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
      activeVessels: stats.activeVessels,
      vesselDetails: {
        count: stats.activeVessels,
        ids: stats.vesselIds.slice(0, 10) // Show first 10 IDs
      }
    };
  }

  // Get specific vessel details
  getVesselDetails(voyageId) {
    const vessels = this.simulator.activeVessels;
    return vessels.get(voyageId) || null;
  }

  // Force update all vessels
  async forceUpdateAll() {
    const activeVessels = this.simulator.getAllActiveVessels();
    vesselLogger.info(`Force updating ${activeVessels.length} vessels`);

    for (const voyageId of activeVessels) {
      const updatedVessel = this.simulator.updateVessel(voyageId, 0);
      if (updatedVessel) {
        await this.publishVessel(updatedVessel);
      }
    }
  }

  // Get vessels in a specific area
  getVesselsInArea(bounds) {
    const vesselsInArea = [];
    const vessels = this.simulator.activeVessels;

    for (const [voyageId, vessel] of vessels) {
      if (vessel.latitude >= bounds.minLat && 
          vessel.latitude <= bounds.maxLat &&
          vessel.longitude >= bounds.minLon && 
          vessel.longitude <= bounds.maxLon) {
        vesselsInArea.push({
          voyageId,
          ...vessel
        });
      }
    }

    return vesselsInArea;
  }
}

module.exports = VesselSimulator; 