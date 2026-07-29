package io.github.hsummerhays.atlas.web;

import io.github.hsummerhays.atlas.domain.model.Carrier;
import io.github.hsummerhays.atlas.domain.model.Shipment;
import io.github.hsummerhays.atlas.service.ShipmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/shipments")
public class ShipmentController {

    private final ShipmentService shipmentService;

    public ShipmentController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    @PostMapping
    public ResponseEntity<Shipment> createShipment(@Valid @RequestBody Shipment shipment) {
        Shipment created = shipmentService.createShipment(shipment);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PostMapping("/{id}/book")
    public ResponseEntity<Shipment> bookShipment(@PathVariable Long id) {
        Shipment booked = shipmentService.bookShipment(id);
        return ResponseEntity.ok(booked);
    }

    @PostMapping("/rates")
    public ResponseEntity<Map<Carrier, BigDecimal>> getRates(@RequestBody Shipment shipment) {
        Map<Carrier, BigDecimal> rates = shipmentService.quoteRates(shipment);
        return ResponseEntity.ok(rates);
    }

    @GetMapping("/track/{trackingNumber}")
    public ResponseEntity<Shipment> trackShipment(@PathVariable String trackingNumber) {
        Shipment shipment = shipmentService.trackShipment(trackingNumber);
        return ResponseEntity.ok(shipment);
    }

    @PostMapping("/cancel/{trackingNumber}")
    public ResponseEntity<Shipment> cancelShipment(@PathVariable String trackingNumber) {
        Shipment shipment = shipmentService.cancelShipment(trackingNumber);
        return ResponseEntity.ok(shipment);
    }
}
