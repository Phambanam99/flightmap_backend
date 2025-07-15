const express = require('express');
const cors = require('cors');
const moment = require('moment');

const app = express();
const PORT = 3001;

// Enable CORS for all routes
app.use(cors());
app.use(express.json());

// Flight data for 5 continuous flights
const flights = [
    {
        id: 'VN123',
        hexident: 'ABC123',
        callsign: 'VN123',
        aircraftType: 'A320',
        registration: 'VN-A123',
        airline: 'Vietnam Airlines',
        origin: 'SGN',
        destination: 'HAN',
        flightNumber: 'VN123',
        route: 'SGN-HAN',
        // Initial position near Ho Chi Minh City
        baseLatitude: 10.8231,
        baseLongitude: 106.6297,
        targetLatitude: 21.0285, // Hanoi
        targetLongitude: 105.8542,
        altitude: 35000,
        groundSpeed: 480,
        currentStep: 0,
        totalSteps: 200
    },
    {
        id: 'VJ456',
        hexident: 'DEF456',
        callsign: 'VJ456',
        aircraftType: 'A321',
        registration: 'VN-A456',
        airline: 'VietJet Air',
        origin: 'DAD',
        destination: 'SGN',
        flightNumber: 'VJ456',
        route: 'DAD-SGN',
        // Initial position near Da Nang
        baseLatitude: 16.0544,
        baseLongitude: 108.2022,
        targetLatitude: 10.8231, // Ho Chi Minh City
        targetLongitude: 106.6297,
        altitude: 33000,
        groundSpeed: 450,
        currentStep: 0,
        totalSteps: 180
    },
    {
        id: 'QH789',
        hexident: 'GHI789',
        callsign: 'QH789',
        aircraftType: 'ATR72',
        registration: 'VN-B789',
        airline: 'Bamboo Airways',
        origin: 'HAN',
        destination: 'PQC',
        flightNumber: 'QH789',
        route: 'HAN-PQC',
        // Initial position near Hanoi
        baseLatitude: 21.0285,
        baseLongitude: 105.8542,
        targetLatitude: 10.2272, // Phu Quoc
        targetLongitude: 103.9670,
        altitude: 25000,
        groundSpeed: 320,
        currentStep: 0,
        totalSteps: 220
    },
    {
        id: 'DL123',
        hexident: 'JKL123',
        callsign: 'DL123',
        aircraftType: 'B777',
        registration: 'N123DL',
        airline: 'Delta Air Lines',
        origin: 'NRT',
        destination: 'SGN',
        flightNumber: 'DL123',
        route: 'NRT-SGN',
        // Initial position near Tokyo
        baseLatitude: 35.7720,
        baseLongitude: 140.3929,
        targetLatitude: 10.8231, // Ho Chi Minh City
        targetLongitude: 106.6297,
        altitude: 42000,
        groundSpeed: 520,
        currentStep: 0,
        totalSteps: 300
    },
    {
        id: 'SQ999',
        hexident: 'MNO999',
        callsign: 'SQ999',
        aircraftType: 'A350',
        registration: '9V-SMF',
        airline: 'Singapore Airlines',
        origin: 'SIN',
        destination: 'HAN',
        flightNumber: 'SQ999',
        route: 'SIN-HAN',
        // Initial position near Singapore
        baseLatitude: 1.3521,
        baseLongitude: 103.8198,
        targetLatitude: 21.0285, // Hanoi
        targetLongitude: 105.8542,
        altitude: 39000,
        groundSpeed: 500,
        currentStep: 0,
        totalSteps: 250
    }
];

