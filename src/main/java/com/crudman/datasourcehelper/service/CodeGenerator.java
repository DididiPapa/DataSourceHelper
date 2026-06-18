package com.crudman.datasourcehelper.service;

import com.crudman.datasourcehelper.model.ColumnInfo;
import com.crudman.datasourcehelper.model.GeneratorConfig;
import com.crudman.datasourcehelper.model.TableInfo;
import com.crudman.datasourcehelper.util.NamingUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 代码生成引擎
 * 使用 Java Text Blocks 作为内联模板，生成 Spring Boot + MyBatis-Plus 代码
 */
public class CodeGenerator {

    private final GeneratorConfig config;

    public CodeGenerator(GeneratorConfig config) {
        this.config = config;
    }

    /**
     * 对单个表生成所有代码文件
     * @return Map<文件路径, 文件内容>
     */
    public Map<String, String> generateAll(TableInfo table) {
        Map<String, String> files = new LinkedHashMap<>();

        String className = NamingUtils.toClassName(table.getTableName(), config.getTablePrefix());
        String packagePath = config.getBasePackage().replace('.', '/');

        // Entity
        String entityContent = generateEntity(table, className);
        files.put(packagePath + "/entity/" + className + ".java", entityContent);

        // Mapper
        String mapperContent = generateMapper(table, className);
        files.put(packagePath + "/mapper/" + className + "Mapper.java", mapperContent);

        // Service
        String serviceContent = generateService(table, className);
        files.put(packagePath + "/service/" + className + "Service.java", serviceContent);

        // ServiceImpl
        String serviceImplContent = generateServiceImpl(table, className);
        files.put(packagePath + "/service/impl/" + className + "ServiceImpl.java", serviceImplContent);

        // Controller
        String controllerContent = generateController(table, className);
        files.put(packagePath + "/controller/" + className + "Controller.java", controllerContent);

        return files;
    }

    /**
     * 将生成的文件写入磁盘
     * @return 写入的文件数量
     */
    public int writeToDisk(Map<String, String> files) throws IOException {
        int count = 0;
        Path basePath = Paths.get(config.getOutputDir());

        for (Map.Entry<String, String> entry : files.entrySet()) {
            Path filePath = basePath.resolve(entry.getKey());
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, entry.getValue());
            count++;
        }

