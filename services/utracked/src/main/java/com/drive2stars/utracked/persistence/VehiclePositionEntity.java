package com.drive2stars.utracked.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "vehicle_position", indexes = @Index(name = "idx_vehicle_position_vin", columnList = "vin"))
public class VehiclePositionEntity extends PanacheEntity {

  @Column(nullable = false)
  public String vin;

  @Column(nullable = false, precision = 10, scale = 7)
  public BigDecimal latitude;

  @Column(nullable = false, precision = 10, scale = 7)
  public BigDecimal longitude;

  public static VehiclePositionEntity findLatestByVin(String vin) {
    return find("vin = ?1 order by id desc", vin).firstResult();
  }
}
