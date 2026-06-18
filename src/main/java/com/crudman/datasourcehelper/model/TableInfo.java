package com.crudman.datasourcehelper.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据库表信息
 */
public class TableInfo {
    private String tableName;        // 原始表名
    private String tableComment;     // 表注释
    private String schema;           // 所属schema
    private List<ColumnInfo> columns; // 列信息列表
    private boolean selected;        // UI 选中状态

    public TableInfo() {
        this.columns = new ArrayList<>();
        this.selected = false;
    }

    public TableInfo(String tableName, String tableComment, String schema) {
        this.tableName = tableName;
        this.tableComment = tableComment;
        this.schema = schema;
        this.columns = new ArrayList<>();
        this.selected = false;
    }

    public void addColumn(ColumnInfo column) {
        this.columns.add(column);
    }

    public ColumnInfo getPrimaryKey() {
        return columns.stream()
                .filter(ColumnInfo::isPrimaryKey)
                .findFirst()
                .orElse(null);
    }

    // Getters and Setters
    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }

    public String getTableComment() { return tableComment; }
    public void setTableComment(String tableComment) { this.tableComment = tableComment; }

    public String getSchema() { return schema; }
    public void setSchema(String schema) { this.schema = schema; }

    public List<ColumnInfo> getColumns() { return columns; }
    public void setColumns(List<ColumnInfo> columns) { this.columns = columns; }

    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }

    @Override
    public String toString() {
        return tableName + (tableComment != null && !tableComment.isEmpty() ? "  (" + tableComment + ")" : "");
    }
}
