package chat;

import java.io.IOException;
import java.util.Scanner;

public class Main {

    public static int PORT = 134;

    private static final String RESET = "\u001B[0m";
    private static final String CYAN = "\u001B[36m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String RED = "\u001B[31m";

    private static ServerHandler serverHandler = new ServerHandler();

    public static void main(String[] args) throws IOException {

        Scanner sc = new Scanner(System.in);
        boolean running = true;

        while (running) {
            clearScreen();
            System.out.println(getMenu());

            System.out.print("Escolha uma opção: ");
            String input = sc.nextLine();

            switch (input) {
                case "1":
                    listarServidores();
                    break;
                case "2":
                    criarServidor();
                    break;
                case "3":
                    conectarServidor();
                    break;
                case "0":
                    System.out.println(RED + "Encerrando..." + RESET);
                    running = false;
                    break;
                default:
                    System.out.println(RED + "Opção inválida!" + RESET);
            }

            if (running) {
                System.out.println("\nPressione ENTER para voltar ao menu...");
                sc.nextLine();
            }
        }

        sc.close();
    }

    public static String getMenu() {
        StringBuilder menu = new StringBuilder();

        menu.append(CYAN).append("=== CHAT SOCKET CLI ===\n").append(RESET);

        menu.append(GREEN).append("[1]").append(RESET).append(" Listar servidores\n");
        menu.append(YELLOW).append("[2]").append(RESET).append(" Criar servidor\n");
        menu.append(YELLOW).append("[3]").append(RESET).append(" Conectar ao servidor\n");
        menu.append(RED).append("[0]").append(RESET).append(" Sair\n");

        return menu.toString();
    }

    private static void listarServidores() {
        System.out.println(CYAN + "[INFO] Listando servidores..." + RESET);
        String[] serverList = serverHandler.getServers();
        if (serverList != null) {
            String serversString = String.join("\n", serverList);
            System.out.println(serversString);
        } else {
            System.out.println(RED + "Não possui servidores na rede. Tente criar um!");
        }
    }

    private static void criarServidor() throws IOException {
        System.out.println(GREEN + "[SERVER] Iniciando servidor na porta " + PORT + RESET);
        ChatServer serverSocket = new ChatServer();
        serverSocket.start(PORT);
        PORT++;
    }

    private static void conectarServidor() {
        System.out.println(YELLOW + "[CLIENT] Conectando ao servidor..." + RESET);
        serverHandler.connectServer();
    }

    private static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }
}