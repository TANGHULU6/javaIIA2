package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;



public class Controller implements Initializable {

    @FXML
    ListView<Message> chatContentList;
    @FXML
    Label currentUsername;
    String username;
    String otherUser;
    List<String> userList;
    @FXML
    private TextArea inputArea;
    @FXML
    ListView<CustomItem> chatList;
    Socket socket;

    private ObjectInputStream in;
    private ObjectOutputStream out;
    private Map<ConversationKey, List<Message>> chatHistory = new HashMap<>();


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        Dialog<String> dialog = new TextInputDialog();
        dialog.setTitle("Login");
        dialog.setHeaderText(null);
        dialog.setContentText("Username:");

        Optional<String> input = dialog.showAndWait();
        if (input.isPresent() && !input.get().isEmpty()) {
            /*
               TODO: Check if there is a user with the same name among the currently logged-in users,
                     if so, ask the user to change the username
             */

            username = input.get();
            if(userList!=null&&userList.contains(username)){
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Information Dialog");
                alert.setHeaderText(null);
                alert.setContentText("change the username");
                alert.showAndWait();
                Platform.exit();
                return;
            }else {
                currentUsername.setText("Current User: " + username);
                connectToServer();
                chatList.setOnMouseClicked(event -> {
                    CustomItem selectedItem = chatList.getSelectionModel().getSelectedItem();
                    if (selectedItem != null) {
                        switchToChat(selectedItem);
                    }
                });
            }

        } else {
            System.out.println("Invalid username " + input + ", exiting");
            Platform.exit();
        }

