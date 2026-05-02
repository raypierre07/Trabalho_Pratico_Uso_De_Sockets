package chat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
    private List<String> lastDiscoveredServers = new ArrayList<>();

    public void listarServidores() {
        System.out.println(CYAN + "[INFO] Buscando servidores na rede (aguarde até 1.5s)..." + RESET);
        List<String> servers = serverHandler.discoverServers();
        lastDiscoveredServers = servers;

        if (!servers.isEmpty()) {
            for (int i = 0; i < servers.size(); i++) {
                System.out.println(GREEN + "ID: " + (i + 1) + RESET + " | Endereço: " + servers.get(i));
            }
        } else {
            System.out.println(RED + "Nenhum servidor encontrado na rede. Tente criar um!" + RESET);
        }
    }

    public void criarServidor() {
        System.out.println(GREEN + "[SERVER] Buscando porta disponível a partir de " + portCounter + "..." + RESET);
        boolean success = false;
        int tentativas = 0;

        while (!success && tentativas < 100) {
            try {
                serverHandler.createServer(portCounter);
                System.out.println(GREEN + "Servidor criado com sucesso na porta " + portCounter + "!" + RESET);
                portCounter++;
                success = true;
            } catch (IOException e) {
                // Se der erro (ex: BindException por porta em uso), tentamos a próxima
                portCounter++;
                tentativas++;
            }
        }

        if (!success) {
            System.out.println(RED + "Não foi possível criar o servidor após várias tentativas." + RESET);
        }
    }

    public void conectarServidor(Scanner sc) {
        System.out.println(YELLOW + "[CLIENT] Conectando ao servidor..." + RESET);
        System.out.print("Digite o ID da lista, a porta local, ou IP:porta: ");
        String target = sc.nextLine().trim();

        String ip = "127.0.0.1";
        int portToConnect = -1;

        try {
            if (target.contains(":")) {
                String[] parts = target.split(":");
                ip = parts[0];
                portToConnect = Integer.parseInt(parts[1]);
            } else {
                int idOrPort = Integer.parseInt(target);
                // Verifica se é um ID da última lista descoberta
                if (idOrPort > 0 && idOrPort <= lastDiscoveredServers.size() && target.length() <= 2) {
                    String[] parts = lastDiscoveredServers.get(idOrPort - 1).split(":");
                    ip = parts[0];
                    portToConnect = Integer.parseInt(parts[1]);
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
