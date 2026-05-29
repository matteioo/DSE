package com.drive2stars.utracked;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;


/**
 * Example JPA entity defined as a Panache Entity.
 * An ID field of Long type is provided, if you want to define your own ID field extends <code>PanacheEntityBase</code> instead.
 * <br>
 * This uses the active record pattern, you can also use the repository pattern instead:
 * {@see https://quarkus.io/guides/hibernate-orm-panache#solution-2-using-the-repository-pattern}.
 * <br>
 * Usage:
 * <br>
 * {@code
 *     public void doSomething() {
 *         MyEntity entity1 = new MyEntity();
 *         entity1.field = "field-1";
 *         entity1.persist();
 *         List<MyEntity> entities = MyEntity.listAll();
 *     }
 * }
 */
@Entity
@Table(name = "vehicle_position")
public class VehiclePositionEntity extends PanacheEntity {

    public String vin;

    @Column(precision = 10, scale = 7)
    public BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    public BigDecimal longitude;
}
