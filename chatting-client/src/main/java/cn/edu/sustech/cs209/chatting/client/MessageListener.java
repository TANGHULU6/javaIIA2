package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import javafx.application.Platform;
import javafx.scene.control.ListView;

import java.io.IOException;
import java.io.ObjectInputStream;

import static cn.edu.sustech.cs209.chatting.client.Controller.fromString;

public class MessageListener implements Runnable {
    private ObjectInputStream in;
    private ListView<Message> chatContentList;
    private String username;
    private String otherUser;
    private volatile boolean isRunning = true;
    public MessageListener(ObjectInputStream in, ListView<Message> chatContentList, String username, String otherUser) {
        this.in = in;
        this.chatContentList = chatContentList;
        this.username = username;
        this.otherUser = otherUser;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void stop() {
        isRunning = false;
    }
    @Override
    public void run() {
            while (isRunning) {
                Object message;
                try{
                    message = in.readObject();
                }catch (Exception e){
                    continue;
                }
                    if (message != null) {
                        if(message instanceof String ){
                            if(message.equals("QUIT")){
                                Platform.runLater(() -> {
                                    // Update the UI with the received message.
                                    // You can use the chatContentList and update it with the new message.

                                    Message msg = new Message(System.currentTimeMillis(),"Server",username,"Server is shutting down...");
                                    chatContentList.getItems().add(msg);
                                });
                            }else {
                                if(((String) message).endsWith("@")){
                                    Platform.runLater(() -> {
                                        // Update the UI with the received message.
                                        // You can use the chatContentList and update it with the new message.

                                        System.out.println(message);
                                        Message msg = fromString(message.toString());
                                        Platform.runLater(() -> chatContentList.getItems().add(msg));

                                    });
                                }

                            }
                        }


                    }
            }

    }
}
