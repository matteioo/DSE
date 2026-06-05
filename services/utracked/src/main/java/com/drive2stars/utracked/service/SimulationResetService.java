package com.drive2stars.utracked.service;

import com.drive2stars.utracked.persistence.VehiclePositionEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class SimulationResetService {

  @Transactional
  public long clearPositions() {
    return VehiclePositionEntity.deleteAll();
  }
}
