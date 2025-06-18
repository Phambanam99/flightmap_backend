const express = require('express');
const cors = require('cors');
const config = require('./config');
const FlightSimulator = require('./services/flightSimulator');
const VesselSimulator = require('./services/vesselSimulator');
const apiClient = require('./utils/apiClient');
const { systemLogger } = require('./utils/logger');

// Initialize Express app
const app = express();
app.use(cors());
app.use(express.json());

// Initialize simulators
const flightSimulator = new FlightSimulator();
const vesselSimulator = new VesselSimulator();

// Health check endpoint
app.get('/health', (req, res) => {
  res.json({
    status: 'healthy',
    service: 'tracking-data-simulator',
    timestamp: new Date().toISOString(),
    uptime: process.uptime()
  });
});

// Get simulator status
app.get('/status', async (req, res) => {
  try {
    const consumerStatus = await apiClient.getConsumerStatus();
    
    res.json({
      simulators: {
        flight: {
          enabled: config.simulation.enableFlights,
          statistics: flightSimulator.getStatistics()
        },
        vessel: {
          enabled: config.simulation.enableVessels,
          statistics: vesselSimulator.getStatistics()
        }
      },
      consumer: consumerStatus?.data || { status: 'Unable to fetch consumer status' },
      config: {
        apiBaseUrl: config.api.baseUrl,
        flightInterval: config.simulation.flightInterval,
        vesselInterval: config.simulation.vesselInterval,
        maxFlights: config.simulation.maxFlights,
        maxVessels: config.simulation.maxVessels
      }
    });
  } catch (error) {
    systemLogger.error('Error getting status', error);
    res.status(500).json({ error: error.message });
  }
});

// Control endpoints
app.post('/control/start', async (req, res) => {
  try {
    const { simulators = ['flight', 'vessel'] } = req.body;
    const started = [];

    if (simulators.includes('flight') && config.simulation.enableFlights) {
      await flightSimulator.start();
      started.push('flight');
    }

    if (simulators.includes('vessel') && config.simulation.enableVessels) {
      await vesselSimulator.start();
      started.push('vessel');
    }

    systemLogger.info('Simulators started', { started });
    res.json({ 
      message: 'Simulators started successfully', 
      started 
    });
  } catch (error) {
    systemLogger.error('Error starting simulators', error);
    res.status(500).json({ error: error.message });
  }
});

app.post('/control/stop', async (req, res) => {
  try {
    const { simulators = ['flight', 'vessel'] } = req.body;
    const stopped = [];

    if (simulators.includes('flight')) {
      await flightSimulator.stop();
      stopped.push('flight');
    }

    if (simulators.includes('vessel')) {
      await vesselSimulator.stop();
      stopped.push('vessel');
    }

    systemLogger.info('Simulators stopped', { stopped });
    res.json({ 
      message: 'Simulators stopped successfully', 
      stopped 
    });
  } catch (error) {
    systemLogger.error('Error stopping simulators', error);
    res.status(500).json({ error: error.message });
  }
});

// Force update all data
app.post('/control/force-update', async (req, res) => {
  try {
    const updates = [];
    
    if (req.body.flight !== false) {
      await flightSimulator.forceUpdateAll();
      updates.push('flight');
    }
    
    if (req.body.vessel !== false) {
      await vesselSimulator.forceUpdateAll();
      updates.push('vessel');
    }

    res.json({ 
      message: 'Force update completed', 
      updated: updates 
    });
  } catch (error) {
    systemLogger.error('Error forcing update', error);
    res.status(500).json({ error: error.message });
  }
});

// Get specific flight details
app.get('/flights/:id', (req, res) => {
  const flight = flightSimulator.getFlightDetails(parseInt(req.params.id));
  if (flight) {
    res.json(flight);
  } else {
    res.status(404).json({ error: 'Flight not found' });
  }
});

// Get specific vessel details
app.get('/vessels/:id', (req, res) => {
  const vessel = vesselSimulator.getVesselDetails(parseInt(req.params.id));
  if (vessel) {
    res.json(vessel);
  } else {
    res.status(404).json({ error: 'Vessel not found' });
  }
});

// Get vessels in area
app.post('/vessels/in-area', (req, res) => {
  const { minLat, maxLat, minLon, maxLon } = req.body;
  
  if (!minLat || !maxLat || !minLon || !maxLon) {
    return res.status(400).json({ error: 'Missing required bounds parameters' });
  }

  const vessels = vesselSimulator.getVesselsInArea({
    minLat: parseFloat(minLat),
    maxLat: parseFloat(maxLat),
    minLon: parseFloat(minLon),
    maxLon: parseFloat(maxLon)
  });

  res.json({ vessels, count: vessels.length });
});

