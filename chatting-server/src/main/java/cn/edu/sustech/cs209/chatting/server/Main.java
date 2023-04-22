package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.common.Message;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;




public class Main {
    private static final int PORT = 5005;
    private static Map<String, ObjectOutputStream> users = new ConcurrentHashMap<>();
    private static List<String> UserList = new ArrayList<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is listening on port " + PORT);
            while (true) {
                Socket socket = serverSocket.accept();
                UserList.add("God");
                new Handler(socket).start();
            }
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public static class Handler extends Thread {
        private Socket socket;
        private String username;
        private ObjectInputStream in;
        private ObjectOutputStream out;

        public Handler(Socket socket) {
            this.socket = socket;
            try {
                in = new ObjectInputStream(socket.getInputStream());
                //in.readObject();
                out = new ObjectOutputStream(socket.getOutputStream());
               // out.writeObject(new String());
                out.flush();

            } catch (IOException e) {
                System.out.println("Error initializing streams: " + e.getMessage());
            }
        }

        private void sendUserList() throws IOException {
            handleMessage(new Message(System.currentTimeMillis(),"God",username,UserList.toString()));
        }

//        private void sendMessageToUser(Object message) throws IOException {
//            String messageContent;
//
//            if (message instanceof Message) {
//                Message msgObj = (Message) message;
//                username=msgObj.getSendTo();
//                ObjectOutputStream targetWriter = users.get(username);
//                if (targetWriter != null) {
//                    targetWriter.writeObject(msgObj);
//                } else {
//                    System.out.println("Error: User " + username + " not found.");
//                }
//            } else {
//                System.out.println("Error: Invalid message format.");
//                return;
//            }
//
//        }


        public void run() {
            try {
                while (true) {
                    Object input = null;
                    try {
                        input = in.readObject();
                    } catch (ClassNotFoundException e) {
                        System.out.println("Invalid object");
                    }
                    if (input == null) {
                        continue;
                    }

                    if (input instanceof String) {
                        handleStringMessage((String) input);
                    } else if (input instanceof Message) {
                        Message message = (Message) input;
                        handleMessage(message);
                    } else {
                        System.out.println("Error: Invalid message format.");
                    }
                }

            } catch (IOException e) {
                System.out.println("Error:zxc " + e.getMessage());
            } finally {
                if (username != null) {
                    users.remove(username);
                }
                try {
                    socket.close();
                } catch (IOException e) {
                    System.out.println("Error:12231 " + e.getMessage());
                }
            }
        }

        private void handleStringMessage(String messageContent) throws IOException {
            if (messageContent.equals("USERLIST")) {
                sendUserList();
            } else {
                username = messageContent;
                System.out.println("Welcome, " + username + "! You can now start chatting.");
                for (ObjectOutputStream writer : users.values()) {
                    if (writer != out) {
                        writer.writeObject(username + " has joined the chat.");
                    }
                }
                UserList.add(username);
                if (!users.containsKey(username)) {
                    users.put(username, out);
                }
            }
        }

        private void handleMessage(Message message) throws IOException {
            String targetUsername = message.getSendTo();
            ObjectOutputStream targetWriter = users.get(targetUsername);
            if (targetWriter != null) {
                targetWriter.reset();
                targetWriter.writeObject(message.toString());
                targetWriter.flush();
            } else {
                System.out.println("Error: User " + targetUsername + " not found.");
            }
        }


//        public String convertObjectToJson(Object object) {
//            Gson gson = new Gson();
//            String json = gson.toJson(object);
//            return json;
//        }



    }
}
