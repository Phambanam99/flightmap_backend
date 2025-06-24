package com.phamnam.tracking_vessel_flight.service.rest.interfaces;

import com.phamnam.tracking_vessel_flight.dto.request.UserRequest;
import com.phamnam.tracking_vessel_flight.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IUserService {
    UserResponse createUser(UserRequest userRequest);

    UserResponse getUserById(Long id);

    List<UserResponse> getAllUsers();

    long count();

    Page<UserResponse> getAllPaginated(Pageable pageable);

    UserResponse updateUser(Long id, UserRequest userRequest);

    void deleteUser(Long id);
}
