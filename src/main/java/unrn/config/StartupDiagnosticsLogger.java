package unrn.config;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;

@Component
public class StartupDiagnosticsLogger {

    private static final Logger LOG = LoggerFactory.getLogger(StartupDiagnosticsLogger.class);

    private final Environment environment;
    private final DataSource dataSource;
    private final ObjectProvider<Flyway> flywayProvider;

    public StartupDiagnosticsLogger(Environment environment, DataSource dataSource,
            ObjectProvider<Flyway> flywayProvider) {
        this.environment = environment;
        this.dataSource = dataSource;
        this.flywayProvider = flywayProvider;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logStartupDiagnostics() {
        LOG.info("Active profiles: {}", Arrays.toString(environment.getActiveProfiles()));
        LOG.info("Datasource URL: {}", resolveJdbcUrl());

        var flyway = flywayProvider.getIfAvailable();
        if (flyway != null) {
            var info = flyway.info();
            LOG.info("Flyway current version: {}", info.current() != null ? info.current().getVersion() : "<none>");
            LOG.info("Flyway applied migrations count: {}", info.applied().length);
            LOG.info("Flyway pending migrations count: {}", info.pending().length);
        } else {
            LOG.warn("Flyway bean not available");
        }
    }

    private String resolveJdbcUrl() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.getMetaData().getURL();
        } catch (SQLException ex) {
            LOG.warn("Could not resolve datasource URL", ex);
            return "<unavailable>";
        }
    }
}