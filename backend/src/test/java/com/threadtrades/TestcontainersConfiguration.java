package com.threadtrades;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	PostgreSQLContainer<?> postgresContainer() {
		return new PostgreSQLContainer<>(DockerImageName.parse("postgres:16"));
	}

	// app.jwt.secret has no default in application.yml (by design, so prod
	// never silently runs with a weak secret) -- tests need a stand-in that
	// doesn't depend on a JWT_SECRET env var being set locally or in CI.
	@Bean
	DynamicPropertyRegistrar jwtSecretRegistrar() {
		return registry -> registry.add("app.jwt.secret", () -> "test-only-jwt-secret-not-for-production-use");
	}

}
