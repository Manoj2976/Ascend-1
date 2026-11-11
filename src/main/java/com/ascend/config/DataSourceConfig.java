package com.ascend.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.net.URI;

/**
 * Accepts DATABASE_URL either as a JDBC url (jdbc:postgresql://host/db) or in the
 * postgres://user:pass@host:5432/db form that Render and Heroku hand out.
 */
@Configuration
public class DataSourceConfig {

    @Bean
    public DataSource dataSource(Environment env) {
        String raw = env.getProperty("DATABASE_URL", "jdbc:postgresql://localhost:5432/ascend");
        String user = env.getProperty("DB_USERNAME", "postgres");
        String pass = env.getProperty("DB_PASSWORD", "postgres");

        String jdbc = raw;
        if (raw.startsWith("postgres://") || raw.startsWith("postgresql://")) {
            URI uri = URI.create(raw);
            if (uri.getUserInfo() != null) {
                String[] parts = uri.getUserInfo().split(":", 2);
                user = parts[0];
                if (parts.length > 1) pass = parts[1];
            }
            String host = uri.getHost();
            int port = uri.getPort() > 0 ? uri.getPort() : 5432;
            StringBuilder sb = new StringBuilder("jdbc:postgresql://").append(host).append(':').append(port).append(uri.getPath());
            String query = uri.getQuery();
            if (query != null && !query.isBlank()) sb.append('?').append(query);
            jdbc = sb.toString();
            // Render's external hostnames need TLS; internal ones (no dots) do not.
            if (host != null && host.contains(".") && !jdbc.contains("sslmode=")) {
                jdbc += (jdbc.contains("?") ? "&" : "?") + "sslmode=require";
            }
        }

        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(jdbc);
        cfg.setUsername(user);
        cfg.setPassword(pass);
        cfg.setMaximumPoolSize(Integer.parseInt(env.getProperty("DB_POOL_SIZE", "5")));
        cfg.setPoolName("ascend-pool");
        return new HikariDataSource(cfg);
    }
}
