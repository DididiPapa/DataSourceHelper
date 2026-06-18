package com.crudman.datasourcehelper.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

/**
 * 命名转换工具类
 */
public class NamingUtils {

    /** 数据库类型 → Java 类型映射 */
    private static final Map<String, String> TYPE_MAPPING = Map.ofEntries(
            // 整数类型
            Map.entry("tinyint", "Integer"),
            Map.entry("smallint", "Integer"),
            Map.entry("mediumint", "Integer"),
            Map.entry("int", "Integer"),
            Map.entry("integer", "Integer"),
            Map.entry("bigint", "Long"),
            Map.entry("int2", "Integer"),
            Map.entry("int4", "Integer"),
            Map.entry("int8", "Long"),
            Map.entry("serial", "Long"),
            Map.entry("bigserial", "Long"),
            Map.entry("smallserial", "Integer"),

            // 浮点类型
            Map.entry("float", "Float"),
            Map.entry("double", "Double"),
            Map.entry("real", "Double"),
            Map.entry("numeric", "BigDecimal"),
            Map.entry("decimal", "BigDecimal"),
            Map.entry("money", "BigDecimal"),

            // 字符串类型
            Map.entry("varchar", "String"),
            Map.entry("char", "String"),
            Map.entry("character varying", "String"),
            Map.entry("character", "String"),
            Map.entry("text", "String"),
            Map.entry("tinytext", "String"),
            Map.entry("mediumtext", "String"),
            Map.entry("longtext", "String"),
            Map.entry("enum", "String"),
            Map.entry("set", "String"),
            Map.entry("json", "String"),
            Map.entry("jsonb", "String"),
            Map.entry("uuid", "String"),

            // 日期时间类型
            Map.entry("date", "LocalDate"),
            Map.entry("datetime", "LocalDateTime"),
            Map.entry("timestamp", "LocalDateTime"),
            Map.entry("timestamptz", "LocalDateTime"),
            Map.entry("time", "LocalTime"),
            Map.entry("timetz", "LocalTime"),
            Map.entry("year", "Integer"),

            // 布尔类型
            Map.entry("bit", "Boolean"),
            Map.entry("bool", "Boolean"),
            Map.entry("boolean", "Boolean"),

            // 二进制类型
            Map.entry("blob", "byte[]"),
            Map.entry("tinyblob", "byte[]"),
            Map.entry("mediumblob", "byte[]"),
            Map.entry("longblob", "byte[]"),
            Map.entry("bytea", "byte[]"),

            // 其他
            Map.entry("oid", "Long")
    );

    /** 需要 import 的 Java 类型对应的包 */
    private static final Map<String, String> TYPE_IMPORTS = Map.of(
            "BigDecimal", "java.math.BigDecimal",
            "LocalDate", "java.time.LocalDate",
            "LocalDateTime", "java.time.LocalDateTime",
            "LocalTime", "java.time.LocalTime"
    );

    /**
     * 数据库数据类型 → Java 类型
     */
    public static String columnToJavaType(String dbType) {
        if (dbType == null) return "String";
        String lower = dbType.toLowerCase().replaceAll("\\(.*\\)", "").trim();
        return TYPE_MAPPING.getOrDefault(lower, "String");
    }

    /**
     * 获取类型所需的 import 包路径，不需要 import 的返回 null
     */
    public static String getTypeImport(String javaType) {
        return TYPE_IMPORTS.get(javaType);
    }

    /**
     * 下划线命名 → 驼峰命名 (首字母小写)
     * 例如: user_name → userName
     */
    public static String toCamelCase(String input) {
        if (input == null || input.isEmpty()) return input;

        StringBuilder result = new StringBuilder();
        boolean nextUpper = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '_' || c == '-') {
                nextUpper = true;
            } else if (nextUpper) {
                result.append(Character.toUpperCase(c));
                nextUpper = false;
            } else {
                result.append(Character.toLowerCase(c));
            }
        }

        return result.toString();
    }

    /**
     * 下划线命名 → 帕斯卡命名 (首字母大写)
     * 例如: user_name → UserName
     */
    public static String toPascalCase(String input) {
        String camel = toCamelCase(input);
        if (camel == null || camel.isEmpty()) return camel;
        return Character.toUpperCase(camel.charAt(0)) + camel.substring(1);
    }

    /**
     * 获取类名 (去除表前缀后转帕斯卡)
     * 例如: (t_user, "t_") → User
     */
    public static String toClassName(String tableName, String prefix) {
        String name = tableName;
        if (prefix != null && !prefix.isEmpty() && name.toLowerCase().startsWith(prefix.toLowerCase())) {
            name = name.substring(prefix.length());
        }
        return toPascalCase(name);
    }

    /**
     * 转换为小写包名
     */
    public static String toPackageName(String input) {
        if (input == null || input.isEmpty()) return input;
        return input.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    /**
     * 获取实体变量名 (首字母小写的驼峰)
     * 例如: User → user, UserInfo → userInfo
     */
    public static String toVariableName(String className) {
        if (className == null || className.isEmpty()) return className;
        return Character.toLowerCase(className.charAt(0)) + className.substring(1);
    }

    /**
     * 获取表注释，若无则用表名
     */
    public static String getCommentOrDefault(String comment, String tableName) {
        return (comment != null && !comment.isEmpty()) ? comment : tableName;
    }
}
