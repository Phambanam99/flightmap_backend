const { Kafka } = require('kafkajs');
const config = require('../config');

class KafkaService {
  constructor() {
    this.kafka = new Kafka({
      clientId: config.kafka.clientId,
      brokers: config.kafka.brokers,
    });

    this.producer = this.kafka.producer({
      maxInFlightRequests: 1,
      idempotent: true,
      transactionTimeout: 30000,
    });

    this.admin = this.kafka.admin();
    this.isConnected = false;
  }

  async connect() {
    try {
      console.log('🔌 Connecting to Kafka...');
      
      // Connect producer
      await this.producer.connect();
      console.log('✅ Kafka Producer connected');

      // Connect admin client
      await this.admin.connect();
      console.log('✅ Kafka Admin connected');

      // Create topics if they don't exist
      await this.createTopicsIfNeeded();

      this.isConnected = true;
      console.log('🚀 Kafka service initialized successfully');
    } catch (error) {
      console.error('❌ Failed to connect to Kafka:', error);
      throw error;
    }
  }

  async createTopicsIfNeeded() {
    try {
      const topics = [
        {
          topic: config.kafka.topics.flight,
          numPartitions: 3,
          replicationFactor: 1,
        },
        {
          topic: config.kafka.topics.ship,
          numPartitions: 3,
          replicationFactor: 1,
        }
      ];

      console.log('📋 Creating Kafka topics if needed...');
      await this.admin.createTopics({
        topics: topics,
        waitForLeaders: true,
      });
      
      console.log('✅ Topics created/verified successfully');
    } catch (error) {
      if (error.type === 'TOPIC_ALREADY_EXISTS') {
        console.log('ℹ️ Topics already exist');
      } else {
        console.error('❌ Error creating topics:', error);
        throw error;
      }
    }
  }

  async publishFlightData(flightData) {
    if (!this.isConnected) {
      throw new Error('Kafka service is not connected');
    }

    try {
      const key = flightData.Id ? flightData.Id.toString() : 'unknown';
      
      await this.producer.send({
        topic: config.kafka.topics.flight,
        messages: [
          {
            key: key,
            value: JSON.stringify(flightData),
            timestamp: Date.now().toString(),
          },
        ],
      });

      console.log(`✈️ Flight data published: ${flightData.Callsign} (${key})`);
    } catch (error) {
      console.error('❌ Error publishing flight data:', error);
      throw error;
    }
  }

  async publishShipData(shipData) {
    if (!this.isConnected) {
      throw new Error('Kafka service is not connected');
    }

    try {
      const key = shipData.voyageId ? shipData.voyageId.toString() : 'unknown';
      
      await this.producer.send({
        topic: config.kafka.topics.ship,
        messages: [
          {
            key: key,
            value: JSON.stringify(shipData),
            timestamp: Date.now().toString(),
          },
        ],
      });

      console.log(`🚢 Ship data published: Voyage ${key}`);
    } catch (error) {
      console.error('❌ Error publishing ship data:', error);
      throw error;
    }
  }

  async publishBatchFlightData(flightDataArray) {
    if (!this.isConnected) {
      throw new Error('Kafka service is not connected');
    }

    try {
      const messages = flightDataArray.map(flight => ({
        key: flight.Id ? flight.Id.toString() : 'unknown',
        value: JSON.stringify(flight),
        timestamp: Date.now().toString(),
      }));

      await this.producer.send({
        topic: config.kafka.topics.flight,
        messages: messages,
      });

      console.log(`✈️ Batch flight data published: ${flightDataArray.length} flights`);
    } catch (error) {
      console.error('❌ Error publishing batch flight data:', error);
      throw error;
    }
  }

  async publishBatchShipData(shipDataArray) {
    if (!this.isConnected) {
      throw new Error('Kafka service is not connected');
    }

    try {
      const messages = shipDataArray.map(ship => ({
        key: ship.voyageId ? ship.voyageId.toString() : 'unknown',
        value: JSON.stringify(ship),
        timestamp: Date.now().toString(),
      }));

      await this.producer.send({
        topic: config.kafka.topics.ship,
        messages: messages,
      });

      console.log(`🚢 Batch ship data published: ${shipDataArray.length} ships`);
    } catch (error) {
      console.error('❌ Error publishing batch ship data:', error);
      throw error;
    }
  }

  async getTopicMetadata() {
    if (!this.isConnected) {
      throw new Error('Kafka service is not connected');
    }

    try {
      const metadata = await this.admin.fetchTopicMetadata({
        topics: [config.kafka.topics.flight, config.kafka.topics.ship]
      });
      return metadata;
    } catch (error) {
      console.error('❌ Error fetching topic metadata:', error);
      throw error;
    }
  }

  async listTopics() {
    if (!this.isConnected) {
      throw new Error('Kafka service is not connected');
    }

    try {
      const topics = await this.admin.listTopics();
      return topics;
    } catch (error) {
      console.error('❌ Error listing topics:', error);
      throw error;
    }
  }

  async disconnect() {
    try {
      if (this.producer) {
        await this.producer.disconnect();
        console.log('🔌 Kafka Producer disconnected');
      }
      
      if (this.admin) {
        await this.admin.disconnect();
        console.log('🔌 Kafka Admin disconnected');
      }
      
      this.isConnected = false;
      console.log('✅ Kafka service disconnected successfully');
    } catch (error) {
      console.error('❌ Error disconnecting from Kafka:', error);
      throw error;
    }
  }

  getConnectionStatus() {
    return {
      connected: this.isConnected,
      brokers: config.kafka.brokers,
      topics: config.kafka.topics,
      clientId: config.kafka.clientId
    };
  }
}

module.exports = KafkaService; 