module com.example.textreaderforkotlin {
    requires javafx.controls;
    requires javafx.fxml;
    requires kotlin.stdlib;

    requires com.dlsc.formsfx;
    requires poi;
    requires org.apache.commons.csv;
    requires java.mail;
    requires java.net.http;
    requires java.desktop;
    requires okhttp3;
    requires poi.ooxml;
    requires poi.ooxml.schemas;
    requires mp3spi;

    opens com.example.textreaderforkotlin to javafx.fxml;
    exports com.example.textreaderforkotlin;
}