// Serve monitoring dashboard
app.get('/', (req, res) => {
  res.send(`
    <!DOCTYPE html>
    <html>
    <head>
      <title>Tracking Data Simulator</title>
      <style>
        body {
          font-family: Arial, sans-serif;
          margin: 20px;
          background-color: #f5f5f5;
        }
        .container {
          max-width: 1200px;
          margin: 0 auto;
        }
        .status-card {
          background: white;
          border-radius: 8px;
          padding: 20px;
          margin: 10px 0;
          box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }
        .metric {
          display: inline-block;
          margin: 10px 20px 10px 0;
        }
        .metric-value {
          font-size: 24px;
          font-weight: bold;
          color: #333;
        }
        .metric-label {
          color: #666;
          font-size: 14px;
        }
        button {
          background-color: #4CAF50;
          color: white;
          padding: 10px 20px;
          border: none;
          border-radius: 4px;
          cursor: pointer;
          margin: 5px;
        }
        button:hover {
          background-color: #45a049;
        }
        button.stop {
          background-color: #f44336;
        }
        button.stop:hover {
          background-color: #da190b;
        }
        .running {
          color: #4CAF50;
        }
        .stopped {
          color: #f44336;
        }
        h1, h2 {
          color: #333;
        }
        .controls {
          margin: 20px 0;
        }
        .log-area {
          background: #f0f0f0;
          padding: 10px;
          border-radius: 4px;
          font-family: monospace;
          font-size: 12px;
          max-height: 200px;
          overflow-y: auto;
        }
      </style>
    </head>
    <body>
      <div class="container">
        <h1>Tracking Data Simulator</h1>
        
        <div class="controls">
          <button onclick="startSimulators()">Start All</button>
          <button onclick="stopSimulators()" class="stop">Stop All</button>
          <button onclick="forceUpdate()">Force Update</button>
          <button onclick="refreshStatus()">Refresh Status</button>
        </div>

        <div id="status-container">
          <p>Loading status...</p>
        </div>
      </div>

      <script>
        async function fetchStatus() {
          try {
            const response = await fetch('/status');
            const data = await response.json();
            updateStatusDisplay(data);
          } catch (error) {
            console.error('Error fetching status:', error);
            document.getElementById('status-container').innerHTML = 
              '<p style="color: red;">Error fetching status: ' + error.message + '</p>';
          }
        }

        function updateStatusDisplay(data) {
          const container = document.getElementById('status-container');
          
          let html = '';
          
          // Flight Simulator Status
          if (data.simulators.flight.enabled) {
            const flight = data.simulators.flight.statistics;
            const isRunning = flight.startTime !== null;
            
            html += '<div class="status-card">';
            html += '<h2>Flight Simulator <span class="' + (isRunning ? 'running' : 'stopped') + '">(' + (isRunning ? 'Running' : 'Stopped') + ')</span></h2>';
            
            if (isRunning) {
              html += '<div class="metric"><div class="metric-value">' + flight.activeFlights + '</div><div class="metric-label">Active Flights</div></div>';
              html += '<div class="metric"><div class="metric-value">' + flight.totalPublished + '</div><div class="metric-label">Total Published</div></div>';
              html += '<div class="metric"><div class="metric-value">' + flight.publishRate + '</div><div class="metric-label">Publish Rate</div></div>';
              html += '<div class="metric"><div class="metric-value">' + flight.successRate + '</div><div class="metric-label">Success Rate</div></div>';
              html += '<div class="metric"><div class="metric-value">' + flight.runtime + '</div><div class="metric-label">Runtime</div></div>';
            }
            
            html += '</div>';
          }
          
          // Vessel Simulator Status
          if (data.simulators.vessel.enabled) {
            const vessel = data.simulators.vessel.statistics;
            const isRunning = vessel.startTime !== null;
            
            html += '<div class="status-card">';
            html += '<h2>Vessel Simulator <span class="' + (isRunning ? 'running' : 'stopped') + '">(' + (isRunning ? 'Running' : 'Stopped') + ')</span></h2>';
            
            if (isRunning) {
              html += '<div class="metric"><div class="metric-value">' + vessel.activeVessels + '</div><div class="metric-label">Active Vessels</div></div>';
              html += '<div class="metric"><div class="metric-value">' + vessel.totalPublished + '</div><div class="metric-label">Total Published</div></div>';
              html += '<div class="metric"><div class="metric-value">' + vessel.publishRate + '</div><div class="metric-label">Publish Rate</div></div>';
              html += '<div class="metric"><div class="metric-value">' + vessel.successRate + '</div><div class="metric-label">Success Rate</div></div>';
              html += '<div class="metric"><div class="metric-value">' + vessel.runtime + '</div><div class="metric-label">Runtime</div></div>';
            }
            
            html += '</div>';
          }
          
          // Consumer Status
          if (data.consumer) {
            html += '<div class="status-card">';
            html += '<h2>Kafka Consumer Status</h2>';
            
            if (data.consumer.consumerHealth) {
              html += '<div class="metric"><div class="metric-value ' + (data.consumer.consumerHealth === 'HEALTHY' ? 'running' : 'stopped') + '">' + data.consumer.consumerHealth + '</div><div class="metric-label">Health Status</div></div>';
            }
            
            if (data.consumer.consumerStatistics) {
              const stats = data.consumer.consumerStatistics;
              Object.keys(stats).forEach(key => {
                if (typeof stats[key] !== 'object') {
                  html += '<div class="metric"><div class="metric-value">' + stats[key] + '</div><div class="metric-label">' + key + '</div></div>';
                }
              });
            }
            
            html += '</div>';
          }
          
          // Configuration
          html += '<div class="status-card">';
          html += '<h2>Configuration</h2>';
          html += '<div class="metric"><div class="metric-value">' + data.config.apiBaseUrl + '</div><div class="metric-label">API Base URL</div></div>';
          html += '<div class="metric"><div class="metric-value">' + data.config.flightInterval + 'ms</div><div class="metric-label">Flight Update Interval</div></div>';
          html += '<div class="metric"><div class="metric-value">' + data.config.vesselInterval + 'ms</div><div class="metric-label">Vessel Update Interval</div></div>';
          html += '<div class="metric"><div class="metric-value">' + data.config.maxFlights + '</div><div class="metric-label">Max Flights</div></div>';
          html += '<div class="metric"><div class="metric-value">' + data.config.maxVessels + '</div><div class="metric-label">Max Vessels</div></div>';
          html += '</div>';
          
          container.innerHTML = html;
        }

        async function startSimulators() {
          try {
            const response = await fetch('/control/start', { method: 'POST' });
            const data = await response.json();
            alert(data.message);
            fetchStatus();
          } catch (error) {
            alert('Error starting simulators: ' + error.message);
          }
        }

        async function stopSimulators() {
          try {
            const response = await fetch('/control/stop', { method: 'POST' });
            const data = await response.json();
            alert(data.message);
            fetchStatus();
          } catch (error) {
            alert('Error stopping simulators: ' + error.message);
          }
        }

        async function forceUpdate() {
          try {
            const response = await fetch('/control/force-update', { method: 'POST' });
            const data = await response.json();
            alert(data.message);
          } catch (error) {
            alert('Error forcing update: ' + error.message);
          }
        }

        function refreshStatus() {
          fetchStatus();
        }

        // Initial load
        fetchStatus();

        // Auto-refresh every 5 seconds
        setInterval(fetchStatus, 5000);
      </script>
    </body>
    </html>
  `);
});

// Start the server
const server = app.listen(config.port, () => {
  systemLogger.info(`Tracking Data Simulator server running on port ${config.port}`);
  console.log(`\n🚀 Tracking Data Simulator is running!`);
  console.log(`\n📊 Dashboard: http://localhost:${config.port}`);
  console.log(`📡 API Base URL: ${config.api.baseUrl}`);
  console.log(`✈️  Flight Simulator: ${config.simulation.enableFlights ? 'Enabled' : 'Disabled'}`);
  console.log(`🚢 Vessel Simulator: ${config.simulation.enableVessels ? 'Enabled' : 'Disabled'}`);
  console.log(`\nPress Ctrl+C to stop\n`);
});

// Graceful shutdown
process.on('SIGINT', async () => {
  systemLogger.info('Shutting down simulators...');
  console.log('\n\nShutting down simulators...');
  
  await flightSimulator.stop();
  await vesselSimulator.stop();
  
  server.close(() => {
    systemLogger.info('Server closed');
    console.log('Server closed. Goodbye!');
    process.exit(0);
  });
});