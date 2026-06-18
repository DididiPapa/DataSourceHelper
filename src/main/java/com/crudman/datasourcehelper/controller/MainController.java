package com.crudman.datasourcehelper.controller;

import com.crudman.datasourcehelper.model.ColumnInfo;
import com.crudman.datasourcehelper.model.DatabaseConfig;
import com.crudman.datasourcehelper.model.GeneratorConfig;
import com.crudman.datasourcehelper.model.TableInfo;
import com.crudman.datasourcehelper.service.CodeGenerator;
import com.crudman.datasourcehelper.service.ConfigService;
import com.crudman.datasourcehelper.service.DatabaseService;
import com.crudman.datasourcehelper.util.NamingUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.stage.DirectoryChooser;
import javafx.util.Callback;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * 主界面控制器
 */
public class MainController implements Initializable {

    // ==================== FXML 绑定：连接面板 ====================
    @FXML private ComboBox<ConfigService.SavedConnection> savedConnCombo;
    @FXML private Button saveConnBtn;
    @FXML private Button deleteConnBtn;

    @FXML private ComboBox<DatabaseConfig.DbType> dbTypeCombo;
    @FXML private TextField hostField;
    @FXML private TextField portField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button testConnBtn;
    @FXML private Button connectBtn;
    @FXML private Button disconnectBtn;
    @FXML private Label connStatusLabel;

    // ==================== FXML 绑定：表选择面板 ====================
    @FXML private TextField tableSearchField;
    @FXML private CheckBox selectAllCheck;
    @FXML private Label tableCountLabel;
    @FXML private ComboBox<String> databaseCombo;
    @FXML private ListView<TableInfo> tableView;

    // ==================== FXML 绑定：列预览面板 ====================
    @FXML private Label selectedTableLabel;
    @FXML private TableView<ColumnInfo> columnTable;
    @FXML private TableColumn<ColumnInfo, String> colNameCol;
    @FXML private TableColumn<ColumnInfo, String> colTypeCol;
    @FXML private TableColumn<ColumnInfo, String> colJavaTypeCol;
    @FXML private TableColumn<ColumnInfo, Integer> colSizeCol;
    @FXML private TableColumn<ColumnInfo, Boolean> colPkCol;
    @FXML private TableColumn<ColumnInfo, Boolean> colAutoCol;
    @FXML private TableColumn<ColumnInfo, Boolean> colNullableCol;
    @FXML private TableColumn<ColumnInfo, String> colRemarkCol;

    // ==================== FXML 绑定：生成配置面板 ====================
    @FXML private TextField basePackageField;
    @FXML private TextField outputDirField;
    @FXML private Button browseDirBtn;
    @FXML private TextField authorField;
    @FXML private TextField tablePrefixField;
    @FXML private CheckBox useLombokCheck;
    @FXML private Button generateBtn;
    @FXML private TextArea logArea;

    // ==================== 服务实例 ====================
    private final DatabaseService databaseService = new DatabaseService();
    private final ConfigService configService = new ConfigService();
    private DatabaseConfig dbConfig;
    private final ObservableList<TableInfo> tableList = FXCollections.observableArrayList();
    private final ObservableList<TableInfo> filteredTableList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initConnectionPanel();
        loadSavedConnections();
        initTableList();
        initColumnTable();
        initGeneratePanel();

        // 搜索过滤
        tableSearchField.textProperty().addListener((obs, oldVal, newVal) -> filterTables(newVal));