        return count;
    }

    // ==================== Entity 生成 ====================

    public String generateEntity(TableInfo table, String className) {
        StringBuilder sb = new StringBuilder();

        // package & imports
        sb.append("package ").append(config.getBasePackage()).append(".entity;\n\n");

        // 收集需要导入的类型
        Set<String> imports = new LinkedHashSet<>();
        imports.add("com.baomidou.mybatisplus.annotation.IdType");
        imports.add("com.baomidou.mybatisplus.annotation.TableField");
        imports.add("com.baomidou.mybatisplus.annotation.TableId");
        imports.add("com.baomidou.mybatisplus.annotation.TableName");
        if (config.isUseLombok()) {
            imports.add("lombok.Data");
        }

        for (ColumnInfo col : table.getColumns()) {
            String javaType = NamingUtils.columnToJavaType(col.getDataType());
            String importPath = NamingUtils.getTypeImport(javaType);
            if (importPath != null) {
                imports.add(importPath);
            }
        }

        // 排序并写入 imports
        imports.stream().sorted().forEach(imp -> sb.append("import ").append(imp).append(";\n"));

        sb.append("\n");

        // 类注释
        String comment = NamingUtils.getCommentOrDefault(table.getTableComment(), table.getTableName());
        sb.append("/**\n");
        sb.append(" * ").append(comment).append("\n");
        if (config.getAuthor() != null && !config.getAuthor().isEmpty()) {
            sb.append(" * @author ").append(config.getAuthor()).append("\n");
        }
        sb.append(" */\n");

        // @Data 或手写 getter/setter
        if (config.isUseLombok()) {
            sb.append("@Data\n");
        }

        // @TableName
        sb.append("@TableName(\"").append(table.getTableName()).append("\")\n");

        // class
        sb.append("public class ").append(className).append(" {\n");

        // 生成字段
        for (ColumnInfo col : table.getColumns()) {
            String javaType = NamingUtils.columnToJavaType(col.getDataType());
            String fieldName = NamingUtils.toCamelCase(col.getColumnName());
            String colComment = col.getRemarks();

            // 字段注释
            if (colComment != null && !colComment.isEmpty()) {
                sb.append("    /** ").append(colComment).append(" */\n");
            }

            // @TableId 或 @TableField
            if (col.isPrimaryKey()) {
                if (col.isAutoIncrement()) {
                    sb.append("    @TableId(value = \"").append(col.getColumnName())
                            .append("\", type = IdType.AUTO)\n");
                } else {
                    sb.append("    @TableId(value = \"").append(col.getColumnName()).append("\")\n");
                }
            } else {
                sb.append("    @TableField(\"").append(col.getColumnName()).append("\")\n");
            }

            sb.append("    private ").append(javaType).append(" ").append(fieldName).append(";\n");
        }

        // 不启用 Lombok 时生成 getter/setter
        if (!config.isUseLombok()) {
            sb.append("\n");
            for (ColumnInfo col : table.getColumns()) {
                String javaType = NamingUtils.columnToJavaType(col.getDataType());
                String fieldName = NamingUtils.toCamelCase(col.getColumnName());
                String getterName = "get" + NamingUtils.toPascalCase(col.getColumnName());
                String setterName = "set" + NamingUtils.toPascalCase(col.getColumnName());

                sb.append("    public ").append(javaType).append(" ").append(getterName).append("() {\n");
                sb.append("        return ").append(fieldName).append(";\n");
                sb.append("    }\n\n");

                sb.append("    public void ").append(setterName).append("(").append(javaType)
                        .append(" ").append(fieldName).append(") {\n");
                sb.append("        this.").append(fieldName).append(" = ").append(fieldName).append(";\n");
                sb.append("    }\n\n");
            }
        }

        sb.append("}\n");
        return sb.toString();
    }

    // ==================== Mapper 生成 ====================

    public String generateMapper(TableInfo table, String className) {
        String entityPackage = config.getBasePackage() + ".entity." + className;

        return """
                package %s.mapper;

                import com.baomidou.mybatisplus.core.mapper.BaseMapper;
                import %s;
                import org.apache.ibatis.annotations.Mapper;

                /**
                 * %s Mapper 接口
                %s
                 */
                @Mapper
                public interface %sMapper extends BaseMapper<%s> {
                }
                """.formatted(
                config.getBasePackage(),
                entityPackage,
                NamingUtils.getCommentOrDefault(table.getTableComment(), className),
                config.getAuthor() != null && !config.getAuthor().isEmpty() ? " * @author " + config.getAuthor() + "\n" : "",
                className,
                className
        );
    }

    // ==================== Service 生成 ====================

    public String generateService(TableInfo table, String className) {
        String entityPackage = config.getBasePackage() + ".entity." + className;

        return """
                package %s.service;

                import com.baomidou.mybatisplus.extension.service.IService;
                import %s;

                /**
                 * %s Service 接口
                %s
                 */
                public interface %sService extends IService<%s> {
                }
                """.formatted(
                config.getBasePackage(),
                entityPackage,
                NamingUtils.getCommentOrDefault(table.getTableComment(), className),
                config.getAuthor() != null && !config.getAuthor().isEmpty() ? " * @author " + config.getAuthor() + "\n" : "",
                className,
                className
        );
    }

    // ==================== ServiceImpl 生成 ====================

    public String generateServiceImpl(TableInfo table, String className) {
        String entityPackage = config.getBasePackage() + ".entity." + className;
        String mapperPackage = config.getBasePackage() + ".mapper." + className + "Mapper";
        String servicePackage = config.getBasePackage() + ".service." + className + "Service";

        return """
                package %s.service.impl;

                import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
                import %s;
                import %s;
                import %s;
                import org.springframework.stereotype.Service;

                /**
                 * %s Service 实现类
                %s
                 */
                @Service
                public class %sServiceImpl extends ServiceImpl<%sMapper, %s> implements %sService {
                }
                """.formatted(
                config.getBasePackage(),
                entityPackage,
                mapperPackage,
                servicePackage,
                NamingUtils.getCommentOrDefault(table.getTableComment(), className),
                config.getAuthor() != null && !config.getAuthor().isEmpty() ? " * @author " + config.getAuthor() + "\n" : "",
                className,
                className, className,
                className
        );
    }

    // ==================== Controller 生成 ====================

    public String generateController(TableInfo table, String className) {
        String entityPackage = config.getBasePackage() + ".entity." + className;
        String servicePackage = config.getBasePackage() + ".service." + className + "Service";
        String varName = NamingUtils.toVariableName(className);
        String serviceVarName = varName + "Service";
        String urlMapping = "/" + NamingUtils.toPackageName(table.getTableName().replace("_", "-"));

        ColumnInfo pk = table.getPrimaryKey();
        String pkType = pk != null ? NamingUtils.columnToJavaType(pk.getDataType()) : "Long";
        String pkName = pk != null ? NamingUtils.toCamelCase(pk.getColumnName()) : "id";

        StringBuilder sb = new StringBuilder();

        sb.append("package ").append(config.getBasePackage()).append(".controller;\n\n");

        sb.append("import ").append(entityPackage).append(";\n");
        sb.append("import ").append(servicePackage).append(";\n");
        sb.append("import org.springframework.beans.factory.annotation.Autowired;\n");
        sb.append("import org.springframework.web.bind.annotation.*;\n\n");
        sb.append("import java.util.List;\n\n");

        String comment = NamingUtils.getCommentOrDefault(table.getTableComment(), className);
        sb.append("/**\n");
        sb.append(" * ").append(comment).append(" Controller\n");
        if (config.getAuthor() != null && !config.getAuthor().isEmpty()) {
            sb.append(" * @author ").append(config.getAuthor()).append("\n");
        }
        sb.append(" */\n");

        sb.append("@RestController\n");
        sb.append("@RequestMapping(\"").append(urlMapping).append("\")\n");
        sb.append("public class ").append(className).append("Controller {\n\n");

        sb.append("    @Autowired\n");
        sb.append("    private ").append(className).append("Service ").append(serviceVarName).append(";\n\n");

        // GET /list - 分页查询列表
        sb.append("    /** 查询列表 */\n");
        sb.append("    @GetMapping(\"/list\")\n");
        sb.append("    public List<").append(className).append("> list() {\n");
        sb.append("        return ").append(serviceVarName).append(".list();\n");
        sb.append("    }\n\n");

        // GET /get - 根据ID查询 (RequestParam)
        sb.append("    /** 根据ID查询 */\n");
        sb.append("    @GetMapping(\"/get\")\n");
        sb.append("    public ").append(className).append(" getById(@RequestParam(\"")
                .append(pkName).append("\") ").append(pkType).append(" ").append(pkName).append(") {\n");
        sb.append("        return ").append(serviceVarName).append(".getById(").append(pkName).append(");\n");
        sb.append("    }\n\n");

        // POST /save - 新增
        sb.append("    /** 新增 */\n");
        sb.append("    @PostMapping(\"/save\")\n");
        sb.append("    public boolean save(@RequestBody ").append(className).append(" ").append(varName).append(") {\n");
        sb.append("        return ").append(serviceVarName).append(".save(").append(varName).append(");\n");
        sb.append("    }\n\n");

        // POST /update - 更新
        sb.append("    /** 更新 */\n");
        sb.append("    @PostMapping(\"/update\")\n");
        sb.append("    public boolean update(@RequestBody ").append(className).append(" ").append(varName).append(") {\n");
        sb.append("        return ").append(serviceVarName).append(".updateById(").append(varName).append(");\n");
        sb.append("    }\n\n");

        // POST /delete - 删除 (RequestParam)
        sb.append("    /** 删除 */\n");
        sb.append("    @PostMapping(\"/delete\")\n");
        sb.append("    public boolean delete(@RequestParam(\"")
                .append(pkName).append("\") ").append(pkType).append(" ").append(pkName).append(") {\n");
        sb.append("        return ").append(serviceVarName).append(".removeById(").append(pkName).append(");\n");
        sb.append("    }\n\n");

        sb.append("}\n");
        return sb.toString();
    }
}
