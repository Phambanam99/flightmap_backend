package com.phamnam.tracking_vessel_flight.service.rest.interfaces;

import com.phamnam.tracking_vessel_flight.dto.request.UserRequest;
import com.phamnam.tracking_vessel_flight.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IUserService {
    User createUser(UserRequest userRequest);

    User getUserById(Long id);

    List<User> getAllUsers();

    long count();

    Page<User> getAllPaginated(Pageable pageable);

    User updateUser(Long id, UserRequest userRequest);

    void deleteUser(Long id);
}
