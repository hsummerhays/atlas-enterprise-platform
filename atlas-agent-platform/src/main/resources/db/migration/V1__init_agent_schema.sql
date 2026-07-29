CREATE TABLE agent_metrics (
    id BIGSERIAL PRIMARY KEY,
    agent_name VARCHAR(100) UNIQUE NOT NULL,
    prs_generated INTEGER NOT NULL,
    prs_accepted INTEGER NOT NULL,
    time_saved_minutes DOUBLE PRECISION NOT NULL,
    bugs_introduced INTEGER NOT NULL,
    cycle_time_reduction_percentage DOUBLE PRECISION NOT NULL
);
