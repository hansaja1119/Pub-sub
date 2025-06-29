import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String serverAddress;
    private int serverPort;
    private String clientType;
    private boolean connected = false;
    private Thread messageListener;

    public Client(String serverAddress, int serverPort, String clientType) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.clientType = clientType.toUpperCase();
    }

    public void start() {
        try {
            // Connect to server
            socket = new Socket(serverAddress, serverPort);
            connected = true;
            System.out.println("Connected to server at " + serverAddress + ":" + serverPort);
            System.out.println("Client mode: " + clientType);

            // Set up input/output streams
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Send client type to server as first message
            out.println(clientType);

            if ("SUBSCRIBER".equals(clientType)) {
                startSubscriber();
            } else if ("PUBLISHER".equals(clientType)) {
                startPublisher();
            } else {
                System.err.println("Invalid client type: " + clientType);
                return;
            }

        } catch (UnknownHostException e) {
            System.err.println("Unknown host: " + serverAddress);
        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        } finally {
            stop();
        }
    }

    private void startSubscriber() {
        System.out.println("=== SUBSCRIBER MODE ===");
        System.out.println("Listening for messages from publishers...");
        System.out.println("Type 'terminate' to exit.");
        System.out.println("========================");

        // Start thread to listen for incoming messages from server
        startMessageListener();

        // Scanner to handle user input for termination
        Scanner scanner = new Scanner(System.in);
        String userInput;

        while (connected) {
            userInput = scanner.nextLine();
            if ("terminate".equals(userInput)) {
                System.out.println("Terminating subscriber connection...");
                out.println("terminate");
                break;
            }
            // Subscribers don't send regular messages, only listen
            System.out.println("(Subscribers only receive messages. Type 'terminate' to exit)");
        }

        scanner.close();
    }

    private void startPublisher() {
        System.out.println("=== PUBLISHER MODE ===");
        System.out.println("Type messages to publish to all subscribers.");
        System.out.println("Type 'terminate' to exit.");
        System.out.println("=======================");

        Scanner scanner = new Scanner(System.in);
        String userInput;

        while (connected) {
            System.out.print("Publish message: ");
            userInput = scanner.nextLine();

            // Send message to server
            out.println(userInput);

            // Check if user wants to terminate
            if ("terminate".equals(userInput)) {
                System.out.println("Terminating publisher connection...");
                break;
            }
        }

        scanner.close();
    }

    private void startMessageListener() {
        messageListener = new Thread(() -> {
            try {
                String message;
                while (connected && (message = in.readLine()) != null) {
                    System.out.println("\n" + message);
                    if ("SUBSCRIBER".equals(clientType)) {
                        System.out.print(""); // Prompt for next input
                    }
                }
            } catch (IOException e) {
                if (connected) {
                    System.err.println("Error receiving messages: " + e.getMessage());
                }
            }
        });
        messageListener.setDaemon(true);
        messageListener.start();
    }

    public void stop() {
        connected = false;

        try {
            if (messageListener != null && messageListener.isAlive()) {
                messageListener.interrupt();
            }

            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
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
        if (args.length != 3) {
            System.err.println("Usage: java Client <server_ip> <port> <PUBLISHER|SUBSCRIBER>");
            System.err.println("Examples:");
            System.err.println("  java Client localhost 5000 PUBLISHER");
            System.err.println("  java Client 192.168.10.2 5000 SUBSCRIBER");
            System.exit(1);
        }

        String serverAddress = args[0];
        String clientType = args[2];

        // Validate client type
        if (!"PUBLISHER".equalsIgnoreCase(clientType) && !"SUBSCRIBER".equalsIgnoreCase(clientType)) {
            System.err.println("Invalid client type: " + clientType);
            System.err.println("Must be either PUBLISHER or SUBSCRIBER");
            System.exit(1);
        }

        try {
            int port = Integer.parseInt(args[1]);
            Client client = new Client(serverAddress, port, clientType);

            // Add shutdown hook for graceful termination
            Runtime.getRuntime().addShutdownHook(new Thread(client::stop));

            client.start();
        } catch (NumberFormatException e) {
            System.err.println("Invalid port number: " + args[1]);
            System.exit(1);
        }
    }
}
