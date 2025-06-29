import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Server {
    private ServerSocket serverSocket;
    private int port;
    private boolean running = false;
    private ExecutorService threadPool;

    // Topic-based collections to manage connected clients
    private final Map<String, Set<ClientHandler>> publishersByTopic = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, Set<ClientHandler>> subscribersByTopic = Collections.synchronizedMap(new HashMap<>());
    private final Set<ClientHandler> allClients = Collections.synchronizedSet(new HashSet<>());

    public Server(int port) {
        this.port = port;
        this.threadPool = Executors.newCachedThreadPool();
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            System.out.println("Pub-Sub Server started on port " + port);

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
                publishersByTopic.clear();
                subscribersByTopic.clear();
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

    // Broadcast message from publisher to subscribers of the same topic
    public void broadcastToTopicSubscribers(String message, String publisherInfo, String topic) {
        String formattedMessage = "[TOPIC:" + topic + "] [PUBLISHER " + publisherInfo + "]: " + message;
        System.out.println("Broadcasting on topic '" + topic + "': " + formattedMessage);

        Set<ClientHandler> topicSubscribers = subscribersByTopic.get(topic);
        if (topicSubscribers == null || topicSubscribers.isEmpty()) {
            System.out.println("No subscribers found for topic: " + topic);
            return;
        }

        synchronized (topicSubscribers) {
            Iterator<ClientHandler> iterator = topicSubscribers.iterator();
            int messagesSent = 0;
            while (iterator.hasNext()) {
                ClientHandler subscriber = iterator.next();
                if (subscriber.sendMessage(formattedMessage)) {
                    messagesSent++;
                } else {
                    // Remove disconnected subscriber
                    iterator.remove();
                    allClients.remove(subscriber);
                }
            }
            System.out.println("Message broadcasted to " + messagesSent + " subscribers on topic: " + topic);
        }
    }

    // Register client as publisher for a specific topic
    public void registerPublisher(ClientHandler client, String topic) {
        publishersByTopic.computeIfAbsent(topic, k -> Collections.synchronizedSet(new HashSet<>())).add(client);
        System.out.println("Publisher registered for topic '" + topic + "'. Total publishers on this topic: " +
                publishersByTopic.get(topic).size());
        displayTopicStatistics();
    }

    // Register client as subscriber for a specific topic
    public void registerSubscriber(ClientHandler client, String topic) {
        subscribersByTopic.computeIfAbsent(topic, k -> Collections.synchronizedSet(new HashSet<>())).add(client);
        System.out.println("Subscriber registered for topic '" + topic + "'. Total subscribers on this topic: " +
                subscribersByTopic.get(topic).size());
        displayTopicStatistics();
    }

    // Remove client from all collections
    public void removeClient(ClientHandler client, String clientType, String topic) {
        if ("PUBLISHER".equals(clientType)) {
            Set<ClientHandler> topicPublishers = publishersByTopic.get(topic);
            if (topicPublishers != null) {
                topicPublishers.remove(client);
                if (topicPublishers.isEmpty()) {
                    publishersByTopic.remove(topic);
                }
            }
        } else if ("SUBSCRIBER".equals(clientType)) {
            Set<ClientHandler> topicSubscribers = subscribersByTopic.get(topic);
            if (topicSubscribers != null) {
                topicSubscribers.remove(client);
                if (topicSubscribers.isEmpty()) {
                    subscribersByTopic.remove(topic);
                }
            }
        }

        allClients.remove(client);
        System.out.println("Client removed from topic '" + topic + "'");
        displayTopicStatistics();
    }

    // Display current topic statistics
    private void displayTopicStatistics() {
        System.out.println("=== CURRENT TOPIC STATISTICS ===");
        System.out
                .println("Active Topics: " + (publishersByTopic.keySet().size() + subscribersByTopic.keySet().size()));

        Set<String> allTopics = new HashSet<>();
        allTopics.addAll(publishersByTopic.keySet());
        allTopics.addAll(subscribersByTopic.keySet());

        for (String topic : allTopics) {
            int publishers = publishersByTopic.getOrDefault(topic, Collections.emptySet()).size();
            int subscribers = subscribersByTopic.getOrDefault(topic, Collections.emptySet()).size();
            System.out
                    .println("  Topic '" + topic + "': " + publishers + " publishers, " + subscribers + " subscribers");
        }
        System.out.println("Total clients: " + allClients.size());
        System.out.println("================================");
    }

    // Get list of available topics
    public Set<String> getAvailableTopics() {
        Set<String> allTopics = new HashSet<>();
        allTopics.addAll(publishersByTopic.keySet());
        allTopics.addAll(subscribersByTopic.keySet());
        return allTopics;
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

// ClientHandler class to handle individual client connections with topic
// support
class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Server server;
    private String clientType;
    private String topic;
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

            // Second message should be the topic
            String topicMessage = in.readLine();
            if (topicMessage == null) {
                disconnect();
                return;
            }

            this.topic = topicMessage.trim();

            if ("PUBLISHER".equalsIgnoreCase(firstMessage)) {
                clientType = "PUBLISHER";
                server.registerPublisher(this, topic);
                System.out.println("Client " + clientInfo + " registered as PUBLISHER for topic: " + topic);
            } else if ("SUBSCRIBER".equalsIgnoreCase(firstMessage)) {
                clientType = "SUBSCRIBER";
                server.registerSubscriber(this, topic);
                System.out.println("Client " + clientInfo + " registered as SUBSCRIBER for topic: " + topic);
                sendMessage("Welcome! You are now subscribed to topic: " + topic);

                // Send available topics info
                Set<String> availableTopics = server.getAvailableTopics();
                if (availableTopics.size() > 1) {
                    sendMessage("Available topics: " + String.join(", ", availableTopics));
                }
            } else {
                System.err.println("Invalid client type from " + clientInfo + ": " + firstMessage);
                disconnect();
                return;
            }

            // Listen for messages from client
            String message;
            while (connected && (message = in.readLine()) != null) {
                if ("terminate".equals(message)) {
                    System.out.println(clientType + " " + clientInfo + " (topic: " + topic + ") requested termination");
                    break;
                }

                System.out.println(clientType + " " + clientInfo + " (topic: " + topic + "): " + message);

                // If it's a publisher, broadcast to subscribers of the same topic
                if ("PUBLISHER".equals(clientType)) {
                    server.broadcastToTopicSubscribers(message, clientInfo, topic);
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
        if (clientType != null && topic != null) {
            server.removeClient(this, clientType, topic);
        }

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

        System.out.println("Client " + clientInfo + " (" + clientType + ", topic: " + topic + ") disconnected");
    }
}
