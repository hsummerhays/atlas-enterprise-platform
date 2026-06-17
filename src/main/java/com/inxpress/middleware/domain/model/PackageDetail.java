package com.inxpress.middleware.domain.model;

import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

@Embeddable
public record PackageDetail(
    double weightInKg,
    double lengthCm,
    double widthCm,
    double heightCm,
    String description,
    BigDecimal declaredValue
) {
    public PackageDetail() {
        this(0.0, 0.0, 0.0, 0.0, null, BigDecimal.ZERO);
    }
}
