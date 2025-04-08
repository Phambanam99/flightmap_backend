package com.phamnam.tracking_vessel_flight.service;

import com.phamnam.tracking_vessel_flight.dto.request.VesselRequest;
import com.phamnam.tracking_vessel_flight.exception.ResourceNotFoundException;
import com.phamnam.tracking_vessel_flight.models.User;
import com.phamnam.tracking_vessel_flight.models.Vessel;
import com.phamnam.tracking_vessel_flight.repository.UserRepository;
import com.phamnam.tracking_vessel_flight.repository.VesselRepository;
import com.phamnam.tracking_vessel_flight.service.interfaces.IVesselService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class VesselService implements IVesselService {
    @Autowired
    private VesselRepository vesselRepository;

    @Autowired
    private UserRepository userRepository;

    public List<Vessel> getAll() {
        List<Vessel> vessels = new ArrayList<>();
        String[] types = { "Cargo", "Tanker", "Container", "Passenger", "Fishing" };
        String[] owners = { "Maersk", "MSC", "CMA CGM", "COSCO", "Evergreen" };

        for (int i = 0; i < 50; i++) {
            Vessel vessel = new Vessel();
            vessel.setId((long) (i + 1));
            vessel.setName("Vessel-" + String.format("%03d", i));
            vessel.setType(types[i % types.length]);
            vessel.setRegistrationNumber("REG-" + String.format("%05d", i));
            vessel.setOwner(owners[i % owners.length]);
            vessels.add(vessel);
        }
        return vessels;
        // return vesselRepository.findAll();
    }

    public Page<Vessel> getAllPaginated(Pageable pageable) {
        return vesselRepository.findAll(pageable);
    }

    public Vessel getVesselById(Long id) {
        return vesselRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vessel", "id", id));
    }

    public Vessel save(VesselRequest vesselRequest, Long userId) {
        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        }

        Vessel vessel = new Vessel();
        vessel.setName(vesselRequest.getName());
        vessel.setType(vesselRequest.getType());
        vessel.setRegistrationNumber(vesselRequest.getRegistrationNumber());
        vessel.setOwner(vesselRequest.getOwner());
        vessel.setUpdatedBy(user);

        return vesselRepository.save(vessel);
    }

    public void deleteVessel(Long id) {
        Vessel vessel = getVesselById(id);
        vesselRepository.delete(vessel);
    }

    public Vessel updateVessel(Long id, VesselRequest vesselRequest, Long userId) {
        Vessel vessel = getVesselById(id);

        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        }

        vessel.setName(vesselRequest.getName());
        vessel.setType(vesselRequest.getType());
        vessel.setRegistrationNumber(vesselRequest.getRegistrationNumber());
        vessel.setOwner(vesselRequest.getOwner());
        vessel.setUpdatedBy(user);

        return vesselRepository.save(vessel);
    }
}
