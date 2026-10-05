package org.santayn.testing.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class DatabaseBackupService {

    private static final DateTimeFormatter FILE_TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC);
    private static final int STDERR_LIMIT_BYTES = 16 * 1024;

    private final DataSource dataSource;
    private final DataSourceProperties dataSourceProperties;
    private final String pgDumpPath;
    private final String psqlPath;
    private final String postgresSchema;
    private final boolean includeCleanStatements;

    public DatabaseBackupService(DataSource dataSource,
                                 DataSourceProperties dataSourceProperties,
                                 @Value("${app.database-backup.pg-dump-path:pg_dump}") String pgDumpPath,
                                 @Value("${app.database-backup.psql-path:psql}") String psqlPath,
                                 @Value("${app.database-backup.schema:}") String postgresSchema,
                                 @Value("${app.database-backup.include-clean:true}") boolean includeCleanStatements) {
        this.dataSource = dataSource;
        this.dataSourceProperties = dataSourceProperties;
        this.pgDumpPath = pgDumpPath;
        this.psqlPath = psqlPath;
        this.postgresSchema = postgresSchema;
        this.includeCleanStatements = includeCleanStatements;
    }

    public DatabaseBackup createFullBackup() {
        String productName = databaseProductName();
        if (productName.toLowerCase(Locale.ROOT).contains("postgresql")) {
            return createPostgresBackup();
        }
        if (productName.toLowerCase(Locale.ROOT).contains("h2")) {
            return createH2Backup();
        }
        throw new DatabaseBackupException("Database backup is supported for PostgreSQL databases.");
    }

    public void restoreBackup(MultipartFile backupFile) {
        if (backupFile == null || backupFile.isEmpty()) {
            throw new DatabaseBackupException("Uploaded backup file is empty.");
        }

        Path uploadedBackupPath = copyToTempFile(backupFile);
        try {
            String productName = databaseProductName();
            if (productName.toLowerCase(Locale.ROOT).contains("postgresql")) {
                restorePostgresBackup(uploadedBackupPath);
                return;
            }
            if (productName.toLowerCase(Locale.ROOT).contains("h2")) {
                restoreH2Backup(uploadedBackupPath);
                return;
            }
            throw new DatabaseBackupException("Database restore is supported for PostgreSQL databases.");
        } finally {
            deleteQuietly(uploadedBackupPath);
        }
    }

    private String databaseProductName() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.getMetaData().getDatabaseProductName();
        } catch (SQLException exception) {
            throw new DatabaseBackupException("Could not detect database type.", exception);
        }
    }

    private DatabaseBackup createPostgresBackup() {
        Path backupPath = createTempBackupPath();
        PostgresConnectionSettings settings = parsePostgresJdbcUrl(dataSourceProperties.getUrl());
        List<String> command = buildPgDumpCommand(settings);

        Process process;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command)
                    .redirectOutput(backupPath.toFile());
            Map<String, String> environment = processBuilder.environment();
            environment.put("PGPASSWORD", nullToEmpty(dataSourceProperties.getPassword()));
            settings.parameters().forEach((key, value) -> {
                if ("sslmode".equalsIgnoreCase(key) && value != null && !value.isBlank()) {
                    environment.put("PGSSLMODE", value);
                }
            });
            process = processBuilder.start();
        } catch (IOException exception) {
            deleteQuietly(backupPath);
            throw new DatabaseBackupException(
                    "Could not start pg_dump. Configure app.database-backup.pg-dump-path if pg_dump is not on PATH.",
                    exception
            );
        }

        CompletableFuture<String> stderrFuture = CompletableFuture.supplyAsync(
                () -> readLimited(process.getErrorStream(), STDERR_LIMIT_BYTES)
        );

        int exitCode;
        try {
            exitCode = process.waitFor();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            deleteQuietly(backupPath);
            throw new DatabaseBackupException("Database backup was interrupted.", exception);
        }

        if (exitCode != 0) {
            deleteQuietly(backupPath);
            String stderr = nullToEmpty(stderrFuture.join()).strip();
            String details = stderr.isBlank() ? "" : " Details: " + abbreviate(stderr, 1000);
            throw new DatabaseBackupException("pg_dump failed with exit code " + exitCode + "." + details);
        }

        return backupResult(backupPath);
    }

    private DatabaseBackup createH2Backup() {
        Path backupPath = createTempBackupPath();
        deleteQuietly(backupPath);
        String escapedPath = backupPath.toAbsolutePath()
                .toString()
                .replace('\\', '/')
                .replace("'", "''");
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("SCRIPT DROP TO '" + escapedPath + "'");
        } catch (SQLException exception) {
            deleteQuietly(backupPath);
            throw new DatabaseBackupException("Could not create H2 database backup.", exception);
        }
        return backupResult(backupPath);
    }

    private void restorePostgresBackup(Path uploadedBackupPath) {
        PostgresConnectionSettings settings = parsePostgresJdbcUrl(dataSourceProperties.getUrl());
        Path restorePath = preparePostgresRestoreFile(uploadedBackupPath);

        try {
            List<String> command = buildPsqlRestoreCommand(settings, restorePath);

            Process process;
            try {
                ProcessBuilder processBuilder = new ProcessBuilder(command)
                        .redirectOutput(ProcessBuilder.Redirect.DISCARD);
                Map<String, String> environment = processBuilder.environment();
                environment.put("PGPASSWORD", nullToEmpty(dataSourceProperties.getPassword()));
                settings.parameters().forEach((key, value) -> {
                    if ("sslmode".equalsIgnoreCase(key) && value != null && !value.isBlank()) {
                        environment.put("PGSSLMODE", value);
                    }
                });
                process = processBuilder.start();
            } catch (IOException exception) {
                throw new DatabaseBackupException(
                        "Could not start psql. Configure app.database-backup.psql-path if psql is not on PATH.",
                        exception
                );
            }

            CompletableFuture<String> stderrFuture = CompletableFuture.supplyAsync(
                    () -> readLimited(process.getErrorStream(), STDERR_LIMIT_BYTES)
            );

            int exitCode;
            try {
                exitCode = process.waitFor();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new DatabaseBackupException("Database restore was interrupted.", exception);
            }

            if (exitCode != 0) {
                String stderr = nullToEmpty(stderrFuture.join()).strip();
                String details = stderr.isBlank() ? "" : " Details: " + abbreviate(stderr, 1000);
                throw new DatabaseBackupException("psql restore failed with exit code " + exitCode + "." + details);
            }
        } finally {
            if (!restorePath.equals(uploadedBackupPath)) {
                deleteQuietly(restorePath);
            }
        }
    }

    private void restoreH2Backup(Path uploadedBackupPath) {
        String escapedPath = uploadedBackupPath.toAbsolutePath()
                .toString()
                .replace('\\', '/')
                .replace("'", "''");
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("DROP ALL OBJECTS");
            statement.execute("RUNSCRIPT FROM '" + escapedPath + "'");
        } catch (SQLException exception) {
            throw new DatabaseBackupException(
                    "Could not restore H2 database backup. Details: " + exception.getMessage(),
                    exception
            );
        }
    }

    private List<String> buildPgDumpCommand(PostgresConnectionSettings settings) {
        List<String> command = new ArrayList<>();
        command.add(pgDumpPath);
        command.add("--format=plain");
        command.add("--encoding=UTF8");
        command.add("--blobs");
        command.add("--no-owner");
        command.add("--no-privileges");
        if (includeCleanStatements) {
            command.add("--clean");
            command.add("--if-exists");
        }
        if (postgresSchema != null && !postgresSchema.isBlank()) {
            command.add("--schema");
            command.add(postgresSchema.trim());
        }
        command.add("--host");
        command.add(settings.host());
        command.add("--port");
        command.add(String.valueOf(settings.port()));
        command.add("--username");
        command.add(nullToEmpty(dataSourceProperties.getUsername()));
        command.add("--dbname");
        command.add(settings.database());
        return command;
    }

    private List<String> buildPsqlRestoreCommand(PostgresConnectionSettings settings, Path backupPath) {
        List<String> command = new ArrayList<>();
        command.add(psqlPath);
        command.add("--set");
        command.add("ON_ERROR_STOP=1");
        command.add("--single-transaction");
        command.add("--host");
        command.add(settings.host());
        command.add("--port");
        command.add(String.valueOf(settings.port()));
        command.add("--username");
        command.add(nullToEmpty(dataSourceProperties.getUsername()));
        command.add("--dbname");
        command.add(settings.database());
        command.add("--file");
        command.add(backupPath.toAbsolutePath().toString());
        return command;
    }

    private Path preparePostgresRestoreFile(Path uploadedBackupPath) {
        if (supportsPostgresTransactionTimeout()) {
            return uploadedBackupPath;
        }

        Path filteredBackupPath = createTempRestorePath();
        try (BufferedReader reader = Files.newBufferedReader(uploadedBackupPath, StandardCharsets.UTF_8);
             BufferedWriter writer = Files.newBufferedWriter(filteredBackupPath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (isTransactionTimeoutStatement(line)) {
                    continue;
                }
                writer.write(line);
                writer.newLine();
            }
            return filteredBackupPath;
        } catch (IOException exception) {
            deleteQuietly(filteredBackupPath);
            throw new DatabaseBackupException("Could not prepare uploaded backup file for restore.", exception);
        }
    }

    private boolean supportsPostgresTransactionTimeout() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("SHOW transaction_timeout");
            return true;
        } catch (SQLException ignored) {
            return false;
        }
    }

    private boolean isTransactionTimeoutStatement(String line) {
        String normalized = line.trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
        return "set transaction_timeout = 0;".equals(normalized);
    }

    private DatabaseBackup backupResult(Path backupPath) {
        try {
            long sizeBytes = Files.size(backupPath);
            if (sizeBytes == 0) {
                deleteQuietly(backupPath);
                throw new DatabaseBackupException("Database backup file is empty.");
            }
            return new DatabaseBackup(
                    backupPath,
                    "student-test-database-backup-" + FILE_TIMESTAMP_FORMATTER.format(Instant.now()) + ".sql",
                    sizeBytes
            );
        } catch (IOException exception) {
            deleteQuietly(backupPath);
            throw new DatabaseBackupException("Could not read generated backup file.", exception);
        }
    }

    private Path createTempBackupPath() {
        try {
            return Files.createTempFile("student-test-database-backup-", ".sql");
        } catch (IOException exception) {
            throw new DatabaseBackupException("Could not create temporary backup file.", exception);
        }
    }

    private Path copyToTempFile(MultipartFile backupFile) {
        Path uploadedBackupPath = createTempRestorePath();
        try (InputStream inputStream = backupFile.getInputStream()) {
            Files.copy(inputStream, uploadedBackupPath, StandardCopyOption.REPLACE_EXISTING);
            if (Files.size(uploadedBackupPath) == 0) {
                deleteQuietly(uploadedBackupPath);
                throw new DatabaseBackupException("Uploaded backup file is empty.");
            }
            return uploadedBackupPath;
        } catch (IOException exception) {
            deleteQuietly(uploadedBackupPath);
            throw new DatabaseBackupException("Could not read uploaded backup file.", exception);
        }
    }

    private Path createTempRestorePath() {
        try {
            return Files.createTempFile("student-test-database-restore-", ".sql");
        } catch (IOException exception) {
            throw new DatabaseBackupException("Could not create temporary restore file.", exception);
        }
    }

    private PostgresConnectionSettings parsePostgresJdbcUrl(String jdbcUrl) {
        if (jdbcUrl == null || !jdbcUrl.startsWith("jdbc:postgresql:")) {
            throw new DatabaseBackupException("PostgreSQL JDBC URL is required for database backups.");
        }

        String rest = jdbcUrl.substring("jdbc:postgresql:".length());
        if (rest.startsWith("//")) {
            int slashIndex = rest.indexOf('/', 2);
            if (slashIndex < 0 || slashIndex == rest.length() - 1) {
                throw new DatabaseBackupException("PostgreSQL JDBC URL must include a database name.");
            }
            String hosts = rest.substring(2, slashIndex);
            String databaseAndParams = rest.substring(slashIndex + 1);
            HostPort hostPort = parseHostPort(hosts.split(",", 2)[0]);
            ParsedDatabase database = parseDatabaseAndParams(databaseAndParams);
            return new PostgresConnectionSettings(hostPort.host(), hostPort.port(), database.database(), database.parameters());
        }

        ParsedDatabase database = parseDatabaseAndParams(rest);
        return new PostgresConnectionSettings("localhost", 5432, database.database(), database.parameters());
    }

    private HostPort parseHostPort(String hostPortValue) {
        if (hostPortValue == null || hostPortValue.isBlank()) {
            return new HostPort("localhost", 5432);
        }

        String trimmed = hostPortValue.trim();
        if (trimmed.startsWith("[")) {
            int closingBracket = trimmed.indexOf(']');
            if (closingBracket < 0) {
                throw new DatabaseBackupException("Invalid PostgreSQL host in JDBC URL.");
            }
            String host = trimmed.substring(1, closingBracket);
            int port = parsePort(trimmed.substring(closingBracket + 1));
            return new HostPort(host, port);
        }

        int colonIndex = trimmed.lastIndexOf(':');
        if (colonIndex > 0) {
            String host = trimmed.substring(0, colonIndex);
            int port = parsePort(trimmed.substring(colonIndex));
            return new HostPort(host, port);
        }
        return new HostPort(trimmed, 5432);
    }

    private int parsePort(String portPart) {
        if (portPart == null || portPart.isBlank()) {
            return 5432;
        }
        String normalized = portPart.startsWith(":") ? portPart.substring(1) : portPart;
        if (normalized.isBlank()) {
            return 5432;
        }
        try {
            return Integer.parseInt(normalized);
        } catch (NumberFormatException exception) {
            throw new DatabaseBackupException("Invalid PostgreSQL port in JDBC URL.", exception);
        }
    }

    private ParsedDatabase parseDatabaseAndParams(String databaseAndParams) {
        int queryIndex = databaseAndParams.indexOf('?');
        String rawDatabase = queryIndex >= 0 ? databaseAndParams.substring(0, queryIndex) : databaseAndParams;
        if (rawDatabase.isBlank()) {
            throw new DatabaseBackupException("PostgreSQL JDBC URL must include a database name.");
        }
        String database = urlDecode(rawDatabase);
        Map<String, String> parameters = queryIndex >= 0
                ? parseQueryParameters(databaseAndParams.substring(queryIndex + 1))
                : Map.of();
        return new ParsedDatabase(database, parameters);
    }

    private Map<String, String> parseQueryParameters(String query) {
        Map<String, String> parameters = new LinkedHashMap<>();
        if (query == null || query.isBlank()) {
            return parameters;
        }
        for (String part : query.split("&")) {
            if (part.isBlank()) {
                continue;
            }
            int equalsIndex = part.indexOf('=');
            String key = equalsIndex >= 0 ? part.substring(0, equalsIndex) : part;
            String value = equalsIndex >= 0 ? part.substring(equalsIndex + 1) : "";
            parameters.put(urlDecode(key), urlDecode(value));
        }
        return parameters;
    }

    private static String readLimited(InputStream inputStream, int limitBytes) {
        try (InputStream input = inputStream;
             ByteArrayOutputStream output = new ByteArrayOutputStream(Math.min(limitBytes, 1024))) {
            byte[] buffer = new byte[4096];
            int read;
            int stored = 0;
            while ((read = input.read(buffer)) != -1) {
                int remaining = limitBytes - stored;
                if (remaining > 0) {
                    int toStore = Math.min(read, remaining);
                    output.write(buffer, 0, toStore);
                    stored += toStore;
                }
            }
            return output.toString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            return "Could not read pg_dump error output: " + exception.getMessage();
        }
    }

    private static String urlDecode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String abbreviate(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }

    public record DatabaseBackup(Path path, String fileName, long sizeBytes) {
        public void deleteQuietly() {
            DatabaseBackupService.deleteQuietly(path);
        }
    }

    private record PostgresConnectionSettings(String host,
                                              int port,
                                              String database,
                                              Map<String, String> parameters) {
    }

    private record HostPort(String host, int port) {
    }

    private record ParsedDatabase(String database, Map<String, String> parameters) {
    }
}
