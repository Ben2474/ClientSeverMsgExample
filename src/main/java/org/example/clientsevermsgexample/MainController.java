package org.example.clientsevermsgexample;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.*;
import java.net.*;
import java.util.ResourceBundle;

import static java.lang.Thread.sleep;

public class MainController implements Initializable {
    @FXML
    private ComboBox dropdownPort;
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private boolean isRunning = true;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        dropdownPort.getItems().addAll("7",     // ping
                "13",     // daytime
                "21",     // ftp
                "23",     // telnet
                "71",     // finger
                "80",     // http
                "119",     // nntp (news)
                "161"      // snmp);
        );
    }

    @FXML
    private Button clearBtn;
    @FXML
    private TextArea resultArea;
    @FXML
    private Label server_lbl;
    @FXML
    private Button testBtn;
    @FXML
    private Label test_lbl;
    @FXML
    private TextField urlName;
    @FXML
    public Button user1_client;
    @FXML
    public Button user2_server;
    Socket socket1;
    Label lb122, lb12;
    TextField msgText;

    @FXML
    void checkConnection(ActionEvent event) {

        String host = urlName.getText();
        int port = Integer.parseInt(dropdownPort.getValue().toString());

        try {
            Socket sock = new Socket(host, port);
            resultArea.appendText(host + " listening on port " + port + "\n");
            sock.close();
        } catch (UnknownHostException e) {
            resultArea.setText(String.valueOf(e) + "\n");
            return;
        } catch (Exception e) {
            resultArea.appendText(host + " not listening on port "
                    + port + "\n");
        }


    }


    @FXML
    void clearBtn(ActionEvent event) {
        resultArea.setText("");
        urlName.setText("");

    }

    String message;

    private void runServer() {
        try {

            ServerSocket serverSocket = new ServerSocket(6666);
            updateServer("Server is running and waiting for a client...");
            while (true) { // Infinite loop
                try {
                    Socket clientSocket = serverSocket.accept();
                    updateServer("Client connected!");

                    new Thread(() -> {
                        try {
                            sleep(3000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    });
                    DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
                    DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());

                    message = dis.readUTF();
                    updateServer("Message from client: " + message);

                    // Sending a response back to the client
                    dos.writeUTF("Received: " + message);

                    dis.close();
                    dos.close();

                } catch (IOException e) {
                    updateServer("Error: " + e.getMessage());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                if (message.equalsIgnoreCase("exit")) break;

            }
        } catch (IOException e) {
            updateServer("Error: " + e.getMessage());
        }
    }

    private void updateServer(String message) {
        // Run on the UI thread
        javafx.application.Platform.runLater(() -> lb12.setText(message + "\n"));
    }


    private void connectToServer(ActionEvent event) {


        try {
            socket1 = new Socket("localhost", 6666);

            DataOutputStream dos = new DataOutputStream(socket1.getOutputStream());
            DataInputStream dis = new DataInputStream(socket1.getInputStream());

            dos.writeUTF(msgText.getText());
            String response = dis.readUTF();
            updateTextClient("Server response: " + response + "\n");

            dis.close();
            dos.close();
            socket1.close();
        } catch (Exception e) {
            updateTextClient("Error: " + e.getMessage() + "\n");
        }


    }

    private void updateTextClient(String message) {
        // Run on the UI thread
        javafx.application.Platform.runLater(() -> lb122.setText(message + "\n"));
    }

    @FXML
    private void startChat(ActionEvent event) {
        try {
            FXMLLoader loader;
            Stage stage = new Stage();
            if (event.getSource() == user2_server) {
                loader = new FXMLLoader(getClass().getResource("server-view.fxml"));
                stage.setTitle("Chat-Server");
                Scene scene = new Scene(loader.load());
                stage.setScene(scene);
                ServerViewController controller = loader.getController();
                new Thread(() -> {
                    try {
                        serverSocket = new ServerSocket(6666);
                        if (!serverSocket.isClosed()) {
                            clientSocket = serverSocket.accept();
                            Platform.runLater(() -> {
                                setupServerChat(controller, clientSocket);
                            });
                        }
                    } catch (IOException e) {
                        if (serverSocket != null && !serverSocket.isClosed()) {
                            e.printStackTrace();
                        }
                    }
                }).start();
                stage.setOnCloseRequest(e -> cleanupSockets());
            } else {
                loader = new FXMLLoader(getClass().getResource("client-view.fxml"));
                stage.setTitle("Chat-Client");
                Scene scene = new Scene(loader.load());
                stage.setScene(scene);
                ClientViewController controller = loader.getController();
                Socket socket = new Socket("localhost", 6666);
                setupClientChat(controller, socket);
                stage.setOnCloseRequest(e -> {
                    try {
                        if (socket != null) socket.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                });
            }
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setupServerChat(ServerViewController controller, Socket socket) {
        try {
            DataInputStream input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            DataOutputStream output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            controller.button_send.setOnAction(e -> {
                try {
                    String message = controller.tf_message.getText();
                    output.writeUTF(message);
                    output.flush();
                    addMessageforChat(controller.vbox_messages, "You: " + message, true);
                    controller.tf_message.clear();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            });
            startMessageReceiver(input, controller.vbox_messages);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void setupClientChat(ClientViewController controller, Socket socket) {
        try {
            DataInputStream input = new DataInputStream(socket.getInputStream());
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());
            controller.button_send.setOnAction(e -> {
                try {
                    String message = controller.tf_message.getText();
                    output.writeUTF(message);
                    addMessageforChat(controller.vbox_messages, "You: " + message, true);
                    controller.tf_message.clear();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            });
            startMessageReceiver(input, controller.vbox_messages);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startMessageReceiver(DataInputStream input, VBox messageBox) {
        new Thread(() -> {
            while (isRunning) {
                try {
                    if (Thread.currentThread().isInterrupted()) {
                        break;
                    }

                    String message = input.readUTF();
                    if (message != null) {
                        final String finalMessage = message;
                        Platform.runLater(() -> {
                            addMessageforChat(messageBox, "Other: " + finalMessage, false);
                        });
                    }

                    Thread.sleep(100); // Reduce CPU usage

                } catch (SocketException e) {
                    isRunning = false;
                    break;
                } catch (IOException e) {
                    if (isRunning) {
                        Platform.runLater(() -> {
                            addMessageforChat(messageBox, "Connection lost", false);
                        });
                    }
                    isRunning = false;
                    break;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }).start();
    }


    private void addMessageforChat(VBox messageBox, String message, boolean isSent) {
        Label messageLabel = new Label(message);
        messageLabel.setStyle("-fx-background-color: " + (isSent ? "#DCF8C6" : "#E8E8E8") + ";" +
                "-fx-padding: 5px;" +
                "-fx-background-radius: 5px;");

        HBox container = new HBox(messageLabel);
        container.setAlignment(isSent ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        container.setPadding(new Insets(5));

        messageBox.getChildren().add(container);
    }

    @FXML
    void startClient(ActionEvent event) {
        if (event.getSource() == user1_client) {
            startChat(event);
        } else {
            Stage stage = new Stage();
            Group root = new Group();
            Button connectButton = new Button("Connect to server");
            connectButton.setLayoutX(100);
            connectButton.setLayoutY(300);
            connectButton.setOnAction(this::connectToServer);
            // new Thread(this::connectToServer).start();

            Label lb11 = new Label("Client");
            lb11.setLayoutX(100);
            lb11.setLayoutY(100);
            msgText = new TextField("msg");
            msgText.setLayoutX(100);
            msgText.setLayoutY(150);

            lb122 = new Label("info");
            lb122.setLayoutX(100);
            lb122.setLayoutY(200);
            root.getChildren().addAll(lb11, lb122, connectButton, msgText);

            Scene scene = new Scene(root, 600, 350);
            stage.setScene(scene);
            stage.setTitle("Client");
            stage.show();
        }
    }

    @FXML
    void startServer(ActionEvent event) {
        if (event.getSource() == user2_server) {
            startChat(event);
        } else {
            Stage stage = new Stage();
            Group root = new Group();
            Label lb11 = new Label("Server");
            lb11.setLayoutX(100);
            lb11.setLayoutY(100);

            lb12 = new Label("info");
            lb12.setLayoutX(100);
            lb12.setLayoutY(200);
            root.getChildren().addAll(lb11, lb12);
            Scene scene = new Scene(root, 600, 350);
            stage.setScene(scene);
            lb12.setText("Server is running and waiting for a client...");

            stage.setTitle("Server");
            stage.show();

            new Thread(this::runServer).start();
        }
    }

    private void cleanupSockets() {
        isRunning = false;
        try {
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
