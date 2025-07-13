const axios = require('axios');
const config = require('../config');
const { apiLogger } = require('./logger');

class ApiClient {
  constructor() {
    this.client = axios.create({
      baseURL: config.api.baseUrl,
      timeout: config.api.timeout,
      headers: {
        'Content-Type': 'application/json'
      }
    });

    // Add request interceptor for logging
    this.client.interceptors.request.use(
      (request) => {
        apiLogger.debug(`API Request: ${request.method?.toUpperCase()} ${request.url}`, {
          data: request.data
        });
        return request;
      },
      (error) => {
        apiLogger.error('API Request Error:', error);
        return Promise.reject(error);
      }
    );

    // Add response interceptor for logging
    this.client.interceptors.response.use(
      (response) => {
        apiLogger.debug(`API Response: ${response.status} ${response.config.url}`, {
          data: response.data
        });
        return response;
      },
      (error) => {
        apiLogger.error(`API Response Error: ${error.response?.status || 'No response'} ${error.config?.url}`, {
          error: error.message,
          data: error.response?.data
        });
        return Promise.reject(error);
      }
    );
  }

  async retry(fn, retries = config.api.retryAttempts, delay = config.api.retryDelay) {
    try {
      return await fn();
    } catch (error) {
      if (retries > 0) {
        apiLogger.warn(`Retrying request, attempts remaining: ${retries}`, {
          error: error.message
        });
        await new Promise(resolve => setTimeout(resolve, delay));
        return this.retry(fn, retries - 1, delay * 2); // Exponential backoff
      }
      throw error;
    }
  }

  async publishFlightTracking(flightData) {
    return this.retry(async () => {
      const response = await this.client.post(
        config.api.endpoints.publishFlight,
        flightData
      );
      return response.data;
    });
  }

  async publishVesselTracking(vesselData) {
    return this.retry(async () => {
      const response = await this.client.post(
        config.api.endpoints.publishVessel,
        vesselData
      );
      return response.data;
    });
  }

  async getConsumerStatus() {
    try {
      const response = await this.client.get(config.api.endpoints.consumerStatus);
      return response.data;
    } catch (error) {
      apiLogger.error('Failed to get consumer status:', error);
      return null;
    }
  }

  async publishBatch(endpoint, dataArray) {
    const results = {
      successful: 0,
      failed: 0,
      errors: []
    };

    for (const data of dataArray) {
      try {
        await this.retry(async () => {
          await this.client.post(endpoint, data);
        });
        results.successful++;
      } catch (error) {
        results.failed++;
        results.errors.push({
          data,
          error: error.message
        });
        apiLogger.error(`Failed to publish data:`, {
          endpoint,
          error: error.message,
          data
        });
      }
    }

    return results;
  }
}

module.exports = new ApiClient(); 