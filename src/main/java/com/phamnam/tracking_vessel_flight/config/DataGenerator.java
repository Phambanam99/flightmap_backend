package com.phamnam.tracking_vessel_flight.config;

import com.phamnam.tracking_vessel_flight.dto.request.FlightRequest;
import com.phamnam.tracking_vessel_flight.dto.request.FlightTrackingRequest;
import com.phamnam.tracking_vessel_flight.models.Aircraft;
import com.phamnam.tracking_vessel_flight.service.FlightService;
import com.phamnam.tracking_vessel_flight.service.FlightTrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
public class DataGenerator implements CommandLineRunner {

    private final FlightService flightService;
    private final FlightTrackingService flightTrackingService;
    private final Random random = new Random();

    // Configuration
    private static final int FLIGHTS_TO_GENERATE = 10000;
    private static final int TRACKING_POINTS_TO_GENERATE = 20000;

    // Sample data arrays
    private final String[] manufacturers = { "Boeing", "Airbus", "Embraer", "Bombardier", "Cessna", "Gulfstream",
            "Dassault", "Lockheed Martin", "Sukhoi", "ATR" };
    private final String[] types = { "Commercial", "Cargo", "Private", "Military", "Charter" };
    private final String[] operators = { "Delta Airlines", "American Airlines", "United Airlines", "Lufthansa",
            "British Airways", "Air France", "Singapore Airlines", "Emirates", "Qatar Airways", "Cathay Pacific",
            "FedEx", "UPS", "DHL", "Air China", "KLM", "Turkish Airlines", "Qantas" };
    private final String[] countries = { "USA", "UK", "France", "Germany", "China", "Japan", "Australia", "Brazil",
            "Canada", "India", "Russia", "UAE", "Qatar", "Singapore", "Netherlands", "Italy", "Spain" };
    private final String[] engineTypes = { "Jet", "Turboprop", "Piston", "Electric", "Turbofan" };
    private final String[] transponderTypes = { "Mode-S", "ADS-B", "Mode-C", "Mode-A", "Mode-4" };
    private final String[] altitudeTypes = { "Cruise", "Climb", "Descent", "Ground", "Approach" };
    private final String[] speedTypes = { "Ground Speed", "Air Speed", "Mach", "Knots", "IAS" };

    @Override
    public void run(String... args) throws Exception {
        // Check if we already have data
//        if (flightService.getAll().size() > 100) {
//            System.out.println("Database already contains data. Skipping data generation.");
//            return;
//        }

        System.out.println("Generating " + FLIGHTS_TO_GENERATE + " flights...");
        List<Aircraft> savedAircrafts = generateFlights();

        System.out.println("Generating " + TRACKING_POINTS_TO_GENERATE + " tracking points...");
        generateTrackingPoints(savedAircrafts);

        System.out.println("Data generation complete!");
    }

    private List<Aircraft> generateFlights() {
        List<Aircraft> savedAircrafts = new ArrayList<>();

        for (int i = 0; i < FLIGHTS_TO_GENERATE; i++) {
            String manufacturer = manufacturers[random.nextInt(manufacturers.length)];
            String operator = operators[random.nextInt(operators.length)];

            FlightRequest flightRequest = FlightRequest.builder()
                    .hexident(generateHexident())
                    .register(generateRegistration())
                    .type(types[random.nextInt(types.length)])
                    .manufacture(manufacturer)
                    .constructorNumber(
                            String.format("%s%d", manufacturer.substring(0, 3).toUpperCase(), random.nextInt(10000)))
                    .operator(operator)
                    .operatorCode(generateOperatorCode(operator))
                    .engines(String.valueOf(random.nextInt(4) + 1))
                    .engineType(engineTypes[random.nextInt(engineTypes.length)])
                    .isMilitary(random.nextInt(10) < 1) // 10% are military
                    .country(countries[random.nextInt(countries.length)])
                    .transponderType(transponderTypes[random.nextInt(transponderTypes.length)])
                    .year(2000 + random.nextInt(24)) // Between 2000 and 2023
                    .source("Generated")
                    .itemType(random.nextInt(5) + 1)
                    .build();

            Aircraft savedAircraft = flightService.save(flightRequest, null);
            savedAircrafts.add(savedAircraft);

            if (i % 500 == 0) {
                System.out.println("Generated " + i + " flights");
            }
        }

        return savedAircrafts;
    }

