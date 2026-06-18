package com.crudman.datasourcehelper.model;

/**
 * 数据库连接配置
 */
public class DatabaseConfig {

    public enum DbType {
        MYSQL("MySQL", "com.mysql.cj.jdbc.Driver", 3306),
        POSTGRESQL("PostgreSQL", "org.postgresql.Driver", 5432);

        private final String displayName;
        private final String driverClass;
        private final int defaultPort;

        DbType(String displayName, String driverClass, int defaultPort) {
            this.displayName = displayName;
            this.driverClass = driverClass;
            this.defaultPort = defaultPort;
        }

        public String getDisplayName() { return displayName; }
        public String getDriverClass() { return driverClass; }
        public int getDefaultPort() { return defaultPort; }

        @Override
        public String toString() { return displayName; }
    }

    private DbType dbType;
    private String host;
    private int port;
    private String databaseName;
    private String username;
    private String password;

    public DatabaseConfig() {
        this.dbType = DbType.MYSQL;
        this.host = "localhost";
        this.port = DbType.MYSQL.getDefaultPort();
        this.databaseName = "";
        this.username = "root";
        this.password = "";
    }

    public String getJdbcUrl() {
        String dbSegment;
        if (databaseName != null && !databaseName.isEmpty()) {
            dbSegment = "/" + databaseName;
        } else {
            // PostgreSQL 必须指定数据库才能连接，默认连 postgres
            dbSegment = (dbType == DbType.POSTGRESQL) ? "/postgres" : "/";
        }
        return switch (dbType) {
            case MYSQL -> String.format(
                    "jdbc:mysql://%s:%d%s?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true",
                    host, port, dbSegment);
            case POSTGRESQL -> String.format(
                    "jdbc:postgresql://%s:%d%s?currentSchema=public",
                    host, port, dbSegment);
        };
    }

    // Getters and Setters
    public DbType getDbType() { return dbType; }
    public void setDbType(DbType dbType) { this.dbType = dbType; }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public String getDatabaseName() { return databaseName; }
    public void setDatabaseName(String databaseName) { this.databaseName = databaseName; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getDriverClass() { return dbType.getDriverClass(); }
}
