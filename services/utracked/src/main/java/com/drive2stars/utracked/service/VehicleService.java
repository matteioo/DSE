package com.drive2stars.utracked.service;

import com.drive2stars.utracked.endpoint.VehicleGPSDto;
import com.drive2stars.utracked.persistence.VehiclePositionEntity;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class VehicleService {

  public VehicleGPSDto getLatestPosition(String vin) {
    VehiclePositionEntity entity = VehiclePositionEntity.findLatestByVin(vin);
    if (entity == null) {
      return null;
    }
    VehicleGPSDto dto = new VehicleGPSDto();
    dto.vin = entity.vin;
    dto.latitude = entity.latitude;
    dto.longitude = entity.longitude;
    return dto;
  }
}