    private void generateTrackingPoints(List<Aircraft> aircrafts) {
        // Create a counter to track progress
        AtomicInteger counter = new AtomicInteger(0);

        // On average, create 2 tracking points per flight
        for (int i = 0; i < TRACKING_POINTS_TO_GENERATE; i++) {
            Aircraft aircraft = aircrafts.get(random.nextInt(aircrafts.size()));

            // Create realistic route points
            double baseLat = 30 + random.nextDouble() * 30; // Between 30°N and 60°N
            double baseLon = -120 + random.nextDouble() * 140; // Between 120°W and 20°E

            // Create a tracking point
            LocalDateTime now = LocalDateTime.now().minusDays(random.nextInt(30));

            FlightTrackingRequest request = FlightTrackingRequest.builder()
                    .flightId(aircraft.getId())
                    .altitude(30000f + random.nextFloat() * 10000f) // Between 30,000 and 40,000 feet
                    .altitudeType(altitudeTypes[random.nextInt(altitudeTypes.length)])
                    .targetAlt(35000f + random.nextFloat() * 5000f)
                    .callsign(generateCallsign(aircraft))
                    .speed(400f + random.nextFloat() * 200f) // Between 400 and 600 knots
                    .speedType(speedTypes[random.nextInt(speedTypes.length)])
                    .verticalSpeed(random.nextFloat() * 1000f - 500f) // Between -500 and 500 feet/min
                    .squawk(1000 + random.nextInt(7000))
                    .distance(random.nextFloat() * 5000f) // Random distance traveled
                    .bearing(random.nextFloat() * 360f) // Random bearing
                    .unixTime(now.toEpochSecond(ZoneOffset.UTC))
                    .updateTime(now)
                    .longitude(baseLon)
                    .latitude(baseLat)
                    .landingUnixTimes(now.plusHours(random.nextInt(5) + 1).toEpochSecond(ZoneOffset.UTC))
                    .landingTimes(now.plusHours(random.nextInt(5) + 1))
                    .build();

            flightTrackingService.save(request, null);

            int count = counter.incrementAndGet();
            if (count % 1000 == 0) {
                System.out.println("Generated " + count + " tracking points");
            }
        }
    }

    // Helper methods to generate realistic-looking data
    private String generateHexident() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append(Integer.toHexString(random.nextInt(16)).toUpperCase());
        }
        return sb.toString();
    }

    private String generateRegistration() {
        String prefix = "N";
        if (random.nextInt(3) == 0) { // 1/3 chance of non-US registration
            String[] prefixes = { "G-", "F-", "D-", "JA", "B-", "VH-", "C-" };
            prefix = prefixes[random.nextInt(prefixes.length)];
        }

        StringBuilder sb = new StringBuilder(prefix);
        for (int i = 0; i < 5; i++) {
            sb.append((char) ('A' + random.nextInt(26)));
        }
        return sb.toString();
    }

    private String generateOperatorCode(String operator) {
        if (operator.contains(" ")) {
            String[] parts = operator.split(" ");
            StringBuilder code = new StringBuilder();
            for (String part : parts) {
                if (part.length() > 0) {
                    code.append(part.charAt(0));
                }
            }
            return code.toString().toUpperCase();
        }
        return operator.substring(0, Math.min(3, operator.length())).toUpperCase();
    }

    private String generateCallsign(Aircraft aircraft) {
        String code = aircraft.getOperatorCode();
        if (code == null || code.isEmpty()) {
            code = aircraft.getOperator().substring(0, Math.min(3, aircraft.getOperator().length()));
        }
        return code.toUpperCase() + random.nextInt(9000) + 1000;
    }
}