package com.crudman.datasourcehelper.service;

import com.crudman.datasourcehelper.model.ColumnInfo;
import com.crudman.datasourcehelper.model.DatabaseConfig;
import com.crudman.datasourcehelper.model.TableInfo;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据库连接与元数据查询服务
 */
public class DatabaseService {

    private Connection connection;

    /**
     * 连接数据库
     */
    public void connect(DatabaseConfig config) throws SQLException, ClassNotFoundException {
        Class.forName(config.getDriverClass());
        connection = DriverManager.getConnection(
                config.getJdbcUrl(),
                config.getUsername(),
                config.getPassword()
        );
    }

    /**
     * 测试数据库连接
     * @return 测试结果消息
     */
    public String testConnection(DatabaseConfig config) {
        try {
            Class.forName(config.getDriverClass());
            try (Connection testConn = DriverManager.getConnection(
                    config.getJdbcUrl(),
                    config.getUsername(),
                    config.getPassword()
            )) {
                DatabaseMetaData meta = testConn.getMetaData();
                return String.format("连接成功！\n数据库: %s\n版本: %s\n驱动: %s %s",
                        meta.getDatabaseProductName(),
                        meta.getDatabaseProductVersion(),
                        meta.getDriverName(),
                        meta.getDriverVersion());
            }
        } catch (Exception e) {
            return "连接失败: " + e.getMessage();
        }
    }

    /**
     * 获取所有数据库列表
     */
    public List<String> fetchDatabases(DatabaseConfig config) throws SQLException {
        List<String> databases = new ArrayList<>();

        if (config.getDbType() == DatabaseConfig.DbType.MYSQL) {
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SHOW DATABASES")) {
                while (rs.next()) {
                    String db = rs.getString(1);
                    // 过滤系统库
                    if (!"information_schema".equalsIgnoreCase(db)
                            && !"mysql".equalsIgnoreCase(db)
                            && !"performance_schema".equalsIgnoreCase(db)
                            && !"sys".equalsIgnoreCase(db)) {
                        databases.add(db);
                    }
                }
            }
        } else {
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT datname FROM pg_database WHERE datistemplate = false AND datallowconn = true ORDER BY datname")) {
                while (rs.next()) {
                    databases.add(rs.getString("datname"));
                }
            }
        }

        return databases;
    }

    /**
     * 获取所有表信息
     */
    public List<TableInfo> fetchTables(DatabaseConfig config) throws SQLException {
        List<TableInfo> tables = new ArrayList<>();
        DatabaseMetaData meta = connection.getMetaData();

        String catalog = null;
        String schemaPattern = null;

        // MySQL 使用 catalog, PostgreSQL 使用 schema
        if (config.getDbType() == DatabaseConfig.DbType.MYSQL) {
            catalog = config.getDatabaseName();
        } else {
            schemaPattern = "public";
        }

        try (ResultSet rs = meta.getTables(catalog, schemaPattern, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                String tableComment = rs.getString("REMARKS");
                String schema = rs.getString("TABLE_SCHEM");

                TableInfo tableInfo = new TableInfo(tableName, tableComment, schema);
                tables.add(tableInfo);
            }
        }

        return tables;
    }

    /**
     * 获取指定表的所有列信息
     */
    public List<ColumnInfo> fetchColumns(DatabaseConfig config, TableInfo table) throws SQLException {
        List<ColumnInfo> columns = new ArrayList<>();
        DatabaseMetaData meta = connection.getMetaData();

        String catalog = null;
        String schemaPattern = null;

        if (config.getDbType() == DatabaseConfig.DbType.MYSQL) {
            catalog = config.getDatabaseName();
        } else {
            schemaPattern = "public";
        }

        // 获取主键列名集合
        List<String> primaryKeys = new ArrayList<>();
        try (ResultSet pkRs = meta.getPrimaryKeys(catalog, schemaPattern, table.getTableName())) {
            while (pkRs.next()) {
                primaryKeys.add(pkRs.getString("COLUMN_NAME"));
            }
        }

        // 获取列信息
        try (ResultSet colRs = meta.getColumns(catalog, schemaPattern, table.getTableName(), "%")) {
            while (colRs.next()) {
                ColumnInfo col = new ColumnInfo();
                col.setColumnName(colRs.getString("COLUMN_NAME"));
                col.setDataType(colRs.getString("TYPE_NAME"));
                col.setColumnSize(colRs.getInt("COLUMN_SIZE"));
                col.setDecimalDigits(colRs.getInt("DECIMAL_DIGITS"));
                col.setNullable(colRs.getInt("NULLABLE") == DatabaseMetaData.columnNullable);
                col.setRemarks(colRs.getString("REMARKS"));

                // 判断是否主键
                col.setPrimaryKey(primaryKeys.contains(col.getColumnName()));

                // 判断是否自增
                String isAutoIncrement = colRs.getString("IS_AUTOINCREMENT");
                col.setAutoIncrement("YES".equalsIgnoreCase(isAutoIncrement));

                columns.add(col);
            }
        }

        // MySQL 可能通过 IS_AUTOINCREMENT 获取不到，从 COLUMN_DEF 推断
        if (config.getDbType() == DatabaseConfig.DbType.MYSQL && columns.stream().noneMatch(ColumnInfo::isAutoIncrement)) {
            for (ColumnInfo col : columns) {
                if (col.isPrimaryKey() && "bigint".equalsIgnoreCase(col.getDataType()) || "int".equalsIgnoreCase(col.getDataType())) {
                    col.setAutoIncrement(true);
                    break;
                }
            }
        }

        // PostgreSQL: 检查 serial 类型
        if (config.getDbType() == DatabaseConfig.DbType.POSTGRESQL) {
            for (ColumnInfo col : columns) {
                String dt = col.getDataType().toLowerCase();
                if (dt.equals("serial") || dt.equals("bigserial") || dt.equals("smallserial")) {
                    col.setAutoIncrement(true);
                }
            }
        }

        return columns;
    }

    /**
     * 获取已建立的连接
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                // ignore
            }
            connection = null;
        }
    }

    /**
     * 是否已连接
     */
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}
