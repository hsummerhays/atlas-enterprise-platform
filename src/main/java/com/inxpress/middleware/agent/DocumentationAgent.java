package com.inxpress.middleware.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DocumentationAgent implements AIAgent {

    private static final Logger log = LoggerFactory.getLogger(DocumentationAgent.class);

    @Override
    public AgentResult run(AgentTask task) {
        log.info("DocumentationAgent running task...");
        return task.execute();
    }
}
