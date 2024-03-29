package com.Parley.Backend.services;

import com.Parley.Backend.dto.LocationResponse;
import com.Parley.Backend.dto.SessionResponse;
import com.Parley.Backend.entities.Location;
import com.Parley.Backend.entities.Session;
import com.Parley.Backend.repositories.LocationRepository;
import com.Parley.Backend.repositories.SessionRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Random;

@Service
@AllArgsConstructor
@Slf4j
public class SessionService {

    private final SessionRepository sessionRepository;
    private final LocationRepository locationRepository;

    public SessionResponse createSession(Location location) {
        Random r = new Random();
        int newSessionCode;


        do {
            newSessionCode = r.nextInt(9999) + 1;
        }
        while (sessionRepository.findBySessionCodeAndStatus(newSessionCode, "Active").isPresent());

        Session createdSession =  new Session();

        createdSession.setSessionCode(newSessionCode);
        createdSession.setStatus("Active");

        Session savedSession = sessionRepository.save(createdSession);
        log.info("New Session was create with ID: " + savedSession.getSessionId());

        location.setSession(savedSession);
        locationRepository.save(location);
        log.info("New Location added: " + location.getLatitude() + ", " + location.getLongitude());

        return this.mapToSessionResponse(savedSession);

    }


    public Session getSession(Long session_id) {

        return sessionRepository.findById(session_id).get();
    }

    public List<Location> getAllLocations() {
        return locationRepository.findAll();
    }

    public SessionResponse joinSession(int sessionCode, Location location){

        Session joinSession = sessionRepository.findBySessionCodeAndStatus(sessionCode, "Active").get();

        location.setSession(joinSession);

        locationRepository.save(location);

        return this.mapToSessionResponse(joinSession);
    }


    public LocationResponse getCenterLocation(Long sessionId) {

        Session currentSession = sessionRepository.findById(sessionId).get();

        BigDecimal avgLatitude = BigDecimal.valueOf(0.0);
        BigDecimal avgLongitude = BigDecimal.valueOf(0.0);

        for (Location l : currentSession.getLocations() ) {

            avgLatitude = avgLatitude.add(l.getLatitude());
            avgLongitude = avgLongitude.add(l.getLongitude());
        }

        avgLatitude = avgLatitude.divide(BigDecimal.valueOf(currentSession.getLocations().size()), 10, RoundingMode.HALF_EVEN);
        avgLongitude = avgLongitude.divide(BigDecimal.valueOf(currentSession.getLocations().size()), 10, RoundingMode.HALF_EVEN);


        return LocationResponse.builder()
                .latitude(avgLatitude)
                .longitude(avgLongitude)
                .build();

    }

    public SessionResponse startSession(Long sessionId) {
        Session currentSession = sessionRepository.findById(sessionId).get();

        currentSession.setStatus("In Progress");

        BigDecimal avgLatitude = BigDecimal.valueOf(0.0);
        BigDecimal avgLongitude = BigDecimal.valueOf(0.0);

        for (Location l : currentSession.getLocations() ) {

            avgLatitude = avgLatitude.add(l.getLatitude());
            avgLongitude = avgLongitude.add(l.getLongitude());
        }

        avgLatitude = avgLatitude.divide(BigDecimal.valueOf(currentSession.getLocations().size()), 10, RoundingMode.HALF_EVEN);
        avgLongitude = avgLongitude.divide(BigDecimal.valueOf(currentSession.getLocations().size()), 10, RoundingMode.HALF_EVEN);

        Location centerLocation = new Location();
        centerLocation.setLatitude(avgLatitude);
        centerLocation.setLongitude(avgLongitude);

        Location savedCenterLocation = locationRepository.save(centerLocation);
        currentSession.setCenterLocation(savedCenterLocation);

        Session savedStartedSession = sessionRepository.save(currentSession);

        return this.mapToSessionResponse(savedStartedSession);



    }


    private SessionResponse mapToSessionResponse(Session session){
        return SessionResponse.builder()
                .sessionId(session.getSessionId())
                .sessionCode(session.getSessionCode())
                .status(session.getStatus())
                .build();
    }

}
