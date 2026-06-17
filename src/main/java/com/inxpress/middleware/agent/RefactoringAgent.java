package com.inxpress.middleware.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RefactoringAgent implements AIAgent {

    private static final Logger log = LoggerFactory.getLogger(RefactoringAgent.class);

    @Override
    public AgentResult run(AgentTask task) {
        log.info("RefactoringAgent running task...");
        return task.execute();
    }
}
