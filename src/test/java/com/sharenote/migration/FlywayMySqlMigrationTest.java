package com.sharenote.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.DriverManager;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class FlywayMySqlMigrationTest {

    @Container
    public static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("sharenote")
            .withUsername("sharenote")
            .withPassword("sharenote-dev-secret")
            .withStartupTimeout(Duration.ofMinutes(5));

    @Test
    // flywayMigratesMysqlSchema: Verifies Flyway migrations run on real MySQL.
    void flywayMigratesMysqlSchema() throws Exception {
        Flyway flyway = Flyway.configure()
                .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration")
                .cleanDisabled(true)
                .load();

        flyway.migrate();

        try (var connection = DriverManager.getConnection(
                MYSQL.getJdbcUrl(),
                MYSQL.getUsername(),
                MYSQL.getPassword());
             var statement = connection.createStatement();
             var resultSet = statement.executeQuery(
                     "SELECT metadata_value FROM application_metadata WHERE metadata_key = 'schema_owner'")) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getString("metadata_value")).isEqualTo("flyway");
        }finally {
            flyway.clean();
        }
    }
}
