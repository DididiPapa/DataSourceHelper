package com.crudman.datasourcehelper;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * DataSourceHelper - 数据库代码生成器
 * 连接数据库，选择表，自动生成 Spring Boot + MyBatis-Plus 代码
 */
public class App extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1200, 800);
        stage.setTitle("DataSourceHelper - 数据库代码生成器");
        stage.getIcons().add(new Image(App.class.getResourceAsStream("favicon.png")));
        stage.setScene(scene);
        stage.setMinWidth(1000);
        stage.setMinHeight(700);
        stage.show();
    }
}
