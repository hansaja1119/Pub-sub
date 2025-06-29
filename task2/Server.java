import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Server {
    private ServerSocket serverSocket;
    private int port;
    private boolean running = false;
    private ExecutorService threadPool;

    // Collections to manage connected clients
    private final Set<ClientHandler> publishers = Collections.synchronizedSet(new HashSet<>());
    private final Set<ClientHandler> subscribers = Collections.synchronizedSet(new HashSet<>());
    private final Set<ClientHandler> allClients = Collections.synchronizedSet(new HashSet<>());

    public Server(int port) {
        this.port = port;
        this.threadPool = Executors.newCachedThreadPool();
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            System.out.println("Server started on port " + port);

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                    allClients.add(clientHandler);
                    threadPool.execute(clientHandler);
                } catch (IOException e) {
                    if (running) {
                        System.err.println("Error accepting client connection: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        } finally {
            stop();
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }

            // Close all client connections
            synchronized (allClients) {
                for (ClientHandler client : allClients) {
                    client.disconnect();
                }
                allClients.clear();
                publishers.clear();
                subscribers.clear();
            }

            threadPool.shutdown();
            try {
                if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                    threadPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                threadPool.shutdownNow();
            }

            System.out.println("Server stopped.");
        } catch (IOException e) {
            System.err.println("Error stopping server: " + e.getMessage());
        }
    }

    // Broadcast message from publisher to all subscribers
    public void broadcastToSubscribers(String message, String publisherInfo) {
        String formattedMessage = "[PUBLISHER " + publisherInfo + "]: " + message;
        System.out.println("Broadcasting: " + formattedMessage);

        synchronized (subscribers) {
            Iterator<ClientHandler> iterator = subscribers.iterator();
            while (iterator.hasNext()) {
                ClientHandler subscriber = iterator.next();
                if (!subscriber.sendMessage(formattedMessage)) {
                    // Remove disconnected subscriber
                    iterator.remove();
                    allClients.remove(subscriber);
                }
            }
        }
        System.out.println("Message broadcasted to " + subscribers.size() + " subscribers");
    }

    // Register client as publisher
    public void registerPublisher(ClientHandler client) {
        publishers.add(client);
        System.out.println("Publisher registered. Total publishers: " + publishers.size());
    }

    // Register client as subscriber
    public void registerSubscriber(ClientHandler client) {
        subscribers.add(client);
        System.out.println("Subscriber registered. Total subscribers: " + subscribers.size());
    }

    // Remove client from all collections
    public void removeClient(ClientHandler client) {
        publishers.remove(client);
        subscribers.remove(client);
        allClients.remove(client);
        System.out.println("Client removed. Publishers: " + publishers.size() + ", Subscribers: " + subscribers.size());
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java Server <port>");
            System.exit(1);
        }

        try {
            int port = Integer.parseInt(args[0]);
            Server server = new Server(port);

            // Add shutdown hook for graceful termination
            Runtime.getRuntime().addShutdownHook(new Thread(server::stop));

            server.start();
        } catch (NumberFormatException e) {
            System.err.println("Invalid port number: " + args[0]);
            System.exit(1);
        }
    }
}

// ClientHandler class to handle individual client connections
class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Server server;
    private String clientType;
    private String clientInfo;
    private boolean connected = true;

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
        try {
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.clientInfo = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
        } catch (IOException e) {
            System.err.println("Error setting up client handler: " + e.getMessage());
            disconnect();
        }
    }

    @Override
    public void run() {
        try {
            System.out.println("New client connected from: " + clientInfo);

            // First message should be the client type (PUBLISHER or SUBSCRIBER)
            String firstMessage = in.readLine();
            if (firstMessage == null) {
                disconnect();
                return;
            }

            if ("PUBLISHER".equalsIgnoreCase(firstMessage)) {
                clientType = "PUBLISHER";
                server.registerPublisher(this);
                System.out.println("Client " + clientInfo + " registered as PUBLISHER");
            } else if ("SUBSCRIBER".equalsIgnoreCase(firstMessage)) {
                clientType = "SUBSCRIBER";
                server.registerSubscriber(this);
                System.out.println("Client " + clientInfo + " registered as SUBSCRIBER");
                sendMessage("Welcome! You are now subscribed to publisher messages.");
            } else {
                System.err.println("Invalid client type from " + clientInfo + ": " + firstMessage);
                disconnect();
                return;
            }

            // Listen for messages from client
            String message;
            while (connected && (message = in.readLine()) != null) {
                if ("terminate".equals(message)) {
                    System.out.println(clientType + " " + clientInfo + " requested termination");
                    break;
                }

                System.out.println(clientType + " " + clientInfo + ": " + message);

                // If it's a publisher, broadcast to all subscribers
                if ("PUBLISHER".equals(clientType)) {
                    server.broadcastToSubscribers(message, clientInfo);
                }
            }

        } catch (IOException e) {
            System.err.println("Error handling client " + clientInfo + ": " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    public boolean sendMessage(String message) {
        if (out != null && connected) {
            try {
                out.println(message);
                return !out.checkError();
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    public void disconnect() {
        connected = false;
        server.removeClient(this);

        try {
            if (in != null)
                in.close();
            if (out != null)
                out.close();
            if (socket != null && !socket.isClosed())
                socket.close();
        } catch (IOException e) {
            System.err.println("Error disconnecting client " + clientInfo + ": " + e.getMessage());
        }

        System.out.println("Client " + clientInfo + " (" + clientType + ") disconnected");
    }
}
