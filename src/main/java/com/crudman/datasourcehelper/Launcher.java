package com.crudman.datasourcehelper;

import javafx.application.Application;

/**
 * 启动器类（避免 JavaFX 模块系统的直接启动问题）
 */
public class Launcher {
    public static void main(String[] args) {
        Application.launch(App.class, args);
    }
}
