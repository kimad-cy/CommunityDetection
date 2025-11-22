module org.example.lpalgo {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.example.lpalgo to javafx.fxml;
    exports org.example.lpalgo;
}