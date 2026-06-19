package com.inxpress.middleware.domain.model;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "shipments")
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tracking_number", unique = true)
    private String trackingNumber;

    @NotNull(message = "Carrier is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Carrier carrier;

    @NotBlank(message = "Service type is required")
    @Column(name = "service_type", nullable = false)
    private String serviceType;

    @NotNull(message = "Sender address is required")
    @Valid
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "contactName", column = @Column(name = "sender_contact_name")),
        @AttributeOverride(name = "companyName", column = @Column(name = "sender_company_name")),
        @AttributeOverride(name = "street1", column = @Column(name = "sender_street1")),
        @AttributeOverride(name = "street2", column = @Column(name = "sender_street2")),
        @AttributeOverride(name = "city", column = @Column(name = "sender_city")),
        @AttributeOverride(name = "stateOrProvince", column = @Column(name = "sender_state")),
        @AttributeOverride(name = "postalCode", column = @Column(name = "sender_postal_code")),
        @AttributeOverride(name = "countryCode", column = @Column(name = "sender_country_code")),
        @AttributeOverride(name = "phone", column = @Column(name = "sender_phone"))
    })
    private Address senderAddress;

    @NotNull(message = "Recipient address is required")
    @Valid
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "contactName", column = @Column(name = "recipient_contact_name")),
        @AttributeOverride(name = "companyName", column = @Column(name = "recipient_company_name")),
        @AttributeOverride(name = "street1", column = @Column(name = "recipient_street1")),
        @AttributeOverride(name = "street2", column = @Column(name = "recipient_street2")),
        @AttributeOverride(name = "city", column = @Column(name = "recipient_city")),
        @AttributeOverride(name = "stateOrProvince", column = @Column(name = "recipient_state")),
        @AttributeOverride(name = "postalCode", column = @Column(name = "recipient_postal_code")),
        @AttributeOverride(name = "countryCode", column = @Column(name = "recipient_country_code")),
        @AttributeOverride(name = "phone", column = @Column(name = "recipient_phone"))
    })
    private Address recipientAddress;

    @NotEmpty(message = "At least one package is required")
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "shipment_packages", joinColumns = @JoinColumn(name = "shipment_id"))
    private List<PackageDetail> packages = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShipmentStatus status;

    @Column(name = "total_cost")
    private BigDecimal totalCost;

    @Column(name = "created_date", nullable = false, updatable = false)
    private Instant createdDate;

    @Column(name = "updated_date")
    private Instant updatedDate;

    @PrePersist
    protected void onCreate() {
        createdDate = Instant.now();
        updatedDate = Instant.now();
        if (status == null) {
            status = ShipmentStatus.CREATED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedDate = Instant.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public Carrier getCarrier() {
        return carrier;
    }

    public void setCarrier(Carrier carrier) {
        this.carrier = carrier;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public Address getSenderAddress() {
        return senderAddress;
    }

    public void setSenderAddress(Address senderAddress) {
        this.senderAddress = senderAddress;
    }

    public Address getRecipientAddress() {
        return recipientAddress;
    }

    public void setRecipientAddress(Address recipientAddress) {
        this.recipientAddress = recipientAddress;
    }

    public List<PackageDetail> getPackages() {
        return packages;
    }

    public void setPackages(List<PackageDetail> packages) {
        this.packages = packages;
    }

    public ShipmentStatus getStatus() {
        return status;
    }

    public void setStatus(ShipmentStatus status) {
        this.status = status;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public Instant getUpdatedDate() {
        return updatedDate;
    }
}
