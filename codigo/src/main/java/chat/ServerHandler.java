package chat;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ServerHandler {

    /**
     * Permite que cada {@link ChatConnection} no servidor registre-se e envie
     * mensagens
     * para os demais clientes do mesmo {@link ServerSocket} (sala de chat por
     * porta).
     */
    public interface MessageRelay {
        void registerClient(ChatConnection connection);

        void unregisterClient(ChatConnection connection);

        void broadcast(ChatConnection sender, String message);
    }

    private static final int DISCOVERY_PORT = 8888;
    private static final String DISCOVER_MESSAGE = "DISCOVER_CHAT_SERVER";

    public List<String> discoverServers() {
        List<String> foundServers = new ArrayList<>();
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true);
            socket.setSoTimeout(1500); // Aguarda 1.5s por respostas

            byte[] requestData = DISCOVER_MESSAGE.getBytes();

            // 1. Dispara no broadcast global
            try {
                DatagramPacket packet = new DatagramPacket(
                        requestData, requestData.length,
                        InetAddress.getByName("255.255.255.255"), DISCOVERY_PORT);
                socket.send(packet);
            } catch (Exception ignored) {}

            // 2. Dispara no localhost explícito (garante achar a própria máquina)
            try {
                DatagramPacket packet = new DatagramPacket(
                        requestData, requestData.length,
                        InetAddress.getByName("127.0.0.1"), DISCOVERY_PORT);
                socket.send(packet);
            } catch (Exception ignored) {}

            // 3. Dispara em todas as interfaces de rede (resolve Radmin/Hamachi/Wi-Fi)
            try {
                Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                while (interfaces.hasMoreElements()) {
                    NetworkInterface netInt = interfaces.nextElement();
                    if (netInt.isLoopback() || !netInt.isUp()) continue;
                    for (InterfaceAddress interfaceAddress : netInt.getInterfaceAddresses()) {
                        InetAddress broadcast = interfaceAddress.getBroadcast();
                        if (broadcast != null) {
                            try {
                                DatagramPacket netPacket = new DatagramPacket(
                                        requestData, requestData.length,
                                        broadcast, DISCOVERY_PORT);
                                socket.send(netPacket);
                            } catch (Exception ignored) {}
                        }
                    }
                }
            } catch (Exception ignored) {}

            long startTime = System.currentTimeMillis();
            byte[] buffer = new byte[256];

            while (System.currentTimeMillis() - startTime < 1500) {
                try {
                    DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
                    socket.receive(receivePacket);
                    String reply = new String(receivePacket.getData(), 0, receivePacket.getLength());

                    if (reply.startsWith("CHAT_SERVER:")) {
                        String tcpPort = reply.split(":")[1].trim();
                        String ip = receivePacket.getAddress().getHostAddress();
                        String serverAddress = ip + ":" + tcpPort;
                        if (!foundServers.contains(serverAddress)) {
                            foundServers.add(serverAddress);
                        }
                    }
                } catch (SocketTimeoutException e) {
                    // Timeout esperado, fim da escuta
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Erro ao buscar servidores: " + e.getMessage());
        }
        return foundServers;
    }

    public void createServer(Integer port) throws IOException {
        ChatServerThread server = new ChatServerThread(port);
        server.start();
    }

    private static class ChatServerThread extends Thread implements MessageRelay {
        private final ServerSocket serverSocket;
        private final int port;
        private boolean running = true;

        /** Lista segura para concorrência. */
        private final CopyOnWriteArrayList<ChatConnection> clients = new CopyOnWriteArrayList<>();
        private DiscoveryResponder responder;

        public ChatServerThread(int port) throws IOException {
            this.serverSocket = new ServerSocket(port);
            this.port = this.serverSocket.getLocalPort();

            // Inicia a thread que responde aos broadcasts de descoberta
            this.responder = new DiscoveryResponder(this.port);
            this.responder.start();
        }

        @Override
        public void run() {
            try {
                while (running) {
                    Socket clientSocket = serverSocket.accept();
                    ChatConnection connection = new ChatConnection(clientSocket, this);
                    connection.start();
                }
            } catch (IOException e) {
                if (running) {
                    e.printStackTrace();
                }
            } finally {
                if (responder != null) {
                    responder.stopResponding();
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

    /**
     * Thread que fica ouvindo por broadcasts UDP e responde com a porta do servidor
     * TCP.
     */
    private static class DiscoveryResponder extends Thread {
        private final int tcpPort;
        private DatagramSocket socket;
        private volatile boolean running = true;

        public DiscoveryResponder(int tcpPort) {
            this.tcpPort = tcpPort;
            setDaemon(true);
        }

        public void stopResponding() {
            running = false;
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }

        @Override
        public void run() {
            try {
                socket = new DatagramSocket(null);
                socket.setReuseAddress(true);
                socket.bind(new InetSocketAddress("0.0.0.0", DISCOVERY_PORT));

                byte[] buffer = new byte[256];
                while (running) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    String msg = new String(packet.getData(), 0, packet.getLength()).trim();
                    if (DISCOVER_MESSAGE.equals(msg)) {
                        String reply = "CHAT_SERVER:" + tcpPort;
                        byte[] replyData = reply.getBytes();
                        DatagramPacket replyPacket = new DatagramPacket(
                                replyData, replyData.length,
                                packet.getAddress(), packet.getPort());
                        socket.send(replyPacket);
                    }
                }
            } catch (IOException e) {
                if (running) {
                    System.err.println("Erro no DiscoveryResponder: " + e.getMessage());
                }
            } finally {
                stopResponding();
            }
        }
    }
}
