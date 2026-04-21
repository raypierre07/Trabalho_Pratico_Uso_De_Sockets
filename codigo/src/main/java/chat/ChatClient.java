package chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ChatClient {

    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;

    public ChatClient(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    public void start() throws IOException {
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        in = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream()));

        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            if (".".equals(inputLine)) {
                out.println("bye");
                break;
            }
            out.println(inputLine);
        }

        in.close();
        out.close();
        clientSocket.close();
    }

    public void connect() {

    }

    public void disconnect() {

    }

    public void sendMessage(String message) {

    }



}
