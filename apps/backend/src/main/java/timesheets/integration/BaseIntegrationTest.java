/*
    Every integration test files will inherit from this class
    To avoid duplicate or repetitive code setup

    It will handle the following:
    - Start PostgreSQL using Testcontainers
    - Start the Spring Boot application
    - Configure to use MockMvc (confirmed in pom.xml)
 */

