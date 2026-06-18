module com.crudman.datasourcehelper {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;

    requires java.sql;
    requires com.zaxxer.hikari;

    requires mysql.connector.j;
    requires org.postgresql.jdbc;

    opens com.crudman.datasourcehelper to javafx.fxml;
    opens com.crudman.datasourcehelper.controller to javafx.fxml;
    opens com.crudman.datasourcehelper.model to javafx.fxml;
    exports com.crudman.datasourcehelper;
}
