package chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ChatConnection extends Thread {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    public ChatConnection() {
    }

    public ChatConnection(Socket socket) {
        this.socket = socket;
    }

    public boolean connect(String ip, int port) {
        try {
            socket = new Socket(ip, port);
            setupStreams();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private void setupStreams() throws IOException {
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    @Override
    public void run() {
        try {
            if (out == null || in == null) {
                setupStreams();
            }
            handleChat();
        } catch (IOException e) {
            System.err.println("Connection error: " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    public void handleChat() {
        // Base para lógica de recebimento e roteamento de mensagens em tempo real
    }

    public void sendMessage(String message) {
        // Base para enviar mensagem ao remetente/servidor
    }

    public void receiveMessages() {
        // Base para chamadas de escuta em tempo real usando Thread
    }

    public void disconnect() {
        try {
            if (in != null)
                in.close();
            if (out != null)
                out.close();
            if (socket != null && !socket.isClosed())
                socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
