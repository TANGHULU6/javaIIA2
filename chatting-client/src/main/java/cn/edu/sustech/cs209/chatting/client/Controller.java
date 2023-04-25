package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import com.vdurmont.emoji.EmojiParser;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
//import org.apache.commons.text.StringEscapeUtils;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;


public class Controller implements Initializable {

    @FXML
    ListView<Message> chatContentList;
    @FXML
    Label currentUsername;
    String username;
    String otherUser;
    List<String> userList;
    List<String> OnlineUserList;
    @FXML
    private TextArea inputArea;
    @FXML
    ListView<CustomItem> chatList;
    @FXML
    Label currentOnlineCnt;

    Socket socket;

    private ObjectInputStream in;
    private ObjectOutputStream out;
    private Map<ConversationKey, List<Message>> chatHistory = new HashMap<>();
    //private MessageListener messageListener;

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
            connectToServer();
            if (userList != null && userList.contains(username)) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Information Dialog");
                alert.setHeaderText(null);
                alert.setContentText("change the username");
                alert.showAndWait();
                Platform.exit();
            } else {
                currentUsername.setText("Current User: " + username);
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
            out = new ObjectOutputStream(socket.getOutputStream());
            //out.writeObject(new String());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());
            //in.readObject();
            //messageListener = new MessageListener( in,chatContentList,username,otherUser);
            out.writeObject(username);
            out.flush();

            new Thread(() -> {
                while (true) {
                    try {
//                        messageListener.stop();
//                        if (messageListener.isRunning()) {
//                            Thread.sleep(1000);
//                        }
                        // System.out.println("stop listening");

                        updateUserList();

                        //startListeningForMessages(messageListener);
                        // System.out.println("start listening");
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
//        if (messageListener != null) {
//            messageListener.stop();
//        }
        ConversationKey key = new ConversationKey(username, otherUser);
        List<Message> chatContent = chatHistory.getOrDefault(key, new ArrayList<>());

        chatContentList.getItems().clear();
        chatContentList.getItems().addAll(chatContent);

//        messageListener = new MessageListener(in, chatContentList, username, otherUser);
//        startListeningForMessages(messageListener);
    }

    private void addMessageToHistory(String sender, String recipient, Message message) {
        ConversationKey key = new ConversationKey(sender, recipient);
        List<Message> conversation = chatHistory.getOrDefault(key, new ArrayList<>());
        String unicode = message.getData();
        String emoji = EmojiParser.parseToUnicode(unicode);
        Message message1 = new Message(message.getTimestamp(), message.getSentBy(), message.getSendTo(), emoji);
        conversation.add(message1);
        chatHistory.put(key, conversation);

    }


    private void openChatWindow(List<String> member) {
        member.add(username);
        String t = member.toString().replaceAll(",", "/");
        try {
            out.writeObject("#" + t);
            out.flush();
            Platform.runLater(() -> {
                ListView<String> newListView = new ListView<>();
                newListView.setPrefSize(chatList.getPrefWidth(), chatList.getPrefHeight());
                List<CustomItem> j = chatList.getItems()
                        .filtered(CustomItem::isOnline)
                        .filtered(s -> member.contains(s.getText()));
                newListView.getItems().clear();
                List<String> lll = j.stream().map(s -> s.getText()).toList();
                newListView.getItems().addAll(lll);


                Stage newStage = new Stage();
                Scene scene = new Scene(newListView);
                newStage.setScene(scene);
                newStage.setWidth(400);
                newStage.setHeight(300);
                newStage.show();
            });

        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    private final Object userListLock = new Object();

    private void updateUserList() throws InterruptedException {
        List<CustomItem> customItems = new ArrayList<>();
        Thread updateUserListThread = new Thread(() -> {
            try {
                try {
//                    out.writeObject(username);
//                    out.flush();
                    out.writeObject("USERLIST");
                    out.flush();
//                    out.writeObject("OUSERLIST");
//                    out.flush();
                } catch (SocketException e) {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Please quit");
                        alert.setHeaderText(null);
                        alert.setContentText("Server is shutting down...");
                        alert.showAndWait();

                    });

                }


            } catch (IOException e) {
                e.printStackTrace();
            }

            // Read the response from the server
            Object receivedObject = null;
            try {
                receivedObject = in.readObject();
            } catch (ClassNotFoundException | IOException e) {
                System.out.println("Received error object" + receivedObject);
            }

            if (receivedObject == null) {
                System.out.println("Received null object");
            } else {
                System.out.println("Received object: " + receivedObject + receivedObject.getClass());

            }

            if (receivedObject instanceof File) {
                try {
                    receiveBytes((File) receivedObject, new File("C:\\Users\\86189\\Desktop\\download.txt"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            }
            String userListStr;
            if (receivedObject != null) {
                userListStr = receivedObject.toString();
            } else userListStr = "[error]";
            if (userListStr.endsWith("@")) {
                System.out.println(userListStr);
                //String decodedMessage = StringEscapeUtils.unescapeJava(userListStr);

                Message msg = fromString(userListStr);
                addMessageToHistory(username, msg.getSentBy(), msg);
                Platform.runLater(() -> {
                    chatContentList.getItems().add(msg);
                    if (!msg.getSentBy().equals(username)) {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("New message received");
                        alert.setHeaderText(null);
                        alert.setContentText("You have a new message");
                        alert.showAndWait();
                    }

                });
            } else {
                if (userListStr.startsWith("#")) {
                    String finalUserListStr = userListStr;
                    Platform.runLater(() -> {
                        synchronized (userListLock) {
                            System.out.println("Clearing chatList items...");
                            chatList.getItems().clear();
                            System.out.println("Adding new chatList items...");
                            chatList.getItems().add(new CustomItem(finalUserListStr));
                            //chatList.getItems().add(new CustomItem("test"));
                            System.out.println("Added new chatList items");
                        }
                    });
                } else {
                    if (userListStr.endsWith("check")) {
                        userListStr = userListStr.substring(1, userListStr.length() - 6); // Remove the brackets
                        String[] userArray = userListStr.split(", "); // Split the string by commas and whitespace
                        OnlineUserList = Arrays.asList(userArray);
                        System.out.println("OnLineUser:" + OnlineUserList);
//                            Platform.runLater(()->{
//                                if(OnlineUserList==null){
//                                    return;
//                                }
//                                for (CustomItem item : chatList.getItems()) {
//                                    boolean online = OnlineUserList.contains(item.getText());
//                                    item.setOnline(online);
//                                }
//                            });
                    } else {
                        String[] two = userListStr.split("qwertyui");
                        if (two.length != 2) {
                            System.out.println("UserList Error Format");
                            return;
                        }
                        String allUser = two[0].substring(1, two[0].length() - 1);
                        String OnlineUser = two[1].substring(1, two[1].length() - 1);
                        String[] userArray = allUser.split(", "); // Split the string by commas and whitespace
                        String[] OnlineUserArray = OnlineUser.split(", ");
                        userList = Arrays.asList(userArray);
                        OnlineUserList = Arrays.asList(OnlineUserArray);
                        customItems.addAll(userList.stream().map(CustomItem::new).toList());
                        System.out.println("chatList:" + customItems.stream().map(s -> s.getText()).toList());

                        Platform.runLater(() -> {
                            synchronized (userListLock) {
                                currentOnlineCnt.setText("Online: " + OnlineUserList.size());
                                System.out.println("Clearing chatList items...");
                                chatList.getItems().clear();
                                System.out.println("Adding new chatList items...");
                                chatList.getItems().addAll(customItems.stream()
                                        .filter(item -> !item.getText().equals(username)).toList());
                                //chatList.getItems().add(new CustomItem("test"));
                                System.out.println("Added new chatList items");
                                if (OnlineUserList == null) {
                                    return;
                                }
                                for (CustomItem item : chatList.getItems()) {
                                    System.out.println("set online status");
                                    boolean online = OnlineUserList.contains(item.getText());
                                    item.setOnline(online);
                                }
                            }
                        });
                    }


                }
            }


        });
        updateUserListThread.start();
        updateUserListThread.join();


    }

    public void updateOnlineStatus(ListView<CustomItem> lll) throws IOException, ClassNotFoundException {
        out.writeObject("OUSERLIST");
        out.flush();
        in.readObject();
        Object receivedObject = null;
        try {
            receivedObject = in.readObject();
        } catch (ClassNotFoundException | IOException e) {
            System.out.println("Received error object" + receivedObject);
        }

        if (receivedObject == null) {
            System.out.println("Received null object");
        } else {
            System.out.println("Received object: " + receivedObject + receivedObject.getClass());

        }

        String userListStr;
        if (receivedObject != null) {
            userListStr = receivedObject.toString();
        } else userListStr = "[error]";
        userListStr = userListStr.substring(1, userListStr.length() - 1); // Remove the brackets
        String[] userArray = userListStr.split(", "); // Split the string by commas and whitespace
        OnlineUserList = Arrays.asList(userArray);
        System.out.println(OnlineUserList);
        Platform.runLater(() -> {
            if (OnlineUserList == null) {
                return;
            }
            for (CustomItem item : lll.getItems()) {
                boolean online = OnlineUserList.contains(item.getText());
                item.setOnline(online);
            }
        });

    }


    private List<String> getSelectedUsers() {
        return chatList.getItems().stream()
                .filter(CustomItem::isSelected)
                .map(CustomItem::getText)
                .collect(Collectors.toList());
    }


    @FXML
    public void QUIT() throws IOException {


        out.writeObject(username + "~");
        out.flush();
        in.close();
        out.close();
        socket.close();
        Platform.exit();
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
        Dialog<List<String>> dialog = new Dialog<>();
        dialog.setTitle("Create Group Chat");
        dialog.setHeaderText("Select users to invite:");


        ListView<CustomItem> userListView = new ListView<>();
        userListView.getItems().addAll(chatList.getItems());
        userListView.getItems().forEach(s -> s.setCheckBoxVisible(true));
        userListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        dialog.getDialogPane().setContent(userListView);


        ButtonType buttonTypeOk = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(buttonTypeOk, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == buttonTypeOk) {
                List<String> selectedUsers = new ArrayList<>();
                for (CustomItem user : userListView.getSelectionModel().getSelectedItems()) {
                    selectedUsers.add(user.getText());
                }
                selectedUsers.addAll(getSelectedUsers());

                return selectedUsers;
            }
            return null;
        });

        Optional<List<String>> result = dialog.showAndWait();
        if (result.isPresent()) {
            List<String> group = result.get();
            openChatWindow(group);
        }


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
        Message msg = new Message(System.currentTimeMillis(), username, otherUser, message);
        if (!message.isEmpty()) {
            // TODO: Send the message to the server.
            try {
                System.out.println(toString(msg));
                out.writeObject(toString(msg) + "@");
                out.flush();
            } catch (IOException e) {
                System.out.println("send illegal message");
            }
//            if(otherUser.startsWith("#")){
//                addMessageToHistory(otherUser,username,msg);
//            }else addMessageToHistory(username,otherUser,msg);
            //chatContentList.getItems().add(msg);
            // Clear the input field.
            inputArea.clear();
        } else {
            System.out.println("Cannot send null text");
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Warning");
            alert.setHeaderText(null);
            alert.setContentText("Cannot send null text");
            alert.showAndWait();
        }
    }

    //    private void startListeningForMessages() {
//        new Thread(() -> {
//            while (true) {
//                try {
//                    Object message = in.readObject();
//                    if (message != null) {
//                        if(message instanceof String){
//                            if(message.equals("QUIT")){
//                                Platform.runLater(() -> {
//                                    // Update the UI with the received message.
//                                    // You can use the chatContentList and update it with the new message.
//
//                                    Message msg = new Message(System.currentTimeMillis(),"Server",username,"Server is shutting down...");
//                                    chatContentList.getItems().add(msg);
//                                });
//                            }else {
//                                Platform.runLater(() -> {
//                                    // Update the UI with the received message.
//                                    // You can use the chatContentList and update it with the new message.
//
//                                    System.out.println(message);
//                                    Message msg = fromString(message.toString());
//                                    chatContentList.getItems().add(msg);
//                                });
//                            }
//                        }
//
//
//                    }
//
//
//                } catch (IOException | ClassNotFoundException e) {
//                    System.out.println("Error: " + e.getMessage());
//                    break;
//                }
//            }
//        }).start();
//    }
    private void startListeningForMessages(MessageListener messageListener) {
        try {
            Thread thread = new Thread(messageListener);
            thread.setDaemon(true);
            thread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Message fromString(String stro) {
        String str = stro.substring(0, stro.length() - 1);
        String[] parts = str.split("\\|");
        if (parts.length != 4) {
            throw new IllegalArgumentException("Invalid message format: " + str);
        }
        try {
            Long timestamp = Long.parseLong(parts[0]);
            String sentBy = parts[1];
            String sendTo = parts[2];
            String data = parts[3];
            return new Message(timestamp, sentBy, sendTo, data);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid message format: " + str, e);
        }
    }

    public static String toString(Message message) {
        return message.getTimestamp() + "|" + message.getSentBy() + "|" + message.getSendTo() + "|" + message.getData();
    }

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
                        setText(null);
                        setGraphic(null);
                        return;
                    }

                    HBox wrapper = new HBox();
                    Label nameLabel = new Label(msg.getSentBy());
                    Label nameGroupLabel = new Label(msg.getData().split(":")[0]);
                    Label msgLabel = new Label(msg.getData());

                    Label msgGroupLabel = new Label("QwQ");
                    try {
                        msgGroupLabel = new Label(msg.getData().split(":")[1]);
                    } catch (Exception ignored) {
                    }

                    nameLabel.setPrefSize(250, 20);
                    nameLabel.setWrapText(true);
                    nameLabel.setStyle("-fx-border-color: black; -fx-border-width: 1px;");
                    nameGroupLabel.setPrefSize(250, 20);
                    nameGroupLabel.setWrapText(true);
                    nameGroupLabel.setStyle("-fx-border-color: black; -fx-border-width: 1px;");
                    Font emojiFont = Font.font("Noto Color Emoji", 14);
                    msgLabel.setFont(emojiFont);
                    msgGroupLabel.setFont(emojiFont);

                    if (username.equals(msg.getSentBy())) {
                        wrapper.setAlignment(Pos.TOP_RIGHT);
                        wrapper.getChildren().addAll(msgLabel, nameLabel);
                        msgLabel.setPadding(new Insets(0, 20, 0, 0));
                    } else {
                        if (msg.getSentBy().startsWith("#")) {
                            if (username.equals(msg.getData().split(":")[0])) {
                                wrapper.setAlignment(Pos.TOP_RIGHT);
                                wrapper.getChildren().addAll(msgGroupLabel, nameGroupLabel);
                                msgLabel.setPadding(new Insets(0, 0, 0, 20));
                            } else {
                                wrapper.setAlignment(Pos.TOP_LEFT);
                                wrapper.getChildren().addAll(nameGroupLabel, msgGroupLabel);
                                msgLabel.setPadding(new Insets(0, 0, 0, 20));
                            }

                        } else {
                            wrapper.setAlignment(Pos.TOP_LEFT);
                            wrapper.getChildren().addAll(nameLabel, msgLabel);
                            msgLabel.setPadding(new Insets(0, 0, 0, 20));
                        }

                    }

                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                    setGraphic(wrapper);
                }
            };
        }
    }

    @FXML
    public void sendFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Resource File");
        File file = fileChooser.showOpenDialog(null);

        if (file != null) {
            try {
                out.writeObject(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
            //sendFile(file);
        }
    }

    //    private void sendFile(File file) {
//        try {
//            ObjectOutputStream outF = new ObjectOutputStream(socket.getOutputStream());
//            try {
//                outF.writeObject("FILE:"+ file.getName());
//                outF.flush();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            byte[] fileContent = Files.readAllBytes(file.toPath());
//            outF.writeObject(fileContent);
//            outF.flush();
//            System.out.println("transferring "+file.getName());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
    private void sendFile(File file) {
        try (DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             FileInputStream fis = new FileInputStream(file)) {
            dos.writeUTF("FILE:" + file.getName());
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                dos.write(buffer, 0, bytesRead);
            }
            System.out.println("Transferred " + file.getName());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void receiveBytes(File receive, File file) throws IOException {
        FileCopyUtil.copy(receive, file);
    }


}
