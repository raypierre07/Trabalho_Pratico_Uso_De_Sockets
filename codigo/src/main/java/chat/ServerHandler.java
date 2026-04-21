package chat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class ServerHandler {

    private Map<Integer, ChatServerThread> activeServers = new HashMap<>();
    private int nextServerId = 1;

    public String[] getServers() {
        if (activeServers.isEmpty()) {
            return null;
        }
        String[] servers = new String[activeServers.size()];
        int i = 0;
        for (Map.Entry<Integer, ChatServerThread> entry : activeServers.entrySet()) {
            servers[i] = "ID: " + entry.getKey() + " | Porta: " + entry.getValue().getPort();
            i++;
        }
        return servers;
    }

    public int createServer(Integer port) throws IOException {
        ChatServerThread server = new ChatServerThread(port);
        server.start();
        int id = nextServerId++;
        activeServers.put(id, server);
        return id;
    }

    public int getPortById(int id) {
        ChatServerThread server = activeServers.get(id);
        return server != null ? server.getPort() : -1;
    }

    private static class ChatServerThread extends Thread {
        private ServerSocket serverSocket;
        private int port;
        private boolean running = true;

        public ChatServerThread(int port) {
            this.port = port;
        }

        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(port);
                while (running) {
                    Socket clientSocket = serverSocket.accept();
                    ChatConnection connection = new ChatConnection(clientSocket);
                    connection.start();
                }
            } catch (IOException e) {
                if (running) {
                    e.printStackTrace();
                }
            }
        }

        public int getPort() {
            return port;
        }
    }
}
