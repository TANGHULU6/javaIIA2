package cn.edu.sustech.cs209.chatting.client;

import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Main extends Application {
    private BufferedReader in;
    private PrintWriter out;

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("main.fxml"));
        stage.setScene(new Scene(fxmlLoader.load()));
        stage.setTitle("Chatting Client");
        stage.show();
        connectToServer();
        Controller controller = fxmlLoader.getController();
        controller.setSocketIO(in, out);

    }

    private void connectToServer() {
        String serverAddress = "127.0.0.1";
        int port = 12345;

        try {
            Socket socket = new Socket(serverAddress, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

//            new Thread(() -> {
//                while (true) {
//                    try {
//                        String message = in.readLine();
//                        if (message != null) {
//                            inputArea.appendText(message + "\n");
//                        }
//                    } catch (IOException e) {
//                        System.out.println("Error: " + e.getMessage());
//                        break;
//                    }
//                }
//            }).start();
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

}
