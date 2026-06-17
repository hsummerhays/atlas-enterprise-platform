package com.inxpress.middleware.domain.model;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;

@Embeddable
public record Address(
    @NotBlank(message = "Name is required") String contactName,
    String companyName,
    @NotBlank(message = "Street Line 1 is required") String street1,
    String street2,
    @NotBlank(message = "City is required") String city,
    @NotBlank(message = "State/Province is required") String stateOrProvince,
    @NotBlank(message = "Postal Code is required") String postalCode,
    @NotBlank(message = "Country Code is required") String countryCode,
    @NotBlank(message = "Phone is required") String phone
) {
    // Zero-arg constructor for JPA/Hibernate proxy/instantiation
    public Address() {
        this(null, null, null, null, null, null, null, null, null);
    }
}
