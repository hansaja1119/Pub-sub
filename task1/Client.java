import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    private Socket socket;
    private PrintWriter out;
    private String serverAddress;
    private int serverPort;

    public Client(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }

    public void start() {
        try {
            // Connect to server
            socket = new Socket(serverAddress, serverPort);
            System.out.println("Connected to server at " + serverAddress + ":" + serverPort);
            System.out.println("Type messages to send to server. Type 'terminate' to exit.");

            // Set up output stream to send messages to server
            out = new PrintWriter(socket.getOutputStream(), true);

            // Scanner to read user input
            Scanner scanner = new Scanner(System.in);

            // Continuously read user input and send to server
            String userInput;
            while (true) {
                System.out.print("Enter message: ");
                userInput = scanner.nextLine();

                // Send message to server
                out.println(userInput);

                // Check if user wants to terminate
                if ("terminate".equals(userInput)) {
                    System.out.println("Terminating client connection...");
                    break;
                }
            }

            scanner.close();

        } catch (UnknownHostException e) {
            System.err.println("Unknown host: " + serverAddress);
        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        } finally {
            stop();
        }
    }

    public void stop() {
        try {
            if (out != null) {
                out.close();
            }
            if (socket != null) {
                socket.close();
            }
            System.out.println("Client disconnected.");
        } catch (IOException e) {
            System.err.println("Error stopping client: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java Client <server_ip> <port>");
            System.exit(1);
        }

        try {
            String serverAddress = args[0];
            int port = Integer.parseInt(args[1]);
            Client client = new Client(serverAddress, port);
            client.start();
        } catch (NumberFormatException e) {
            System.err.println("Invalid port number: " + args[1]);
            System.exit(1);
        }
    }
}