// Vessel data for 5 ships
const vessels = [
    {
        id: 'EVERGREEN001',
        mmsi: '123456789',
        vesselName: 'EVER GIVEN VIETNAM',
        vesselType: 'Container Ship',
        imo: '9811000123',
        callsign: 'VRNG1',
        flag: 'VN',
        // Initial position near Ho Chi Minh City port
        baseLatitude: 10.7769,
        baseLongitude: 106.7009,
        targetLatitude: 1.2966, // Singapore
        targetLongitude: 103.7764,
        length: 400,
        width: 59,
        draught: 14.5,
        destination: 'SINGAPORE',
        currentStep: 0,
        totalSteps: 500,
        navigationStatus: 'Under way using engine'
    },
    {
        id: 'MAERSK002',
        mmsi: '234567890',
        vesselName: 'MAERSK SAIGON',
        vesselType: 'Container Ship',
        imo: '9811000234',
        callsign: 'VRNG2',
        flag: 'VN',
        // Initial position near Hai Phong port
        baseLatitude: 20.8525,
        baseLongitude: 106.6635,
        targetLatitude: 22.3193, // Hong Kong
        targetLongitude: 114.1694,
        length: 366,
        width: 51,
        draught: 13.2,
        destination: 'HONG KONG',
        currentStep: 0,
        totalSteps: 400,
        navigationStatus: 'Under way using engine'
    },
    {
        id: 'COSCO003',
        mmsi: '345678901',
        vesselName: 'COSCO VIETNAM',
        vesselType: 'Bulk Carrier',
        imo: '9811000345',
        callsign: 'VRNG3',
        flag: 'VN',
        // Initial position near Da Nang port
        baseLatitude: 16.0583,
        baseLongitude: 108.2372,
        targetLatitude: 31.2304, // Shanghai
        targetLongitude: 121.4737,
        length: 300,
        width: 45,
        draught: 12.8,
        destination: 'SHANGHAI',
        currentStep: 0,
        totalSteps: 600,
        navigationStatus: 'Under way using engine'
    },
    {
        id: 'HAPAG004',
        mmsi: '456789012',
        vesselName: 'HAPAG MEKONG',
        vesselType: 'Container Ship',
        imo: '9811000456',
        callsign: 'VRNG4',
        flag: 'VN',
        // Initial position near Can Tho
        baseLatitude: 10.0452,
        baseLongitude: 105.7469,
        targetLatitude: 13.7563, // Bangkok
        targetLongitude: 100.5018,
        length: 335,
        width: 48,
        draught: 11.5,
        destination: 'BANGKOK',
        currentStep: 0,
        totalSteps: 350,
        navigationStatus: 'Under way using engine'
    },
    {
        id: 'YANGMING005',
        mmsi: '567890123',
        vesselName: 'YANG MING VIETNAM',
        vesselType: 'Container Ship',
        imo: '9811000567',
        callsign: 'VRNG5',
        flag: 'VN',
        // Initial position near Quy Nhon port
        baseLatitude: 13.7830,
        baseLongitude: 109.2240,
        targetLatitude: 14.5995, // Manila
        targetLongitude: 120.9842,
        length: 320,
        width: 46,
        draught: 12.0,
        destination: 'MANILA',
        currentStep: 0,
        totalSteps: 450,
        navigationStatus: 'Under way using engine'
    }
];

// Helper function to interpolate between two points
function interpolatePosition(start, end, step, totalSteps) {
    const ratio = step / totalSteps;
    return {
        latitude: start.latitude + (end.latitude - start.latitude) * ratio,
        longitude: start.longitude + (end.longitude - start.longitude) * ratio
    };
}

// Helper function to add realistic variance
function addVariance(value, variance = 0.002) {
    return value + (Math.random() - 0.5) * 2 * variance;
}

// Update flight positions continuously
function updateFlightPositions() {
    flights.forEach(flight => {
        // Calculate current position based on step
        const position = interpolatePosition(
            { latitude: flight.baseLatitude, longitude: flight.baseLongitude },
            { latitude: flight.targetLatitude, longitude: flight.targetLongitude },
            flight.currentStep,
            flight.totalSteps
        );

        // Add realistic variance
        flight.currentLatitude = addVariance(position.latitude);
        flight.currentLongitude = addVariance(position.longitude);

        // Update other dynamic properties
        flight.currentAltitude = flight.altitude + Math.floor((Math.random() - 0.5) * 2000);
        flight.currentGroundSpeed = flight.groundSpeed + Math.floor((Math.random() - 0.5) * 40);
        flight.track = Math.floor(Math.random() * 360);
        flight.heading = flight.track + Math.floor((Math.random() - 0.5) * 10);
        flight.verticalRate = Math.floor((Math.random() - 0.5) * 1000);

        // Move to next step (loop back when reaching destination)
        flight.currentStep = (flight.currentStep + 1) % flight.totalSteps;
    });
}

// Update vessel positions continuously
function updateVesselPositions() {
    vessels.forEach(vessel => {
        // Calculate current position based on step
        const position = interpolatePosition(
            { latitude: vessel.baseLatitude, longitude: vessel.baseLongitude },
            { latitude: vessel.targetLatitude, longitude: vessel.targetLongitude },
            vessel.currentStep,
            vessel.totalSteps
        );

        // Add realistic variance (smaller for vessels)
        vessel.currentLatitude = addVariance(position.latitude, 0.001);
        vessel.currentLongitude = addVariance(position.longitude, 0.001);

        // Update other dynamic properties
        vessel.speed = 12 + Math.random() * 8; // 12-20 knots
        vessel.course = Math.floor(Math.random() * 360);
        vessel.heading = vessel.course + Math.floor((Math.random() - 0.5) * 20);

        // Move to next step (loop back when reaching destination)
        vessel.currentStep = (vessel.currentStep + 1) % vessel.totalSteps;
    });
}