        chatContentList.setCellFactory(new MessageCellFactory());
    }
    private void connectToServer() {
        String serverAddress = "127.0.0.1";
        int port = 5005;

        try {
            socket = new Socket(serverAddress, port);
            out =  new ObjectOutputStream(socket.getOutputStream());
            //out.writeObject(new String());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());
            //in.readObject();

            new Thread(() -> {
                while (true) {
                    try {
                        updateUserList();
                         Thread.sleep(10000); // Sleep for 10 seconds
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();


        } catch (IOException e) {
            System.out.println(1211);
        }
    }

    private void switchToChat(CustomItem selectedItem) {
        otherUser = selectedItem.getText();
        ConversationKey key = new ConversationKey(username, otherUser);
        List<Message> chatContent = chatHistory.getOrDefault(key, new ArrayList<>());

        chatContentList.getItems().clear();
        chatContentList.getItems().addAll(chatContent);
    }

    private void addMessageToHistory(String sender, String recipient, Message message) {
        ConversationKey key = new ConversationKey(sender, recipient);
        List<Message> conversation = chatHistory.getOrDefault(key, new ArrayList<>());
        conversation.add(message);
        chatHistory.put(key, conversation);
    }


//    private void openChatWindow(String username) {
//        Stage chatWindow = new Stage();
//        chatWindow.setTitle("Chat with " + username);
//
//        VBox layout = new VBox(10);
//        layout.setPadding(new Insets(10));
//
//        ListView<String> messagesList = new ListView<>();
//        TextField messageInput = new TextField();
//        Button sendButton = new Button("Send");
//
//        sendButton.setOnAction(e -> {
//            // TODO: Implement sending messages to the selected user
//        });
//
//        layout.getChildren().addAll(messagesList, messageInput, sendButton);
//        chatWindow.setScene(new Scene(layout, 300, 400));
//        chatWindow.show();
//    }

    public List<CustomItem> requestUserList(Socket socket) {
        // Send a command to the server to request the user list
        List<CustomItem> customItems=new ArrayList<>();
        new Thread(()->{
            while(true) {
                try {
                    out.writeObject(username);
                    out.flush();
                    out.writeObject("USERLIST");
                    out.flush();

                } catch (IOException e) {
                    e.printStackTrace();
                }

                // Read the response from the server
                Object receivedObject = null;
                try {
                    receivedObject = in.readObject();
                } catch (ClassNotFoundException | IOException e) {
                    System.out.println("Received error object"+receivedObject);
                    continue;
                }

                if (receivedObject == null) {
                    System.out.println("Received null object");
                } else {
                    System.out.println("Received object: " + receivedObject+ receivedObject.getClass());

                }

                String userListStr;
                    if (receivedObject != null) {
                        userListStr = receivedObject.toString();
                    } else userListStr = "[error]";

                    userListStr = userListStr.substring(1, userListStr.length() - 1); // Remove the brackets
                    String[] userArray = userListStr.split(", "); // Split the string by commas and whitespace
                    userList = Arrays.asList(userArray);
                    customItems.addAll(userList.stream().map(CustomItem::new).toList());
                    System.out.println(customItems.stream().map(s->s.getText()).toList());
                    break;
            }
        }).start();

        return customItems;
    }



    private final Object userListLock = new Object();

    private void updateUserList() {
        Task<List<CustomItem>> task = new Task<List<CustomItem>>() {
            @Override
            protected List<CustomItem> call() throws Exception {
                return requestUserList(socket);
            }
        };

        task.setOnSucceeded(event -> {
            List<CustomItem> userList = task.getValue();
            Platform.runLater(() -> {
                synchronized (userListLock) {
                    System.out.println("Clearing chatList items...");
                    chatList.getItems().clear();
                    System.out.println("Adding new chatList items...");
                    System.out.println(userList);
                    chatList.getItems().addAll(userList.stream()
                            .filter(item -> !item.getText().equals(username)).toList());
                    chatList.getItems().add(new CustomItem("test"));
                    System.out.println("Added new chatList items");

                }
            });

        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }



    private List<String> getSelectedUsers() {
        return chatList.getItems().stream()
                .filter(CustomItem::isSelected)
                .map(CustomItem::getText)
                .collect(Collectors.toList());
    }





    @FXML
    public void createPrivateChat() {
        AtomicReference<String> user = new AtomicReference<>();

        // Call this method inside the createPrivateChat() method, before showing the stage
        updateUserList();
        Stage stage = new Stage();
        ComboBox<String> userSel = new ComboBox<>();

        // FIXME: get the user list from server, the current user's name should be filtered out

        List<CustomItem> customItems = requestUserList(socket);
        userSel.getItems().addAll(customItems.stream().map(CustomItem::getText).toList());

        Button okBtn = new Button("OK");
        okBtn.setOnAction(e -> {
            user.set(userSel.getSelectionModel().getSelectedItem());
            if (user.get() != null) {
                chatList.getItems().add(new CustomItem(user.get()));
            }
            stage.close();
        });

        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20, 20, 20, 20));
        box.getChildren().addAll(userSel, okBtn);
        stage.setScene(new Scene(box));
        stage.showAndWait();

        // TODO: if the current user already chatted with the selected user, just open the chat with that user
        // TODO: otherwise, create a new chat item in the left panel, the title should be the selected user's name
    }

    /**
     * A new dialog should contain a multi-select list, showing all user's name.
     * You can select several users that will be joined in the group chat, including yourself.
     * <p>
     * The naming rule for group chats is similar to WeChat:
     * If there are > 3 users: display the first three usernames, sorted in lexicographic order, then use ellipsis with the number of users, for example:
     * UserA, UserB, UserC... (10)
     * If there are <= 3 users: do not display the ellipsis, for example:
     * UserA, UserB (2)
     */
    @FXML
    public void createGroupChat() {
    }

    /**
     * Sends the message to the <b>currently selected</b> chat.
     * <p>
     * Blank messages are not allowed.
     * After sending the message, you should clear the text input field.
     */
    @FXML
    public void doSendMessage() {
        String message = inputArea.getText().trim();
        Message msg = new Message(System.currentTimeMillis(),username,otherUser,message);
        if (!message.isEmpty()) {
            // TODO: Send the message to the server.
            try {
                out.writeObject(msg);
            } catch (IOException e) {
                System.out.println("send illegal message");
            }

            addMessageToHistory(username,otherUser,msg);
            chatContentList.getItems().add(msg);
            // Clear the input field.
            inputArea.clear();
        }
    }
//    private void startListeningForMessages() {
//        new Thread(() -> {
//            while (true) {
//                try {
//                    String message = in.readLine();
//                    if (message != null) {
//                        Platform.runLater(() -> {
//                            // Update the UI with the received message.
//                            // You can use the chatContentList and update it with the new message.
//                            // Assuming the server sends messages in the format "username:message"
//
//                            Message msg = new Message(System.currentTimeMillis(),username,otherUser,message);
//                            chatContentList.getItems().add(msg);
//                        });
//                    }
//                } catch (IOException e) {
//                    System.out.println("Error: " + e.getMessage());
//                    break;
//                }
//            }
//        }).start();
//    }

    /**
     * You may change the cell factory if you changed the design of {@code Message} model.
     * Hint: you may also define a cell factory for the chats displayed in the left panel, or simply override the toString method.
     */
    private class MessageCellFactory implements Callback<ListView<Message>, ListCell<Message>> {
        @Override
        public ListCell<Message> call(ListView<Message> param) {
            return new ListCell<Message>() {

                @Override
                public void updateItem(Message msg, boolean empty) {
                    super.updateItem(msg, empty);
                    if (empty || Objects.isNull(msg)) {
                        return;
                    }

                    HBox wrapper = new HBox();
                    Label nameLabel = new Label(msg.getSentBy());
                    Label msgLabel = new Label(msg.getData());

                    nameLabel.setPrefSize(50, 20);
                    nameLabel.setWrapText(true);
                    nameLabel.setStyle("-fx-border-color: black; -fx-border-width: 1px;");

                    if (username.equals(msg.getSentBy())) {
                        wrapper.setAlignment(Pos.TOP_RIGHT);
                        wrapper.getChildren().addAll(msgLabel, nameLabel);
                        msgLabel.setPadding(new Insets(0, 20, 0, 0));
                    } else {
                        wrapper.setAlignment(Pos.TOP_LEFT);
                        wrapper.getChildren().addAll(nameLabel, msgLabel);
                        msgLabel.setPadding(new Insets(0, 0, 0, 20));
                    }

                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                    setGraphic(wrapper);
                }
            };
        }
    }
}
