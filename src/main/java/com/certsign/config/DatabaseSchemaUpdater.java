package com.certsign.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DatabaseSchemaUpdater implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseSchemaUpdater(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        ensureLicenceTypeSchema();
    }

    private void ensureLicenceTypeSchema() {
        execute("""
                CREATE TABLE IF NOT EXISTS licence_types (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    name VARCHAR(120) NOT NULL,
                    description VARCHAR(255) DEFAULT NULL,
                    active BIT(1) NOT NULL,
                    created_at DATETIME(6) NOT NULL,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_licence_types_name (name)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
                """);

        ensureColumn("programs", "licence_type_id", "BIGINT NULL");
        ensureIndex("programs", "idx_programs_licence_type", "licence_type_id");
        ensureForeignKey(
                "programs",
                "fk_programs_licence_type",
                "licence_type_id",
                "licence_types",
                "id"
        );

        ensureColumn("certificates", "program_id", "BIGINT NULL");
        ensureIndex("certificates", "idx_certificates_program", "program_id");
        ensureForeignKey(
                "certificates",
                "fk_certificates_program",
                "program_id",
                "programs",
                "id"
        );

        ensureColumn("certificates", "licence_type_id", "BIGINT NULL");
        ensureIndex("certificates", "idx_certificates_licence_type", "licence_type_id");
        ensureForeignKey(
                "certificates",
                "fk_certificates_licence_type",
                "licence_type_id",
                "licence_types",
                "id"
        );

        ensureColumn("certificates", "submitted_for_approval", "BIT(1) NOT NULL DEFAULT b'0'");
    }

    private void ensureColumn(String tableName, String columnName, String definition) {
        if (!tableExists(tableName) || columnExists(tableName, columnName)) {
            return;
        }
        execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
    }

    private void ensureIndex(String tableName, String indexName, String columnName) {
        if (!tableExists(tableName) || indexExists(tableName, indexName)) {
            return;
        }
        execute("ALTER TABLE " + tableName + " ADD INDEX " + indexName + " (" + columnName + ")");
    }

    private void ensureForeignKey(String tableName,
                                  String foreignKeyName,
                                  String columnName,
                                  String referencedTableName,
                                  String referencedColumnName) {
        if (!tableExists(tableName)
                || !tableExists(referencedTableName)
                || !columnExists(tableName, columnName)
                || foreignKeyExists(tableName, foreignKeyName)) {
            return;
        }
        execute("ALTER TABLE " + tableName
                + " ADD CONSTRAINT " + foreignKeyName
                + " FOREIGN KEY (" + columnName + ") REFERENCES "
                + referencedTableName + " (" + referencedColumnName + ")");
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                """, Integer.class, tableName);
        return count != null && count > 0;
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND column_name = ?
                """, Integer.class, tableName, columnName);
        return count != null && count > 0;
    }

    private boolean indexExists(String tableName, String indexName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND index_name = ?
                """, Integer.class, tableName, indexName);
        return count != null && count > 0;
    }

    private boolean foreignKeyExists(String tableName, String foreignKeyName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.table_constraints
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND constraint_name = ?
                  AND constraint_type = 'FOREIGN KEY'
                """, Integer.class, tableName, foreignKeyName);
        return count != null && count > 0;
    }

    private void execute(String sql) {
        jdbcTemplate.execute(sql);
    }
}
