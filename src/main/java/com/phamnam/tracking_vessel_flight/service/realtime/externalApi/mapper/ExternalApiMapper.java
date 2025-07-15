package com.phamnam.tracking_vessel_flight.service.realtime.externalApi.mapper;

import com.phamnam.tracking_vessel_flight.dto.request.AircraftTrackingRequest;
import com.phamnam.tracking_vessel_flight.dto.request.VesselTrackingRequest;
import com.phamnam.tracking_vessel_flight.dto.response.external.AdsbExchangeAircraftData;
import com.phamnam.tracking_vessel_flight.dto.response.external.ChinaportsVesselData;
import com.phamnam.tracking_vessel_flight.dto.response.external.FlightRadar24AircraftData;
import com.phamnam.tracking_vessel_flight.dto.response.external.MarineTrafficV2VesselData;
import com.phamnam.tracking_vessel_flight.dto.response.external.MarineTrafficVesselData;
import com.phamnam.tracking_vessel_flight.dto.response.external.VesselFinderVesselData;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Mapper to convert external API responses to internal DTOs
 */
@Component
public class ExternalApiMapper {

    /**
     * Convert Chinaports vessel data to VesselTrackingRequest
     */
    public VesselTrackingRequest fromChinaports(ChinaportsVesselData data) {
        if (data == null) {
            return null;
        }

        return VesselTrackingRequest.builder()
                .mmsi(data.getMmsi())
                .latitude(data.getLat())
                .longitude(data.getLon())
                .speed(data.getSpeed())
                .course(data.getCourse())
                .heading(data.getHeading())
                .navigationStatus(data.getNavStatus())
                .vesselName(data.getVesselName())
                .vesselType(data.getVesselType())
                .imo(data.getImo())
                .callsign(data.getCallsign())
                .flag(data.getFlag() != null ? data.getFlag() : "CN") // Default to China
                .length(data.getLength())
                .width(data.getWidth())
                .draught(data.getDraught())
                .destination(data.getDestination())
                .eta(data.getEta())
                .lastPort(data.getLastPort())
                .nextPort(data.getNextPort())
                .timestamp(LocalDateTime.now())
                .dataQuality(0.85) // Chinaports generally has good quality data for Chinese waters
                .source("CHINAPORTS")
                .build();
    }

    /**
     * Convert MarineTraffic V2 vessel data to VesselTrackingRequest
     */
    public VesselTrackingRequest fromMarineTrafficV2(MarineTrafficV2VesselData data) {
        if (data == null) {
            return null;
        }

        return VesselTrackingRequest.builder()
                .mmsi(data.getMmsi())
                .latitude(data.getLat())
                .longitude(data.getLon())
                .speed(data.getSpeed())
                .course(data.getCourse())
                .heading(data.getHeading())
                .navigationStatus(data.getStatus()) // MarineTraffic uses "status" field
                .vesselName(data.getShipname()) // MarineTraffic uses "shipname"
                .vesselType(data.getShiptype()) // MarineTraffic uses "shiptype"
                .imo(data.getImo())
                .callsign(data.getCallsign())
                .flag(data.getFlag())
                .length(data.getLength())
                .width(data.getWidth())
                .draught(data.getDraught())
                .destination(data.getDestination())
                .eta(data.getEta())
                .lastPort(data.getLastport()) // MarineTraffic uses "lastport"
                .nextPort(data.getNextport()) // MarineTraffic uses "nextport"
                .timestamp(LocalDateTime.now())
                .dataQuality(0.90) // MarineTraffic generally has high quality data
                .source("MARINETRAFFIC_V2")
                .build();
    }

    /**
     * Convert MarineTraffic vessel data to VesselTrackingRequest
     */
    public VesselTrackingRequest fromMarineTraffic(MarineTrafficVesselData data) {
        if (data == null) {
            return null;
        }

        return VesselTrackingRequest.builder()
                .mmsi(data.getMmsi())
                .latitude(data.getLat())
                .longitude(data.getLon())
                .speed(data.getSpeed())
                .course(data.getCourse())
                .heading(data.getHeading())
                .navigationStatus(data.getStatus())
                .vesselName(data.getShipname())
                .vesselType(data.getShiptype())
                .imo(data.getImo())
                .callsign(data.getCallsign())
                .flag(data.getFlag())
                .length(data.getLength())
                .width(data.getWidth())
                .draught(data.getDraught())
                .destination(data.getDestination())
                .eta(data.getEta())
                .lastPort(data.getLastport())
                .nextPort(data.getNextport())
                .timestamp(LocalDateTime.now())
                .dataQuality(0.92) // MarineTraffic main API has high quality data
                .source("MARINETRAFFIC")
                .build();
    }

