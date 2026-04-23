package chat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class ServerHandler {

    /**
     * Permite que cada {@link ChatConnection} no servidor registre-se e envie mensagens
     * para os demais clientes do mesmo {@link ServerSocket} (sala de chat por porta).
     */
    public interface MessageRelay {
        void registerClient(ChatConnection connection);

        void unregisterClient(ChatConnection connection);

        void broadcast(ChatConnection sender, String message);
    }

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

    private static class ChatServerThread extends Thread implements MessageRelay {
        private final ServerSocket serverSocket;
        private final int port;
        private boolean running = true;

        /** Lista segura para concorrência: várias threads de cliente podem broadcast ao mesmo tempo. */
        private final CopyOnWriteArrayList<ChatConnection> clients = new CopyOnWriteArrayList<>();

        public ChatServerThread(int port) throws IOException {
            this.serverSocket = new ServerSocket(port);
            this.port = this.serverSocket.getLocalPort();
        }

        @Override
        public void run() {
            try {
                while (running) {
                    Socket clientSocket = serverSocket.accept();
                    // Cada cliente roda na sua própria thread; o relay agrupa todos desta porta.
                    ChatConnection connection = new ChatConnection(clientSocket, this);
                    connection.start();
                }
            } catch (IOException e) {
                if (running) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void registerClient(ChatConnection connection) {
            clients.add(connection);
        }

        @Override
        public void unregisterClient(ChatConnection connection) {
            clients.remove(connection);
        }

        @Override
        public void broadcast(ChatConnection sender, String message) {
            String prefix = "[p" + sender.getRemotePort() + "] ";
            for (ChatConnection peer : clients) {
                peer.sendMessage(prefix + message);
            }
        }

        public int getPort() {
            return port;
        }
    }
}
