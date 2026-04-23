package chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ChatConnection extends Thread {
    private static final String QUIT_TOKEN = "/sair";

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    /** Quando não nulo, esta conexão é o lado servidor e participa do relay entre clientes. */
    private ServerHandler.MessageRelay relay;

    public ChatConnection() {
        this(null, null);
    }

    public ChatConnection(Socket socket) {
        this(socket, null);
    }

    public ChatConnection(Socket socket, ServerHandler.MessageRelay relay) {
        this.socket = socket;
        this.relay = relay;
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
        // autoFlush true: cada println envia imediatamente (evita mensagens presas no buffer)
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

    /**
     * Loop principal do lado servidor: lê linhas do cliente e retransmite à sala.
     * O protocolo é texto linha a linha ({@link BufferedReader#readLine()}).
     */
    public void handleChat() throws IOException {
        if (relay != null) {
            relay.registerClient(this);
        }
        try {
            String line;
            while ((line = in.readLine()) != null) {
                if (QUIT_TOKEN.equalsIgnoreCase(line.trim())) {
                    // Não propaga o comando de saída como mensagem de chat
                    break;
                }
                if (relay != null) {
                    relay.broadcast(this, line);
                }
            }
        } finally {
            if (relay != null) {
                relay.unregisterClient(this);
            }
        }
    }

    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    /**
     * Leitura bloqueante — deve ser chamada de uma thread separada do {@code Scanner},
     * senão enviar e receber ao mesmo tempo não funciona.
     */
    public String readIncomingLine() throws IOException {
        return in.readLine();
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

    /** Identificação simples no broadcast (porta do lado remoto do TCP). */
    public int getRemotePort() {
        return socket != null ? socket.getPort() : -1;
    }
}