        log("欢迎使用 DataSourceHelper 数据库代码生成器");
        log("请配置数据库连接信息，然后点击「连接数据库」");
    }

    // ==================== 连接面板初始化 ====================

    private void initConnectionPanel() {
        dbTypeCombo.getItems().setAll(DatabaseConfig.DbType.values());
        dbTypeCombo.setValue(DatabaseConfig.DbType.MYSQL);

        // 切换数据库类型时自动更新端口
        dbTypeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                portField.setText(String.valueOf(newVal.getDefaultPort()));
            }
        });
    }

    // ==================== 表列表初始化 ====================

    private void initTableList() {
        // 使用 CheckBoxListCell 实现带复选框的列表
        tableView.setCellFactory(CheckBoxListCell.forListView(
                item -> new javafx.beans.property.SimpleBooleanProperty(item.isSelected()),
                null));

        // 点击某个表时显示列信息
        tableView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                showColumnPreview(newVal);
            }
        });
    }

    // ==================== 列预览表格初始化 ====================

    private void initColumnTable() {
        colNameCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getColumnName()));
        colTypeCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getDataType()));
        colJavaTypeCol.setCellValueFactory(data ->
                new SimpleStringProperty(NamingUtils.columnToJavaType(data.getValue().getDataType())));
        colSizeCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleIntegerProperty(data.getValue().getColumnSize()).asObject());
        colPkCol.setCellValueFactory(data ->
                new SimpleBooleanProperty(data.getValue().isPrimaryKey()));
        colAutoCol.setCellValueFactory(data ->
                new SimpleBooleanProperty(data.getValue().isAutoIncrement()));
        colNullableCol.setCellValueFactory(data ->
                new SimpleBooleanProperty(data.getValue().isNullable()));
        colRemarkCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getRemarks()));
    }

    // ==================== 生成面板初始化 ====================

    private void initGeneratePanel() {
        // 默认输出目录：用户桌面下的 generated-code
        String desktopDir = System.getProperty("user.home") + File.separator + "Desktop"
                + File.separator + "generated-code";
        outputDirField.setText(desktopDir);
    }

    // ==================== 事件处理 ====================

    @FXML
    private void onTestConnection() {
        DatabaseConfig config = buildDbConfig();
        if (config == null) return;

        log("正在测试连接...");
        testConnBtn.setDisable(true);

        runAsync(() -> {
            String result = databaseService.testConnection(config);
            Platform.runLater(() -> {
                log(result);
                testConnBtn.setDisable(false);

                if (result.startsWith("连接成功")) {
                    connStatusLabel.setText("测试连接成功");
                    connStatusLabel.setStyle("-fx-text-fill: #27ae60;");
                } else {
                    connStatusLabel.setText("连接失败");
                    connStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
                }
            });
        });
    }

    @FXML
    private void onConnect() {
        DatabaseConfig config = buildDbConfig();
        if (config == null) return;

        // 先关闭旧连接
        databaseService.disconnect();
        tableList.clear();
        filteredTableList.clear();
        columnTable.getItems().clear();
        databaseCombo.getItems().clear();
        databaseCombo.setPromptText("加载中...");

        log("正在连接 " + config.getDbType().getDisplayName() + " 服务器: " + config.getHost() + "...");
        connectBtn.setDisable(true);
        testConnBtn.setDisable(true);
        connStatusLabel.setText("正在连接...");

        runAsync(() -> {
            try {
                databaseService.connect(config);
                this.dbConfig = config;

                // 获取数据库列表
                List<String> databases = databaseService.fetchDatabases(config);

                Platform.runLater(() -> {
                    databaseCombo.getItems().setAll(databases);
                    if (!databases.isEmpty()) {
                        databaseCombo.setPromptText("请选择数据库");
                    } else {
                        databaseCombo.setPromptText("未找到数据库");
                    }

                    connStatusLabel.setText("已连接 - " + config.getHost());
                    connStatusLabel.setStyle("-fx-text-fill: #27ae60;");
                    connectBtn.setDisable(false);
                    testConnBtn.setDisable(false);
                    disconnectBtn.setDisable(false);
                    selectedTableLabel.setText("请选择数据库查看表");
                    log("连接成功！找到 " + databases.size() + " 个数据库，请选择");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    connStatusLabel.setText("连接失败");
                    connStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
                    connectBtn.setDisable(false);
                    testConnBtn.setDisable(false);
                    databaseCombo.setPromptText("连接后可选择数据库");
                    log("连接失败: " + e.getMessage());
                });
            }
        });
    }

    @FXML
    private void onDatabaseSelected() {
        String selectedDb = databaseCombo.getValue();
        if (selectedDb == null || selectedDb.isEmpty() || dbConfig == null) return;

        dbConfig.setDatabaseName(selectedDb);
        log("切换到数据库: " + selectedDb);
        selectedTableLabel.setText("正在加载表...");

        runAsync(() -> {
            try {
                // 重新连接到选中的数据库
                databaseService.disconnect();
                databaseService.connect(dbConfig);
                List<TableInfo> tables = databaseService.fetchTables(dbConfig);

                Platform.runLater(() -> {
                    tableList.setAll(tables);
                    filteredTableList.setAll(tables);
                    tableView.setItems(filteredTableList);
                    updateTableCount();
                    columnTable.getItems().clear();
                    selectAllCheck.setSelected(false);

                    connStatusLabel.setText("已连接 - " + dbConfig.getHost() + " / " + selectedDb);
                    selectedTableLabel.setText("共加载 " + tables.size() + " 张表，请选择表查看列信息");
                    log("数据库 " + selectedDb + "：加载 " + tables.size() + " 张表");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    selectedTableLabel.setText("加载失败: " + e.getMessage());
                    log("切换数据库失败: " + e.getMessage());
                });
            }
        });
    }

    @FXML
    private void onDisconnect() {
        databaseService.disconnect();
        tableList.clear();
        filteredTableList.clear();
        tableView.setItems(filteredTableList);
        columnTable.getItems().clear();
        selectedTableLabel.setText("请选择一张表查看列信息");
        updateTableCount();

        databaseCombo.getItems().clear();
        databaseCombo.setPromptText("连接后可选择数据库");

        connStatusLabel.setText("未连接");
        connStatusLabel.setStyle("-fx-text-fill: #7f8c8d;");
        disconnectBtn.setDisable(true);
        log("已断开连接");
    }

    // ==================== 已保存连接配置 ====================

    private void loadSavedConnections() {
        List<ConfigService.SavedConnection> profiles = configService.loadAll();
        savedConnCombo.getItems().setAll(profiles);
        log("已加载 " + profiles.size() + " 个已保存的连接配置");
    }

    @FXML
    private void onSavedConnectionSelected() {
        ConfigService.SavedConnection selected = savedConnCombo.getValue();
        if (selected == null) return;

        DatabaseConfig config = selected.getConfig();
        dbTypeCombo.setValue(config.getDbType());
        hostField.setText(config.getHost());
        portField.setText(String.valueOf(config.getPort()));
        usernameField.setText(config.getUsername());
        passwordField.setText(config.getPassword());

        // 如果保存配置时有数据库名也填上
        if (config.getDatabaseName() != null && !config.getDatabaseName().isEmpty()) {
            // 数据库名在连接后通过 databaseCombo 选择，这里暂存到 dbConfig
        }

        deleteConnBtn.setDisable(false);
        log("已加载配置: " + selected.getName());
    }

    @FXML
    private void onSaveConnection() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("保存连接配置");
        dialog.setHeaderText("请输入此连接配置的名称");
        dialog.setContentText("名称:");

        dialog.showAndWait().ifPresent(name -> {
            if (name.trim().isEmpty()) return;

            DatabaseConfig config = buildDbConfig();
            if (config == null) return;

            // 如果已连接且选择了数据库，保留数据库名
            if (dbConfig != null && dbConfig.getDatabaseName() != null) {
                config.setDatabaseName(dbConfig.getDatabaseName());
            }

            ConfigService.SavedConnection sc = new ConfigService.SavedConnection();
            sc.setName(name.trim());
            sc.setConfig(config);

            try {
                configService.save(sc);
                loadSavedConnections();
                log("连接配置已保存: " + name.trim());
            } catch (IOException e) {
                log("保存失败: " + e.getMessage());
            }
        });
    }

    @FXML
    private void onDeleteConnection() {
        ConfigService.SavedConnection selected = savedConnCombo.getValue();
        if (selected == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("删除连接配置");
        confirm.setHeaderText("确定要删除 \"" + selected.getName() + "\" 吗？");
        confirm.setContentText("此操作不可撤销");

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    configService.delete(selected.getName());
                    savedConnCombo.setValue(null);
                    deleteConnBtn.setDisable(true);
                    loadSavedConnections();
                    log("已删除配置: " + selected.getName());
                } catch (IOException e) {
                    log("删除失败: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void onSelectAll() {
        boolean selectAll = selectAllCheck.isSelected();
        for (TableInfo table : filteredTableList) {
            table.setSelected(selectAll);
        }
        // CheckBoxListCell 需要强制刷新才能看到变化
        tableView.refresh();
        updateTableCount();
    }

    @FXML
    private void onBrowseOutputDir() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("选择代码输出目录");
        File dir = chooser.showDialog(outputDirField.getScene().getWindow());
        if (dir != null) {
            outputDirField.setText(dir.getAbsolutePath());
            log("输出目录: " + dir.getAbsolutePath());
        }
    }

    @FXML
    private void onGenerate() {
        // 收集选中的表（从 tableList 和 filteredTableList 中合并）
        final List<TableInfo> selectedTables = tableList.stream()
                .filter(TableInfo::isSelected)
                .toList();

        if (selectedTables.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "提示", "请至少选择一张表");
            return;
        }

        final String outputDir = outputDirField.getText().trim();
        if (outputDir.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "提示", "请选择输出目录");
            return;
        }

        final GeneratorConfig genConfig = buildGeneratorConfig();
        final CodeGenerator generator = new CodeGenerator(genConfig);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        log("========== " + timestamp + " 开始生成 ==========");

        generateBtn.setDisable(true);

        runAsync(() -> {
            try {
                int totalFiles = 0;

                // 先加载每张表的列信息
                for (TableInfo table : selectedTables) {
                    Platform.runLater(() -> log("正在处理表: " + table.getTableName() + " ..."));

                    List<ColumnInfo> columns = databaseService.fetchColumns(dbConfig, table);
                    table.setColumns(columns);
                }

                // 生成代码
                for (TableInfo table : selectedTables) {
                    Map<String, String> files = generator.generateAll(table);
                    int count = generator.writeToDisk(files);
                    totalFiles += count;

                    String className = NamingUtils.toClassName(table.getTableName(), genConfig.getTablePrefix());
                    int finalCount = count;
                    Platform.runLater(() ->
                            log("  ✅ " + table.getTableName() + " → " + className
                                    + " (生成 " + finalCount + " 个文件)"));
                }

                int finalTotal = totalFiles;
                Platform.runLater(() -> {
                    log("========== 生成完成！共生成 " + finalTotal + " 个文件 ==========");
                    log("输出目录: " + outputDir);
                    generateBtn.setDisable(false);
                    showAlert(Alert.AlertType.INFORMATION, "生成完成",
                            "成功生成 " + finalTotal + " 个文件！\n输出目录: " + outputDir);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    log("生成失败: " + e.getMessage());
                    generateBtn.setDisable(false);
                    showAlert(Alert.AlertType.ERROR, "生成失败", e.getMessage());
                });
            }
        });
    }

    // ==================== 辅助方法 ====================

    /**
     * 从 UI 构建数据库配置
     */
    private DatabaseConfig buildDbConfig() {
        if (dbTypeCombo.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "提示", "请选择数据库类型");
            return null;
        }
        if (hostField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "提示", "请输入主机地址");
            return null;
        }

        DatabaseConfig config = new DatabaseConfig();
        config.setDbType(dbTypeCombo.getValue());
        config.setHost(hostField.getText().trim());
        config.setUsername(usernameField.getText().trim());
        config.setPassword(passwordField.getText());

        try {
            config.setPort(Integer.parseInt(portField.getText().trim()));
        } catch (NumberFormatException e) {
            config.setPort(config.getDbType().getDefaultPort());
        }

        return config;
    }

    /**
     * 从 UI 构建生成器配置
     */
    private GeneratorConfig buildGeneratorConfig() {
        GeneratorConfig config = new GeneratorConfig();
        config.setBasePackage(basePackageField.getText().trim());
        config.setOutputDir(outputDirField.getText().trim());
        config.setAuthor(authorField.getText().trim());
        config.setTablePrefix(tablePrefixField.getText().trim());
        config.setUseLombok(useLombokCheck.isSelected());
        return config;
    }

    /**
     * 显示指定表的列信息预览
     */
    private void showColumnPreview(TableInfo table) {
        selectedTableLabel.setText("表: " + table.getTableName()
                + (table.getTableComment() != null ? " (" + table.getTableComment() + ")" : ""));

        // 如果还没加载列信息，从数据库加载
        if (table.getColumns().isEmpty() && dbConfig != null && databaseService.isConnected()) {
            runAsync(() -> {
                try {
                    List<ColumnInfo> columns = databaseService.fetchColumns(dbConfig, table);
                    table.setColumns(columns);
                    Platform.runLater(() -> {
                        columnTable.setItems(FXCollections.observableArrayList(columns));
                    });
                } catch (SQLException e) {
                    Platform.runLater(() -> log("获取列信息失败: " + e.getMessage()));
                }
            });
        } else {
            columnTable.setItems(FXCollections.observableArrayList(table.getColumns()));
        }
    }

    /**
     * 按搜索关键字过滤表列表
     */
    private void filterTables(String keyword) {
        filteredTableList.clear();
        if (keyword == null || keyword.trim().isEmpty()) {
            filteredTableList.addAll(tableList);
        } else {
            String lower = keyword.toLowerCase().trim();
            filteredTableList.addAll(tableList.stream()
                    .filter(t -> t.getTableName().toLowerCase().contains(lower)
                            || (t.getTableComment() != null
                            && t.getTableComment().toLowerCase().contains(lower)))
                    .collect(Collectors.toList()));
        }
        tableView.setItems(filteredTableList);
        updateTableCount();
    }

    /**
     * 更新表数量标签
     */
    private void updateTableCount() {
        long selected = tableList.stream().filter(TableInfo::isSelected).count();
        tableCountLabel.setText("共 " + filteredTableList.size() + " 张表，已选 " + selected + " 张");
    }

    /**
     * 在后台线程运行任务
     */
    private void runAsync(Runnable runnable) {
        new Thread(runnable).start();
    }

    /**
     * 日志输出
     */
    private void log(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        logArea.appendText("[" + timestamp + "] " + message + "\n");
    }

    /**
     * 弹窗提示
     */
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
