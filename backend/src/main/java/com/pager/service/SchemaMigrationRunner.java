package com.pager.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Applies additive, idempotent column migrations for databases created before
 * a given column existed. schema.sql only runs CREATE TABLE IF NOT EXISTS, so
 * it never alters an already-existing table — this runner covers that gap.
 * Runs before any other ApplicationRunner (e.g. SeedDataService) so the schema
 * is fully up to date before any queries execute.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SchemaMigrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaMigrationRunner.class);

    private final JdbcTemplate jdbcTemplate;

    public SchemaMigrationRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        addColumnIfMissing("study_sessions", "source", "TEXT NOT NULL DEFAULT 'PAGE_ACCEPTED'");
        addColumnIfMissing("study_sessions", "notes", "TEXT");
        addColumnIfMissing("study_sessions", "checkin_message_id", "TEXT");
        addColumnIfMissing("study_sessions", "checkin_chat_id", "TEXT");
    }

    private void addColumnIfMissing(String table, String column, String columnDefinition) {
        Set<String> existing = new HashSet<>();
        jdbcTemplate.query("PRAGMA table_info(" + table + ")", rs -> {
            existing.add(rs.getString("name"));
        });
        if (existing.isEmpty()) {
            // Table doesn't exist yet (fresh DB) — schema.sql will create it with the column already included.
            return;
        }
        if (!existing.contains(column)) {
            log.info("Migrating schema: adding column {}.{}", table, column);
            jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + columnDefinition);
        }
    }
}
