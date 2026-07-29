package io.github.hsummerhays.atlas.adapter;

import io.github.hsummerhays.atlas.domain.model.Carrier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class CarrierRegistry {

    private final Map<Carrier, CarrierAdapter> adapters;

    public CarrierRegistry(List<CarrierAdapter> adapterList) {
        this.adapters = adapterList.stream()
                .collect(Collectors.toMap(CarrierAdapter::getCarrier, Function.identity()));
    }

    public Optional<CarrierAdapter> getAdapter(Carrier carrier) {
        return Optional.ofNullable(adapters.get(carrier));
    }
}
