package com.crudman.datasourcehelper.model;

/**
 * 代码生成器配置
 */
public class GeneratorConfig {
    private String basePackage = "com.example";     // 基础包名
    private String outputDir = "";                   // 输出目录
    private String author = "";                      // 作者
    private String tablePrefix = "";                 // 表前缀（生成时会去除）
    private boolean useLombok = true;                // 是否使用 Lombok @Data
    private boolean generateController = true;       // 是否生成 Controller
    private boolean generateService = true;          // 是否生成 Service
    private boolean generateServiceImpl = true;      // 是否生成 ServiceImpl
    private boolean generateMapper = true;           // 是否生成 Mapper

    public GeneratorConfig() {}

    // Getters and Setters
    public String getBasePackage() { return basePackage; }
    public void setBasePackage(String basePackage) { this.basePackage = basePackage; }

    public String getOutputDir() { return outputDir; }
    public void setOutputDir(String outputDir) { this.outputDir = outputDir; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getTablePrefix() { return tablePrefix; }
    public void setTablePrefix(String tablePrefix) { this.tablePrefix = tablePrefix; }

    public boolean isUseLombok() { return useLombok; }
    public void setUseLombok(boolean useLombok) { this.useLombok = useLombok; }

    public boolean isGenerateController() { return generateController; }
    public void setGenerateController(boolean generateController) { this.generateController = generateController; }

    public boolean isGenerateService() { return generateService; }
    public void setGenerateService(boolean generateService) { this.generateService = generateService; }

    public boolean isGenerateServiceImpl() { return generateServiceImpl; }
    public void setGenerateServiceImpl(boolean generateServiceImpl) { this.generateServiceImpl = generateServiceImpl; }

    public boolean isGenerateMapper() { return generateMapper; }
    public void setGenerateMapper(boolean generateMapper) { this.generateMapper = generateMapper; }

    public boolean getUseLombok() { return useLombok; }
    public boolean getGenerateController() { return generateController; }
    public boolean getGenerateService() { return generateService; }
    public boolean getGenerateServiceImpl() { return generateServiceImpl; }
    public boolean getGenerateMapper() { return generateMapper; }
}
