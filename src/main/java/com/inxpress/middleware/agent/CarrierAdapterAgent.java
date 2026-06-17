package com.inxpress.middleware.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CarrierAdapterAgent implements AIAgent {

    private static final Logger log = LoggerFactory.getLogger(CarrierAdapterAgent.class);

    @Override
    public AgentResult run(AgentTask task) {
        log.info("CarrierAdapterAgent running task...");
        return task.execute();
    }
}
