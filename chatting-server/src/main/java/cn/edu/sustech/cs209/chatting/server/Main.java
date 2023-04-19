package cn.edu.sustech.cs209.chatting.server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Main {
    private static final int PORT = 12345;
    private static Map<String, PrintWriter> users = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is listening on port " + PORT);
            while (true) {
                Socket socket = serverSocket.accept();
                new Handler(socket).start();
            }
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static class Handler extends Thread {
        private Socket socket;
        private String username;
        private BufferedReader in;
        private PrintWriter out;

        public Handler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                while (true) {
                    out.println("Enter your username:");
                    username = in.readLine();
                    if (username == null) {
                        return;
                    }
                    synchronized (users) {
                        if (!users.containsKey(username)) {
                            users.put(username, out);
                            break;
                        }
                    }
                }

                out.println("Welcome, " + username + "! You can now start chatting.");
                for (PrintWriter writer : users.values()) {
                    if (writer != out) {
                        writer.println(username + " has joined the chat.");
                    }
                }
                String message;
                while ((message = in.readLine()) != null) {
                    for (PrintWriter writer : users.values()) {
                        if (writer != out) {
                            writer.println(username + ": " + message);
                        }
                    }
                }

            } catch (IOException e) {
                System.out.println("Error: " + e.getMessage());
            } finally {
                if (username != null) {
                    users.remove(username);
                }
                try {
                    socket.close();
                } catch (IOException e) {
                    System.out.println("Error: " + e.getMessage());
                }
            }
        }
    }
}
