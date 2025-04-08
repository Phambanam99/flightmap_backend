package com.phamnam.tracking_vessel_flight.service.interfaces;

import com.phamnam.tracking_vessel_flight.models.auth.AuthenticationRequest;
import com.phamnam.tracking_vessel_flight.models.auth.AuthenticationResponse;
import com.phamnam.tracking_vessel_flight.models.auth.LogoutRequest;
import com.phamnam.tracking_vessel_flight.models.auth.RefreshTokenRequest;
import com.phamnam.tracking_vessel_flight.models.auth.RegisterRequest;

public interface IAuthenticationService {
    AuthenticationResponse authenticate(AuthenticationRequest request);

    AuthenticationResponse refreshToken(RefreshTokenRequest request);

    AuthenticationResponse register(RegisterRequest request);

    void logout(LogoutRequest request);
}
