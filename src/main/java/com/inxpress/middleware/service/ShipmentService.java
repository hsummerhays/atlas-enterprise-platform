package com.inxpress.middleware.service;

import com.inxpress.middleware.adapter.CarrierAdapter;
import com.inxpress.middleware.adapter.CarrierRegistry;
import com.inxpress.middleware.domain.exception.CarrierAdapterException;
import com.inxpress.middleware.domain.exception.ShipmentNotFoundException;
import com.inxpress.middleware.domain.model.Carrier;
import com.inxpress.middleware.domain.model.Shipment;
import com.inxpress.middleware.domain.model.ShipmentStatus;
import com.inxpress.middleware.domain.repository.ShipmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

@Service
public class ShipmentService {

    private static final Logger log = LoggerFactory.getLogger(ShipmentService.class);

    private final ShipmentRepository shipmentRepository;
    private final CarrierRegistry carrierRegistry;
    private final ShipmentEventPublisher eventPublisher;

    public ShipmentService(ShipmentRepository shipmentRepository,
                           CarrierRegistry carrierRegistry,
                           ShipmentEventPublisher eventPublisher) {
        this.shipmentRepository = shipmentRepository;
        this.carrierRegistry = carrierRegistry;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Shipment createShipment(Shipment shipment) {
        // Client-supplied id/status must never control persistence: a non-null id would make
        // Spring Data treat this as an update and could overwrite an unrelated existing row.
        shipment.setId(null);
        shipment.setStatus(ShipmentStatus.CREATED);
        Shipment saved = shipmentRepository.save(shipment);
        eventPublisher.publishShipmentEvent("SHIPMENT_CREATED", saved);
        return saved;
    }

    @Transactional
    public Shipment bookShipment(Long id) {
        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new ShipmentNotFoundException("Shipment not found with id: " + id));

        CarrierAdapter adapter = carrierRegistry.getAdapter(shipment.getCarrier())
                .orElseThrow(() -> new CarrierAdapterException("No adapter registered for carrier: " + shipment.getCarrier()));

        Shipment bookedShipment = adapter.bookShipment(shipment);
        Shipment saved = shipmentRepository.save(bookedShipment);

        eventPublisher.publishShipmentEvent("SHIPMENT_BOOKED", saved);
        return saved;
    }

    public Map<Carrier, BigDecimal> quoteRates(Shipment shipment) {
        Map<Carrier, BigDecimal> quotes = new EnumMap<>(Carrier.class);
        for (Carrier carrier : Carrier.values()) {
            carrierRegistry.getAdapter(carrier).ifPresent(adapter -> {
                try {
                    BigDecimal rate = adapter.quoteRate(shipment);
                    quotes.put(carrier, rate);
                } catch (Exception e) {
                    log.warn("Failed to get rate quote from carrier {}: {}", carrier, e.getMessage());
                }
            });
        }
        return quotes;
    }

    @Transactional
    public Shipment trackShipment(String trackingNumber) {
        Shipment shipment = shipmentRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new ShipmentNotFoundException("Shipment not found with tracking number: " + trackingNumber));

        CarrierAdapter adapter = carrierRegistry.getAdapter(shipment.getCarrier())
                .orElseThrow(() -> new CarrierAdapterException("No adapter registered for carrier: " + shipment.getCarrier()));

        ShipmentStatus status = adapter.trackShipment(trackingNumber);
        shipment.setStatus(status);
        Shipment saved = shipmentRepository.save(shipment);

        eventPublisher.publishShipmentEvent("SHIPMENT_STATUS_UPDATED", saved);
        return saved;
    }

    @Transactional
    public Shipment cancelShipment(String trackingNumber) {
        Shipment shipment = shipmentRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new ShipmentNotFoundException("Shipment not found with tracking number: " + trackingNumber));

        CarrierAdapter adapter = carrierRegistry.getAdapter(shipment.getCarrier())
                .orElseThrow(() -> new CarrierAdapterException("No adapter registered for carrier: " + shipment.getCarrier()));

        boolean success = adapter.cancelShipment(trackingNumber);
        if (success) {
            shipment.setStatus(ShipmentStatus.CANCELLED);
            Shipment saved = shipmentRepository.save(shipment);
            eventPublisher.publishShipmentEvent("SHIPMENT_CANCELLED", saved);
            return saved;
        } else {
            throw new CarrierAdapterException("Failed to cancel shipment with carrier: " + shipment.getCarrier());
        }
    }
}
