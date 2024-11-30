package org.example.clientsevermsgexample;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class ClientViewController {
    @FXML
    public Button button_send;
    @FXML
    public TextField tf_message;
    @FXML
    public VBox vbox_messages;
    @FXML
    public ScrollPane sp_main;
}
