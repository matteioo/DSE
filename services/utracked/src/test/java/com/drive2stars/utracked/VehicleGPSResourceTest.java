package com.drive2stars.utracked;

import com.drive2stars.utracked.persistence.VehiclePositionEntity;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.UserTransaction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

@QuarkusTest
class VehicleGPSResourceTest {

    @Inject
    UserTransaction tx;

    @BeforeEach
    void insertTestData() throws Exception {
        tx.begin();
        VehiclePositionEntity.deleteAll();

        persist("VIN001", "48.2082000", "16.3738000");
        persist("VIN001", "48.2090000", "16.3750000"); // newer entry for VIN001
        persist("VIN002", "48.2100000", "16.3760000");

        tx.commit();
    }

    @AfterEach
    void cleanup() throws Exception {
        tx.begin();
        VehiclePositionEntity.deleteAll();
        tx.commit();
    }

    @Test
    void getLatestPositions_returnsOneEntryPerVin() {
        given()
            .when().get("/positions")
            .then()
            .statusCode(200)
            .body("$.size()", is(2));
    }

    @Test
    void getLatestPosition_knownVin_returnsLatestEntry() {
        given()
            .when().get("/positions/VIN001")
            .then()
            .statusCode(200)
            .body("vin", is("VIN001"))
            .body("latitude", is(48.2090000f));
    }

    @Test
    void getLatestPosition_unknownVin_returns404() {
        given()
            .when().get("/positions/UNKNOWN")
            .then()
            .statusCode(404);
    }

    @Test
    void getHistory_returnsAllEntriesForVin() {
        given()
            .when().get("/positions/VIN001/history")
            .then()
            .statusCode(200)
            .body("$.size()", is(2));
    }

    @Test
    void getHistory_withLimit_respectsLimit() {
        given()
            .queryParam("limit", 1)
            .when().get("/positions/VIN001/history")
            .then()
            .statusCode(200)
            .body("$.size()", is(1));
    }

    private void persist(String vin, String lat, String lon) {
        var e = new VehiclePositionEntity();
        e.vin = vin;
        e.latitude = new BigDecimal(lat);
        e.longitude = new BigDecimal(lon);
        e.persist();
    }
}
