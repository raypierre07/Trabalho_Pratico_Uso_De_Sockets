package chat;

import java.util.Scanner;

public class Main {

    private static final String RESET = "\u001B[0m";
    private static final String CYAN = "\u001B[36m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String RED = "\u001B[31m";

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);
        ChatController controller = new ChatController();
        boolean running = true;

        while (running) {
            clearScreen();
            System.out.println(getMenu());

            System.out.print("Escolha uma opção: ");
            String input = sc.nextLine();

            switch (input) {
                case "1":
                    controller.listarServidores();
                    break;
                case "2":
                    controller.criarServidor();
                    break;
                case "3":
                    controller.conectarServidor(sc);
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

    private static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }
}