package chat;

import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChatController {

    private static final String RESET = "\u001B[0m";
    private static final String CYAN = "\u001B[36m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String RED = "\u001B[31m";

    private ServerHandler serverHandler = new ServerHandler();
    private int portCounter = 134;

    public void listarServidores() {
        System.out.println(CYAN + "[INFO] Listando servidores..." + RESET);
        String[] serverList = serverHandler.getServers();
        if (serverList != null) {
            String serversString = String.join("\n", serverList);
            System.out.println(serversString);
        } else {
            System.out.println(RED + "Não possui servidores na rede. Tente criar um!" + RESET);
        }
    }

    public void criarServidor() {
        System.out.println(GREEN + "[SERVER] Iniciando servidor na porta " + portCounter + RESET);
        try {
            int id = serverHandler.createServer(portCounter);
            System.out.println(GREEN + "Servidor criado com sucesso! ID: " + id + RESET);
            portCounter++;
        } catch (IOException e) {
            System.out.println(RED + "Erro ao criar servidor: " + e.getMessage() + RESET);
        }
    }

    public void conectarServidor(Scanner sc) {
        System.out.println(YELLOW + "[CLIENT] Conectando ao servidor..." + RESET);
        System.out.print("Digite o ID do servidor local ou a porta (ou IP:porta): ");
        String target = sc.nextLine();

        String ip = "127.0.0.1";
        int portToConnect = -1;

        try {
            if (target.contains(":")) {
                String[] parts = target.split(":");
                ip = parts[0];
                portToConnect = Integer.parseInt(parts[1]);
            } else {
                int idOrPort = Integer.parseInt(target);
                int foundPort = serverHandler.getPortById(idOrPort);
                if (foundPort != -1) {
                    portToConnect = foundPort;
                } else {
                    portToConnect = idOrPort;
                }
            }
        } catch (Exception e) {
            System.out.println(RED + "Entrada inválida!" + RESET);
            return;
        }

        System.out.println(YELLOW + "Conectando em " + ip + ":" + portToConnect + "..." + RESET);
        ChatConnection connection = new ChatConnection();
        if (connection.connect(ip, portToConnect)) {
            System.out.println(GREEN + "Conectado com sucesso!" + RESET);

            iniciarChat(connection, sc);

            connection.disconnect();
            System.out.println(YELLOW + "Desconectado do servidor." + RESET);
        } else {
            System.out.println(RED + "Não foi possível conectar." + RESET);
        }
    }

    private void iniciarChat(ChatConnection connection, Scanner sc) {
        System.out.println(CYAN + "[CHAT] Conectado. Digite mensagens; use /sair para voltar ao menu." + RESET);

        AtomicBoolean chatAtivo = new AtomicBoolean(true);

        // readLine() bloqueia: em thread separada para não travar o teclado (envio + recepção paralelos)
        Thread receptor = new Thread(() -> {
            try {
                String linha;
                while (chatAtivo.get() && (linha = connection.readIncomingLine()) != null) {
                    System.out.println(GREEN + "[Recebido] " + RESET + linha);
                }
            } catch (IOException e) {
                if (chatAtivo.get()) {
                    System.out.println(RED + "Erro ao receber: " + e.getMessage() + RESET);
                }
            }
        }, "chat-receptor");
        receptor.setDaemon(true);
        receptor.start();

        try {
            while (true) {
                String envio = sc.nextLine();
                if ("/sair".equalsIgnoreCase(envio.trim())) {
                    connection.sendMessage("/sair");
                    break;
                }
                connection.sendMessage(envio);
            }
        } finally {
            chatAtivo.set(false);
            connection.disconnect();
            try {
                receptor.join(1500);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
