package com.drive2stars.utracked.service;

import com.drive2stars.utracked.endpoint.VehicleGPSDto;
import com.drive2stars.utracked.persistence.VehiclePositionEntity;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class VehicleService {

  public List<VehicleGPSDto> getLatestPositions() {
    return VehiclePositionEntity.findLatestPositions().stream()
        .map(this::toDto)
        .toList();
  }

  public VehicleGPSDto getLatestPosition(String vin) {
    VehiclePositionEntity entity = VehiclePositionEntity.findLatestByVin(vin);
    return entity == null ? null : toDto(entity);
  }

  public List<VehicleGPSDto> getHistory(String vin, int limit) {
    return VehiclePositionEntity.findHistoryByVin(vin, limit).stream()
        .map(this::toDto)
        .toList();
  }

  private VehicleGPSDto toDto(VehiclePositionEntity entity) {
    VehicleGPSDto dto = new VehicleGPSDto();
    dto.vin = entity.vin;
    dto.latitude = entity.latitude;
    dto.longitude = entity.longitude;
    return dto;
  }
}