    /**
     * Convert VesselFinder vessel data to VesselTrackingRequest
     */
    public VesselTrackingRequest fromVesselFinder(VesselFinderVesselData data) {
        if (data == null) {
            return null;
        }

        return VesselTrackingRequest.builder()
                .mmsi(data.getActualMmsi())
                .latitude(data.getActualLatitude())
                .longitude(data.getActualLongitude())
                .speed(data.getActualSpeed())
                .course(data.getActualCourse())
                .heading(data.getActualHeading())
                .navigationStatus(data.getActualNavStatus())
                .vesselName(data.getActualVesselName())
                .vesselType(data.getActualVesselType())
                .imo(data.getActualImo())
                .callsign(data.getActualCallsign())
                .flag(data.getActualFlag())
                .length(data.getActualLength())
                .width(data.getActualWidth())
                .draught(data.getActualDraught())
                .destination(data.getActualDestination())
                .eta(data.getActualEta())
                .lastPort(data.getLastPort())
                .nextPort(data.getNextPort())
                .timestamp(LocalDateTime.now())
                .dataQuality(0.88) // VesselFinder has good quality data
                .source("VESSELFINDER")
                .build();
    }

    /**
     * Convert ADS-B Exchange aircraft data to AircraftTrackingRequest
     */
    public AircraftTrackingRequest fromAdsbExchange(AdsbExchangeAircraftData data) {
        if (data == null) {
            return null;
        }

        return AircraftTrackingRequest.builder()
                .hexident(data.getHex()) // ADS-B Exchange uses "hex" for ICAO hex identifier
                .callsign(data.getFlight() != null ? data.getFlight().trim() : null)
                .latitude(data.getLat())
                .longitude(data.getLon())
                .altitude(data.getAltBaro() != null ? data.getAltBaro() : data.getAltGeom())
                .groundSpeed(data.getGs() != null ? data.getGs().intValue() : null)
                .track(data.getTrack() != null ? data.getTrack().intValue() : null)
                .heading(data.getTrueHeading() != null ? data.getTrueHeading().intValue() : null)
                .magneticHeading(data.getMagHeading())
                .verticalRate(data.getBaroRate() != null ? data.getBaroRate() : data.getGeomRate())
                .squawk(data.getSquawk())
                .transponderCode(data.getSquawk())
                .emergency(data.getEmergency() != null)
                .onGround(data.getAltBaro() != null && data.getAltBaro() <= 50) // Simple ground detection
                .aircraftType(data.getType())
                .timestamp(LocalDateTime.now())
                .dataQuality(0.95) // ADS-B Exchange has very high quality data
                .source("ADSB_EXCHANGE")
                .build();
    }

    /**
     * Convert FlightRadar24 aircraft data to AircraftTrackingRequest
     */
    public AircraftTrackingRequest fromFlightRadar24(String hexIdent, FlightRadar24AircraftData data) {
        if (data == null) {
            return null;
        }

        String squawk = data.getSquawk();
        boolean isEmergency = squawk != null &&
                ("7500".equals(squawk) || "7600".equals(squawk) || "7700".equals(squawk));

        return AircraftTrackingRequest.builder()
                .hexident(hexIdent)
                .latitude(data.getLatitude())
                .longitude(data.getLongitude())
                .track(data.getHeading())
                .altitude(data.getAltitude())
                .groundSpeed(data.getSpeed())
                .squawk(squawk)
                .aircraftType(data.getAircraftType())
                .registration(data.getRegistration())
                .callsign(data.getCallsign())
                .onGround(data.getOnGround())
                .verticalRate(data.getVerticalRate())
                .emergency(isEmergency)
                .timestamp(LocalDateTime.now())
                .dataQuality(0.80) // FlightRadar24 has good data quality
                .origin(data.getOrigin())
                .destination(data.getDestination())
                .source("FLIGHTRADAR24")
                .build();
    }
}