// Start continuous updates
setInterval(updateFlightPositions, 3000); // Update every 3 seconds
setInterval(updateVesselPositions, 3000); // Update every 3 seconds

// Initialize positions
updateFlightPositions();
updateVesselPositions();

// Mock API endpoints for FlightRadar24
app.get('/api/mock/flightradar24', (req, res) => {
    console.log(`🛩️  FlightRadar24 API called at ${moment().format('YYYY-MM-DD HH:mm:ss')}`);
    
    const aircraftData = flights.map(flight => [
        flight.hexident,
        flight.currentLatitude,
        flight.currentLongitude,
        flight.track,
        flight.currentAltitude,
        flight.currentGroundSpeed,
        flight.squawk || '7000',
        flight.registration,
        flight.aircraftType,
        flight.callsign,
        flight.origin,
        flight.destination,
        flight.flightNumber,
        flight.onGround || false,
        flight.verticalRate || 0,
        flight.airline
    ]);

    res.json(aircraftData);
});

// Mock API endpoints for ADS-B Exchange
app.get('/api/mock/adsbexchange', (req, res) => {
    console.log(`📡 ADS-B Exchange API called at ${moment().format('YYYY-MM-DD HH:mm:ss')}`);
    
    const aircraftData = {
        aircraft: flights.map(flight => ({
            hex: flight.hexident,
            flight: flight.callsign,
            r: flight.registration,
            t: flight.aircraftType,
            lat: flight.currentLatitude,
            lon: flight.currentLongitude,
            alt_baro: flight.currentAltitude,
            gs: flight.currentGroundSpeed,
            track: flight.track,
            baro_rate: flight.verticalRate,
            squawk: flight.squawk || '7000',
            emergency: 'none',
            category: 'A3',
            nav_qnh: 1013.25,
            nav_altitude_mcp: flight.currentAltitude,
            nav_heading: flight.heading,
            nic: 8,
            rc: 186,
            seen_pos: 1.2,
            version: 2,
            nic_baro: 1,
            nac_p: 10,
            nac_v: 2,
            sil: 3,
            sil_type: 'perhour',
            gva: 2,
            sda: 2,
            alert: 0,
            spi: 0
        }))
    };

    res.json(aircraftData);
});

// Mock API endpoints for MarineTraffic
app.get('/api/mock/marinetraffic', (req, res) => {
    console.log(`🚢 MarineTraffic API called at ${moment().format('YYYY-MM-DD HH:mm:ss')}`);
    
    const vesselData = vessels.map(vessel => ({
        MMSI: vessel.mmsi,
        LAT: vessel.currentLatitude,
        LON: vessel.currentLongitude,
        SPEED: vessel.speed,
        COURSE: vessel.course,
        HEADING: vessel.heading,
        STATUS: vessel.navigationStatus,
        SHIPNAME: vessel.vesselName,
        SHIPTYPE: vessel.vesselType,
        IMO: vessel.imo,
        CALLSIGN: vessel.callsign,
        FLAG: vessel.flag,
        LENGTH: vessel.length,
        WIDTH: vessel.width,
        DRAUGHT: vessel.draught,
        DESTINATION: vessel.destination,
        ETA: moment().add(Math.floor(Math.random() * 5) + 1, 'days').format('MM-DD HH:mm')
    }));

    res.json(vesselData);
});

// Mock API endpoints for VesselFinder
app.get('/api/mock/vesselfinder', (req, res) => {
    console.log(`🚢 VesselFinder API called at ${moment().format('YYYY-MM-DD HH:mm:ss')}`);
    
    const vesselData = {
        vessels: vessels.map(vessel => ({
            mmsi: vessel.mmsi,
            lat: vessel.currentLatitude,
            lng: vessel.currentLongitude,
            sog: vessel.speed,
            cog: vessel.course,
            rot: (Math.random() - 0.5) * 10,
            heading: vessel.heading,
            navstat: vessel.navigationStatus,
            imo: vessel.imo,
            name: vessel.vesselName,
            callsign: vessel.callsign,
            type: vessel.vesselType,
            a: vessel.length,
            b: 20,
            c: vessel.width / 2,
            d: vessel.width / 2,
            draught: vessel.draught,
            dest: vessel.destination,
            eta: moment().add(Math.floor(Math.random() * 5) + 1, 'days').unix(),
            country: 'Vietnam'
        }))
    };

    res.json(vesselData);
});

