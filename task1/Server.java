import java.io.*;
import java.net.*;

public class Server {
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private BufferedReader in;
    private int port;

    public Server(int port) {
        this.port = port;
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server started on port " + port);
            System.out.println("Waiting for client connection...");

            // Accept client connection
            clientSocket = serverSocket.accept();
            System.out.println("Client connected from: " + clientSocket.getInetAddress().getHostAddress());

            // Set up input stream to read messages from client
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            // Listen for messages from client
            String message;
            while ((message = in.readLine()) != null) {
                if ("terminate".equals(message)) {
                    System.out.println("Client requested termination. Closing connection...");
                    break;
                }
                System.out.println("Client: " + message);
            }

        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        } finally {
            stop();
        }
    }

    public void stop() {
        try {
            if (in != null) {
                in.close();
            }
            if (clientSocket != null) {
                clientSocket.close();
            }
            if (serverSocket != null) {
                serverSocket.close();
            }
            System.out.println("Server stopped.");
        } catch (IOException e) {
            System.err.println("Error stopping server: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java Server <port>");
            System.exit(1);
        }

        try {
            int port = Integer.parseInt(args[0]);
            Server server = new Server(port);
            server.start();
        } catch (NumberFormatException e) {
            System.err.println("Invalid port number: " + args[0]);
            System.exit(1);
        }
    }
}
