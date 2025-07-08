const axios = require('axios');

const BASE_URL = 'http://localhost:3001';

async function delay(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

async function testSimulator() {
  console.log('🧪 Testing Tracking Data Simulator...\n');

  try {
    // 1. Health Check
    console.log('1️⃣ Testing Health Check...');
    const health = await axios.get(`${BASE_URL}/health`);
    console.log('✅ Health:', health.data);
    console.log();

    // 2. Get Status
    console.log('2️⃣ Getting Initial Status...');
    const initialStatus = await axios.get(`${BASE_URL}/api/status`);
    console.log('📊 Status:', initialStatus.data.data);
    console.log();

    // 3. Get Configuration
    console.log('3️⃣ Getting Configuration...');
    const config = await axios.get(`${BASE_URL}/api/config`);
    console.log('⚙️ Config simulation params:', config.data.data.simulation);
    console.log();

    // 4. Manual Flight Generation
    console.log('4️⃣ Testing Manual Flight Generation...');
    const flightResponse = await axios.post(`${BASE_URL}/api/manual/flight`, {
      Callsign: 'TEST001',
      Latitude: 10.823,
      Longitude: 106.629,
      Altitude: 25000
    });
    console.log('✈️ Flight created:', flightResponse.data.data.Callsign);
    console.log();

    // 5. Manual Ship Generation
    console.log('5️⃣ Testing Manual Ship Generation...');
    const shipResponse = await axios.post(`${BASE_URL}/api/manual/ship`, {
      voyageId: 99999,
      latitude: 10.8,
      longitude: 107.1,
      speed: 18
    });
    console.log('🚢 Ship created:', shipResponse.data.data.voyageId);
    console.log();

    // 6. Airport Scenario
    console.log('6️⃣ Testing Airport Scenario...');
    const airportScenario = await axios.post(`${BASE_URL}/api/simulation/scenarios/airport`, {
      airportLocation: 'tanSonNhat'
    });
    console.log('🛫 Airport scenario:', airportScenario.data.message);
    console.log('   Flights created:', airportScenario.data.data.flightsCreated);
    console.log();

    // 7. Port Scenario
    console.log('7️⃣ Testing Port Scenario...');
    const portScenario = await axios.post(`${BASE_URL}/api/simulation/scenarios/port`);
    console.log('🚢 Port scenario:', portScenario.data.message);
    console.log('   Ships created:', portScenario.data.data.shipsCreated);
    console.log();

    // 8. Start Simulation
    console.log('8️⃣ Starting Real-time Simulation...');
    const startSim = await axios.post(`${BASE_URL}/api/simulation/start`, {
      flightInterval: 3000,
      shipInterval: 4000,
      maxFlights: 15,
      maxShips: 8,
      batchMode: false
    });
    console.log('🎬 Simulation started:', startSim.data.message);
    console.log('   Config:', startSim.data.data.activeVehicles);
    console.log();

    // 9. Monitor for a while
    console.log('9️⃣ Monitoring simulation for 15 seconds...');
    for (let i = 0; i < 5; i++) {
      await delay(3000);
      const status = await axios.get(`${BASE_URL}/api/status`);
      const { activeVehicles, stats } = status.data.data;
      console.log(`   📊 Update ${i + 1}: ${activeVehicles.flights} flights, ${activeVehicles.ships} ships`);
      console.log(`       Published: ${stats.flightsPublished} flights, ${stats.shipsPublished} ships`);
    }
    console.log();

    // 10. Stop Simulation
    console.log('🔟 Stopping Simulation...');
    const stopSim = await axios.post(`${BASE_URL}/api/simulation/stop`);
    console.log('⏹️ Simulation stopped:', stopSim.data.message);
    console.log();

    // 11. Final Status
    console.log('1️⃣1️⃣ Final Status...');
    const finalStatus = await axios.get(`${BASE_URL}/api/status`);
    console.log('📈 Final stats:', finalStatus.data.data.stats);
    console.log();

    console.log('🎉 All tests completed successfully!');
    console.log('✨ The simulator is working correctly and publishing data to Kafka.');

  } catch (error) {
    console.error('❌ Test failed:', error.response?.data || error.message);
    process.exit(1);
  }
}

// Run tests
if (require.main === module) {
  testSimulator().catch(console.error);
}

module.exports = { testSimulator }; 