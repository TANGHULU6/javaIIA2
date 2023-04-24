package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.client.ConversationKey;
import cn.edu.sustech.cs209.chatting.common.Message;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;




public class Main {
    private static final int PORT = 5005;
    private static Map<String, ObjectOutputStream> users = new ConcurrentHashMap<>();
    private static List<String> UserList = new ArrayList<>();
    private Map<ConversationKey, List<Message>> chatHistory_Server = new HashMap<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is listening on port " + PORT);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Server is shutting down...");
                try {
                    for (ObjectOutputStream out : users.values()) {
                        out.writeObject("QUIT");
                        out.flush();
                    }
                } catch (IOException e) {
                    System.out.println("Error sending shutdown message: " + e.getMessage());
                }
            }));
            while (true) {
                Socket socket = serverSocket.accept();
//                UserList.add("God");
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
            ObjectOutputStream targetWriter = users.get(username);
            if (targetWriter != null) {
                targetWriter.reset();
                targetWriter.writeObject(UserList.toString());
                targetWriter.flush();
            } else {
                System.out.println("Error: User " + username + " not found.");
            }
        }

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
                    System.out.println("not null");
                    if (input instanceof String) {
                        if(input.toString().endsWith("@")){
                            Message m=fromString(input.toString());
                            if(m.getSendTo().startsWith("#")){
                                handleGroupMessage(m);
                            }else handleMessage(m);

                            System.out.println("Server is passing..."+input);
                        }else {
                            if((input.toString()).endsWith("~")){
                                String dead=input.toString().substring(0,input.toString().length()-1);

                            }else {
                                handleStringMessage(input.toString());
                            }

                            System.out.println("Server is processing..."+input);
                        }

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
//                for (ObjectOutputStream writer : users.values()) {
//                    if (writer != out) {
//                        writer.writeObject(username + " has joined the chat.");
//                    }
//                }
                if (!UserList.contains(username)) {
                   UserList.add(username);
                    System.out.println("Welcome, " + username + "! You can now start chatting.");
                }
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
                targetWriter.writeObject(toString(message)+"@");
                targetWriter.flush();
            } else {
                System.out.println("Error: User " + targetUsername + " not found.");
            }
        }
        private void handleGroupMessage(Message message) throws IOException {
            String targetUsername = message.getSendTo();
            String userListStr = targetUsername.substring(1);
            String[] userArray = userListStr.split("/ ");
            List<String>userList = Arrays.asList(userArray);
            userList.stream().forEach(uu->{
                ObjectOutputStream targetWriter = users.get(uu);
                if (targetWriter != null) {
                    try {
                        targetWriter.reset();
                        targetWriter.writeObject(toString(message)+"@");
                        targetWriter.flush();
                    } catch (IOException e) {
                        System.out.println("Error: User " + uu + message+" group chat error");
                        e.printStackTrace();
                    }

                } else {
                    System.out.println("Error: User " + uu + " not found.");
                }
            });

        }

//        public String convertObjectToJson(Object object) {
//            Gson gson = new Gson();
//            String json = gson.toJson(object);
//            return json;
//        }

        public static Message fromString(String stro) {
            String str=stro.substring(0,stro.length()-1);
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
        public static String toString(Message message){
            return message.getTimestamp()+"|"+message.getSentBy()+"|"+message.getSendTo()+"|"+message.getData();
        }

    }
}
