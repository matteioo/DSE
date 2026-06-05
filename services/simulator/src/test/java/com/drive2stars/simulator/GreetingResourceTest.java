package com.drive2stars.simulator;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

@QuarkusTest
class GreetingResourceTest {
    @Test
    void testVehicleGpsEndpoint() {
        given()
          .when().get("/vehicles/D2S-DEMO-VIN-001/gps")
          .then()
             .statusCode(200)
             .body("vin", is("D2S-DEMO-VIN-001"))
             .body("latitude", notNullValue())
             .body("longitude", notNullValue());
    }

    @Test
    void testUnknownVehicleGpsEndpoint() {
        given()
          .when().get("/vehicles/UNKNOWN/gps")
          .then()
             .statusCode(404);
    }

    @Test
    void testVehicleSonarEndpoint() {
        given()
          .when().get("/vehicles/D2S-DEMO-VIN-001/sonar")
          .then()
             .statusCode(200)
             .body("size()", is(0));
    }

    @Test
    void testBackendSonarEndpoint() {
        given()
          .when().get("/sonar/readings")
          .then()
             .statusCode(200)
             .body("size()", is(0));
    }

    @Test
    void testUnknownVehicleSonarEndpoint() {
        given()
          .when().get("/vehicles/UNKNOWN/sonar")
          .then()
             .statusCode(404);
    }
}
