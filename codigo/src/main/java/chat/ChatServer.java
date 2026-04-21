package chat;

import java.io.IOException;
import java.net.ServerSocket;

public class ChatServer {

    private ServerSocket serverSocket;

    public void start(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        while (true)
            new ChatClient(serverSocket.accept()).start();
    }

    public void stop() {

    }

}
