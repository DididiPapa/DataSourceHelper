package com.crudman.datasourcehelper.service;

import com.crudman.datasourcehelper.model.DatabaseConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * 数据库连接配置持久化服务
 * 存储位置: src/main/resources/com/crudman/datasourcehelper/connections.properties
 */
public class ConfigService {

    private static final Path STORE_FILE = Paths.get(
            "src", "main", "resources", "com", "crudman", "datasourcehelper", "connections.properties");

    /**
     * 加载所有已保存的连接配置
     */
    public List<SavedConnection> loadAll() {
        List<SavedConnection> profiles = new ArrayList<>();
        if (!Files.exists(STORE_FILE)) return profiles;

        try {
            Properties props = new Properties();
            props.load(Files.newBufferedReader(STORE_FILE));

            // 读取配置文件数量
            int count = Integer.parseInt(props.getProperty("count", "0"));

            for (int i = 0; i < count; i++) {
                String prefix = "profile." + i + ".";
                String name = props.getProperty(prefix + "name");
                if (name == null || name.isEmpty()) continue;

                SavedConnection sc = new SavedConnection();
                sc.setName(name);
                sc.setConfig(buildConfig(props, prefix));
                profiles.add(sc);
            }
        } catch (Exception e) {
            // 配置文件损坏时忽略
        }
        return profiles;
    }

    /**
     * 保存所有连接配置
     */
    public void saveAll(List<SavedConnection> profiles) throws IOException {
        Files.createDirectories(STORE_FILE.getParent());

        Properties props = new Properties();
        props.setProperty("count", String.valueOf(profiles.size()));

        for (int i = 0; i < profiles.size(); i++) {
            String prefix = "profile." + i + ".";
            SavedConnection sc = profiles.get(i);
            props.setProperty(prefix + "name", sc.getName());
            writeConfig(props, prefix, sc.getConfig());
        }

        props.store(Files.newBufferedWriter(STORE_FILE), "DataSourceHelper - 数据库连接配置");
    }

    /**
     * 保存或更新单个连接配置（按名称去重）
     */
    public void save(SavedConnection connection) throws IOException {
        List<SavedConnection> profiles = loadAll();
        boolean found = false;
        for (int i = 0; i < profiles.size(); i++) {
            if (profiles.get(i).getName().equals(connection.getName())) {
                profiles.set(i, connection);
                found = true;
                break;
            }
        }
        if (!found) {
            profiles.add(connection);
        }
        saveAll(profiles);
    }

    /**
     * 按名称删除连接配置
     */
    public void delete(String name) throws IOException {
        List<SavedConnection> profiles = loadAll();
        profiles.removeIf(p -> p.getName().equals(name));
        saveAll(profiles);
    }

    private DatabaseConfig buildConfig(Properties props, String prefix) {
        DatabaseConfig config = new DatabaseConfig();
        // DbType
        String dbTypeStr = props.getProperty(prefix + "dbType", "MYSQL");
        try {
            config.setDbType(DatabaseConfig.DbType.valueOf(dbTypeStr));
        } catch (IllegalArgumentException e) {
            config.setDbType(DatabaseConfig.DbType.MYSQL);
        }
        config.setHost(props.getProperty(prefix + "host", "localhost"));
        try {
            config.setPort(Integer.parseInt(props.getProperty(prefix + "port", "3306")));
        } catch (NumberFormatException e) {
            config.setPort(3306);
        }
        config.setDatabaseName(props.getProperty(prefix + "databaseName", ""));
        config.setUsername(props.getProperty(prefix + "username", "root"));
        config.setPassword(props.getProperty(prefix + "password", ""));
        return config;
    }

    private void writeConfig(Properties props, String prefix, DatabaseConfig config) {
        props.setProperty(prefix + "dbType", config.getDbType().name());
        props.setProperty(prefix + "host", config.getHost());
        props.setProperty(prefix + "port", String.valueOf(config.getPort()));
        props.setProperty(prefix + "databaseName",
                config.getDatabaseName() != null ? config.getDatabaseName() : "");
        props.setProperty(prefix + "username", config.getUsername());
        props.setProperty(prefix + "password",
                config.getPassword() != null ? config.getPassword() : "");
    }

    // ==================== 内部数据类 ====================

    public static class SavedConnection {
        private String name;
        private DatabaseConfig config = new DatabaseConfig();

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public DatabaseConfig getConfig() { return config; }
        public void setConfig(DatabaseConfig config) { this.config = config; }

        @Override
        public String toString() { return name; }
    }
}
