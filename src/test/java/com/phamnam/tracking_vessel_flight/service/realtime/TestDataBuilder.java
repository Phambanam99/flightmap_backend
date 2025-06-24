package com.phamnam.tracking_vessel_flight.service.realtime;

import com.phamnam.tracking_vessel_flight.dto.request.AircraftTrackingRequest;
import com.phamnam.tracking_vessel_flight.dto.request.VesselTrackingRequest;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Utility class for building test data objects
 */
public class TestDataBuilder {

    public static class AircraftTestDataBuilder {

        public static AircraftTrackingRequest createDefaultAircraft() {
            return AircraftTrackingRequest.builder()
                    .hexident("ABC123")
                    .latitude(40.7128)
                    .longitude(-74.0060)
                    .altitude(35000)
                    .groundSpeed(500)
                    .track(180)
                    .verticalRate(0)
                    .squawk("1200")
                    .aircraftType("B737")
                    .registration("N123AB")
                    .callsign("UAL123")
                    .origin("JFK")
                    .destination("LAX")
                    .flightNumber("UA123")
                    .onGround(false)
                    .timestamp(LocalDateTime.now())
                    .dataQuality(0.95)
                    .airline("United Airlines")
                    .route("JFK-LAX")
                    .emergency(false)
                    .flightStatus("En Route")
                    .heading(180)
                    .magneticHeading(175.0)
                    .trueAirspeed(480.0)
                    .windDirection(270)
                    .windSpeed(25)
                    .temperature(-45.0)
                    .transponderCode("1200")
                    .build();
        }

        public static AircraftTrackingRequest createAircraftWithId(String hexident) {
            return createDefaultAircraft().toBuilder()
                    .hexident(hexident)
                    .callsign("TEST" + hexident.substring(0, 3))
                    .build();
        }

        public static AircraftTrackingRequest createAircraftAtLocation(double lat, double lon) {
            return createDefaultAircraft().toBuilder()
                    .latitude(lat)
                    .longitude(lon)
                    .build();
        }

        public static List<AircraftTrackingRequest> createMultipleAircraft(int count) {
            return java.util.stream.IntStream.range(0, count)
                    .mapToObj(i -> createAircraftWithId("ABC" + String.format("%03d", i)))
                    .toList();
        }
    }

    public static class VesselTestDataBuilder {

        public static VesselTrackingRequest createDefaultVessel() {
            return VesselTrackingRequest.builder()
                    .mmsi("123456789")
                    .latitude(40.7128)
                    .longitude(-74.0060)
                    .speed(15.5)
                    .course(180)
                    .heading(180)
                    .navigationStatus("Under way using engine")
                    .vesselName("Container Ship Alpha")
                    .vesselType("Container Ship")
                    .imo("1234567")
                    .callsign("TEST123")
                    .flag("US")
                    .length(300)
                    .width(40)
                    .draught(12.5)
                    .destination("New York")
                    .eta("2024-12-31 14:30")
                    .timestamp(LocalDateTime.now())
                    .dataQuality(0.90)
                    .cargoType("Containers")
                    .deadweight(50000)
                    .grossTonnage(75000)
                    .buildYear("2020")
                    .portOfRegistry("New York")
                    .ownerOperator("Test Shipping Co.")
                    .vesselClass("Container")
                    .dangerousCargo(false)
                    .securityAlert(false)
                    .route("Asia-US East Coast")
                    .lastPort("Shanghai")
                    .nextPort("New York")
                    .build();
        }

        public static VesselTrackingRequest createVesselWithMmsi(String mmsi) {
            return createDefaultVessel().toBuilder()
                    .mmsi(mmsi)
                    .vesselName("Test Vessel " + mmsi)
                    .build();
        }

        public static VesselTrackingRequest createVesselAtLocation(double lat, double lon) {
            return createDefaultVessel().toBuilder()
                    .latitude(lat)
                    .longitude(lon)
                    .build();
        }

        public static List<VesselTrackingRequest> createMultipleVessels(int count) {
            return java.util.stream.IntStream.range(0, count)
                    .mapToObj(i -> createVesselWithMmsi(String.format("%09d", 123456000 + i)))
                    .toList();
        }
    }

    // Common test scenarios
    public static class TestScenarios {

        public static List<AircraftTrackingRequest> getFlightRadar24Data() {
            return Arrays.asList(
                    AircraftTestDataBuilder.createAircraftWithId("FR24001"),
                    AircraftTestDataBuilder.createAircraftWithId("FR24002"));
        }

        public static List<AircraftTrackingRequest> getAdsbExchangeData() {
            return Arrays.asList(
                    AircraftTestDataBuilder.createAircraftWithId("ADSB001"),
                    AircraftTestDataBuilder.createAircraftWithId("ADSB002"));
        }

        public static List<VesselTrackingRequest> getMarineTrafficData() {
            return Arrays.asList(
                    VesselTestDataBuilder.createVesselWithMmsi("111111111"),
                    VesselTestDataBuilder.createVesselWithMmsi("222222222"));
        }

        public static List<VesselTrackingRequest> getVesselFinderData() {
            return Arrays.asList(
                    VesselTestDataBuilder.createVesselWithMmsi("333333333"),
                    VesselTestDataBuilder.createVesselWithMmsi("444444444"));
        }

        public static List<VesselTrackingRequest> getChinaportsData() {
            return Arrays.asList(
                    VesselTestDataBuilder.createVesselWithMmsi("555555555"),
                    VesselTestDataBuilder.createVesselWithMmsi("666666666"));
        }

        public static List<VesselTrackingRequest> getMarineTrafficV2Data() {
            return Arrays.asList(
                    VesselTestDataBuilder.createVesselWithMmsi("777777777"),
                    VesselTestDataBuilder.createVesselWithMmsi("888888888"));
        }
    }
}