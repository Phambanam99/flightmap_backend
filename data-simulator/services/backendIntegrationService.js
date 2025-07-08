const axios = require('axios');
const config = require('../config');

class BackendIntegrationService {
  constructor() {
    this.backendBaseUrl = process.env.BACKEND_BASE_URL || 'http://localhost:8080';
    this.publishEndpoints = {
      flight: '/api/tracking/publish/flight',
      vessel: '/api/tracking/publish/vessel'
    };
    this.consumerEndpoints = {
      status: '/api/tracking/consumer/status',
      resetCounters: '/api/tracking/consumer/reset-counters',
      triggerBatch: '/api/tracking/consumer/trigger-batch-update',
      cacheMetrics: '/api/tracking/consumer/cache/metrics'
    };
    this.stats = {
      flightsSent: 0,
      vesselsSent: 0,
      errors: 0,
      lastError: null,
      startTime: Date.now()
    };
  }

  // Publish flight tracking data to Java backend
  async publishFlightTracking(flightData) {
    try {
      const url = `${this.backendBaseUrl}${this.publishEndpoints.flight}`;
      
      const response = await axios.post(url, flightData, {
        headers: {
          'Content-Type': 'application/json',
          'X-Source': 'DATA_SIMULATOR'
        },
        timeout: 5000
      });

      if (response.data.success) {
        this.stats.flightsSent++;
        console.log(`✈️ Flight data published to backend: ${flightData.callsign} (ID: ${flightData.id})`);
        return { success: true, data: response.data };
      } else {
        throw new Error(`Backend returned error: ${response.data.message}`);
      }

    } catch (error) {
      this.stats.errors++;
      this.stats.lastError = {
        timestamp: new Date().toISOString(),
        error: error.message,
        data: { flightId: flightData.id, callsign: flightData.callsign }
      };
      
      console.error(`❌ Error publishing flight data to backend:`, error.message);
      throw error;
    }
  }

  // Publish vessel tracking data to Java backend
  async publishVesselTracking(vesselData) {
    try {
      const url = `${this.backendBaseUrl}${this.publishEndpoints.vessel}`;
      
      const response = await axios.post(url, vesselData, {
        headers: {
          'Content-Type': 'application/json',
          'X-Source': 'DATA_SIMULATOR'
        },
        timeout: 5000
      });

      if (response.data.success) {
        this.stats.vesselsSent++;
        console.log(`🚢 Vessel data published to backend: ${vesselData.vesselName} (Voyage: ${vesselData.voyageId})`);
        return { success: true, data: response.data };
      } else {
        throw new Error(`Backend returned error: ${response.data.message}`);
      }

    } catch (error) {
      this.stats.errors++;
      this.stats.lastError = {
        timestamp: new Date().toISOString(),
        error: error.message,
        data: { voyageId: vesselData.voyageId, vesselName: vesselData.vesselName }
      };
      
      console.error(`❌ Error publishing vessel data to backend:`, error.message);
      throw error;
    }
  }

  // Publish batch flight data
  async publishBatchFlightData(flightDataArray) {
    const results = [];
    
    for (const flightData of flightDataArray) {
      try {
        const result = await this.publishFlightTracking(flightData);
        results.push({ success: true, id: flightData.id, result });
      } catch (error) {
        results.push({ success: false, id: flightData.id, error: error.message });
      }
    }
    
    console.log(`📦 Batch flight data published: ${results.filter(r => r.success).length}/${results.length} successful`);
    return results;
  }

  // Publish batch vessel data
  async publishBatchVesselData(vesselDataArray) {
    const results = [];
    
    for (const vesselData of vesselDataArray) {
      try {
        const result = await this.publishVesselTracking(vesselData);
        results.push({ success: true, voyageId: vesselData.voyageId, result });
      } catch (error) {
        results.push({ success: false, voyageId: vesselData.voyageId, error: error.message });
      }
    }
    
    console.log(`📦 Batch vessel data published: ${results.filter(r => r.success).length}/${results.length} successful`);
    return results;
  }

  // Monitor backend consumer status
  async getConsumerStatus() {
    try {
      const url = `${this.backendBaseUrl}${this.consumerEndpoints.status}`;
      
      const response = await axios.get(url, {
        timeout: 3000,
        headers: { 'X-Source': 'DATA_SIMULATOR' }
      });

      return response.data;
    } catch (error) {
      console.error('❌ Error getting consumer status:', error.message);
      throw error;
    }
  }

  // Reset consumer counters
  async resetConsumerCounters() {
    try {
      const url = `${this.backendBaseUrl}${this.consumerEndpoints.resetCounters}`;
      
      const response = await axios.post(url, {}, {
        timeout: 3000,
        headers: { 'X-Source': 'DATA_SIMULATOR' }
      });

      console.log('🔄 Consumer counters reset successfully');
      return response.data;
    } catch (error) {
      console.error('❌ Error resetting consumer counters:', error.message);
      throw error;
    }
  }

  // Trigger manual batch update
  async triggerBatchUpdate() {
    try {
      const url = `${this.backendBaseUrl}${this.consumerEndpoints.triggerBatch}`;
      
      const response = await axios.post(url, {}, {
        timeout: 5000,
        headers: { 'X-Source': 'DATA_SIMULATOR' }
      });

      console.log('🚀 Manual batch update triggered successfully');
      return response.data;
    } catch (error) {
      console.error('❌ Error triggering batch update:', error.message);
      throw error;
    }
  }

  // Get cache metrics
  async getCacheMetrics() {
    try {
      const url = `${this.backendBaseUrl}${this.consumerEndpoints.cacheMetrics}`;
      
      const response = await axios.get(url, {
        timeout: 3000,
        headers: { 'X-Source': 'DATA_SIMULATOR' }
      });

      return response.data;
    } catch (error) {
      console.error('❌ Error getting cache metrics:', error.message);
      throw error;
    }
  }

  // Check backend health
  async checkBackendHealth() {
    try {
      const url = `${this.backendBaseUrl}/actuator/health`;
      
      const response = await axios.get(url, {
        timeout: 3000,
        headers: { 'X-Source': 'DATA_SIMULATOR' }
      });

      return {
        healthy: response.status === 200,
        status: response.data
      };
    } catch (error) {
      return {
        healthy: false,
        error: error.message
      };
    }
  }

  // Get integration statistics
  getStats() {
    const runtime = Date.now() - this.stats.startTime;
    
    return {
      ...this.stats,
      runtime: runtime,
      averageFlightsPerMinute: this.stats.flightsSent / (runtime / 60000),
      averageVesselsPerMinute: this.stats.vesselsSent / (runtime / 60000),
      successRate: ((this.stats.flightsSent + this.stats.vesselsSent) / 
                   (this.stats.flightsSent + this.stats.vesselsSent + this.stats.errors)) * 100
    };
  }

  // Reset statistics
  resetStats() {
    this.stats = {
      flightsSent: 0,
      vesselsSent: 0,
      errors: 0,
      lastError: null,
      startTime: Date.now()
    };
    console.log('📊 Integration statistics reset');
  }
}

module.exports = BackendIntegrationService; 