// Mock API endpoints for Chinaports
app.get('/api/mock/chinaports', (req, res) => {
    console.log(`🚢 Chinaports API called at ${moment().format('YYYY-MM-DD HH:mm:ss')}`);
    
    // Return subset of vessels (simulate Chinese ports data)
    const chineseVessels = vessels.slice(0, 2).map(vessel => ({
        mmsi: vessel.mmsi,
        latitude: vessel.currentLatitude,
        longitude: vessel.currentLongitude,
        speed: vessel.speed,
        course: vessel.course,
        heading: vessel.heading,
        navStatus: vessel.navigationStatus,
        vesselName: vessel.vesselName,
        vesselType: vessel.vesselType,
        imo: vessel.imo,
        callsign: vessel.callsign,
        flag: vessel.flag,
        length: vessel.length,
        width: vessel.width,
        draft: vessel.draught,
        destination: vessel.destination,
        eta: moment().add(Math.floor(Math.random() * 3) + 1, 'days').format('YYYY-MM-DD HH:mm:ss')
    }));

    res.json({ data: chineseVessels });
});

// Mock API endpoints for MarineTraffic V2
app.get('/api/mock/marinetrafficv2', (req, res) => {
    console.log(`🚢 MarineTraffic V2 API called at ${moment().format('YYYY-MM-DD HH:mm:ss')}`);
    
    const vesselData = {
        data: vessels.map(vessel => ({
            mmsi: vessel.mmsi,
            lat: vessel.currentLatitude,
            lon: vessel.currentLongitude,
            speed: vessel.speed,
            course: vessel.course,
            heading: vessel.heading,
            navStatus: vessel.navigationStatus,
            shipName: vessel.vesselName,
            shipType: vessel.vesselType,
            imo: vessel.imo,
            callSign: vessel.callsign,
            flag: vessel.flag,
            length: vessel.length,
            width: vessel.width,
            draught: vessel.draught,
            destination: vessel.destination,
            eta: moment().add(Math.floor(Math.random() * 4) + 1, 'days').format('YYYY-MM-DD HH:mm'),
            lastUpdate: moment().format('YYYY-MM-DD HH:mm:ss')
        }))
    };

    res.json(vesselData);
});

// Health check endpoint
app.get('/health', (req, res) => {
    res.json({
        status: 'OK',
        timestamp: moment().format('YYYY-MM-DD HH:mm:ss'),
        message: 'Flight and Vessel Simulator is running',
        activeFlights: flights.length,
        activeVessels: vessels.length
    });
});

// API status endpoint
app.get('/api/status', (req, res) => {
    res.json({
        flightRadar24: { status: 'active', flights: flights.length },
        adsbExchange: { status: 'active', flights: flights.length },
        marineTraffic: { status: 'active', vessels: vessels.length },
        vesselFinder: { status: 'active', vessels: vessels.length },
        chinaports: { status: 'active', vessels: Math.min(2, vessels.length) },
        marineTrafficV2: { status: 'active', vessels: vessels.length },
        lastUpdate: moment().format('YYYY-MM-DD HH:mm:ss')
    });
});

// Start server
app.listen(PORT, () => {
    console.log(`🚀 Flight & Vessel Simulator Server is running on port ${PORT}`);
    console.log(`📊 Simulating ${flights.length} flights and ${vessels.length} vessels`);
    console.log(`🔄 Data updates every 3 seconds`);
    console.log(`📡 Available endpoints:`);
    console.log(`   - FlightRadar24: http://localhost:${PORT}/api/mock/flightradar24`);
    console.log(`   - ADS-B Exchange: http://localhost:${PORT}/api/mock/adsbexchange`);
    console.log(`   - MarineTraffic: http://localhost:${PORT}/api/mock/marinetraffic`);
    console.log(`   - VesselFinder: http://localhost:${PORT}/api/mock/vesselfinder`);
    console.log(`   - Chinaports: http://localhost:${PORT}/api/mock/chinaports`);
    console.log(`   - MarineTraffic V2: http://localhost:${PORT}/api/mock/marinetrafficv2`);
    console.log(`   - Health Check: http://localhost:${PORT}/health`);
    console.log(`   - API Status: http://localhost:${PORT}/api/status`);
});
