from datetime import datetime, timedelta
from airflow import DAG
from airflow.providers.http.operators.http import SimpleHttpOperator
import json

default_args = {
    'owner': 'inxpress-ops',
    'depends_on_past': False,
    'start_date': datetime(2026, 6, 17),
    'email_on_failure': True,
    'email': ['ops-alerts@inxpress.com'],
    'retries': 2,
    'retry_delay': timedelta(minutes=5),
}

with DAG(
    'nightly_ai_devops_workflow',
    default_args=default_args,
    description='10 PM nightly AI automation for carrier adapter reviews, test generation, and documentation',
    schedule_interval='0 22 * * *',  # Run daily at 10 PM
    catchup=False,
    tags=['ai-agent', 'devops', 'nightly'],
) as dag:

    # 1. Review Carrier Adapters using CarrierAdapterAgent
    review_carrier_adapters = SimpleHttpOperator(
        task_id='review_carrier_adapters',
        http_conn_id='inxpress_middleware_api',
        endpoint='/api/v1/agents/carrieradapteragent/execute',
        method='POST',
        headers={"Content-Type": "application/json"},
        data=json.dumps({
            "inputData": "Scan and research FedEx, UPS, and DHL spec updates",
            "context": {"env": "production"}
        }),
        response_filter=lambda response: response.json(),
    )

    # 2. Generate Missing Tests using TestGenerationAgent
    generate_missing_tests = SimpleHttpOperator(
        task_id='generate_missing_tests',
        http_conn_id='inxpress_middleware_api',
        endpoint='/api/v1/agents/testgenerationagent/execute',
        method='POST',
        headers={"Content-Type": "application/json"},
        data=json.dumps({
            "inputData": "Identify gaps in adapter test coverage and write JUnit files",
            "context": {"env": "production"}
        }),
        response_filter=lambda response: response.json(),
    )

    # 3. Update System Documentation using DocumentationAgent
    update_documentation = SimpleHttpOperator(
        task_id='update_documentation',
        http_conn_id='inxpress_middleware_api',
        endpoint='/api/v1/agents/documentationagent/execute',
        method='POST',
        headers={"Content-Type": "application/json"},
        data=json.dumps({
            "inputData": "Generate API specification markdown updates",
            "context": {"env": "production"}
        }),
        response_filter=lambda response: response.json(),
    )

    # 4. Trigger Refactoring and Security Review
    security_and_refactoring_review = SimpleHttpOperator(
        task_id='security_and_refactoring_review',
        http_conn_id='inxpress_middleware_api',
        endpoint='/api/v1/agents/securityreviewagent/execute',
        method='POST',
        headers={"Content-Type": "application/json"},
        data=json.dumps({
            "inputData": "Verify security compliance and refactor Virtual Thread scopes",
            "context": {"env": "production"}
        }),
        response_filter=lambda response: response.json(),
    )

    # Dependency routing workflow sequence
    review_carrier_adapters >> generate_missing_tests >> update_documentation >> security_and_refactoring_review
