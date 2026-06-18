package com.crudman.datasourcehelper.model;

/**
 * 数据库列信息
 */
public class ColumnInfo {
    private String columnName;       // 列名
    private String dataType;         // 数据库类型 (int, varchar, datetime, etc.)
    private int columnSize;          // 列长度
    private int decimalDigits;       // 小数位数
    private boolean nullable;        // 是否可为空
    private boolean primaryKey;      // 是否主键
    private boolean autoIncrement;   // 是否自增
    private String remarks;          // 列注释

    public ColumnInfo() {}

    // Getters and Setters
    public String getColumnName() { return columnName; }
    public void setColumnName(String columnName) { this.columnName = columnName; }

    public String getDataType() { return dataType; }
    public void setDataType(String dataType) { this.dataType = dataType; }

    public int getColumnSize() { return columnSize; }
    public void setColumnSize(int columnSize) { this.columnSize = columnSize; }

    public int getDecimalDigits() { return decimalDigits; }
    public void setDecimalDigits(int decimalDigits) { this.decimalDigits = decimalDigits; }

    public boolean isNullable() { return nullable; }
    public void setNullable(boolean nullable) { this.nullable = nullable; }

    public boolean isPrimaryKey() { return primaryKey; }
    public void setPrimaryKey(boolean primaryKey) { this.primaryKey = primaryKey; }

    public boolean isAutoIncrement() { return autoIncrement; }
    public void setAutoIncrement(boolean autoIncrement) { this.autoIncrement = autoIncrement; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public boolean getNullable() { return nullable; }
    public boolean getPrimaryKey() { return primaryKey; }
    public boolean getAutoIncrement() { return autoIncrement; }
}
