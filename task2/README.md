# Publisher-Subscriber Client-Server Application

This enhanced client-server application implements a **Publisher-Subscriber** pattern with support for **multiple concurrent connections**.

## Features

### **Requirements Met:**

- **Multiple Concurrent Connections**: Server handles unlimited simultaneous clients using thread pool
- **Publisher/Subscriber Roles**: Clients specify their role as command line argument
- **Message Broadcasting**: Publishers send messages to ALL subscribers
- **Selective Display**: Publisher messages are NOT shown to other publishers
- **Thread-Safe Operations**: Proper synchronization for concurrent access

### **Technical Improvements:**

- **Multi-threading**: Each client connection runs in separate thread
- **Thread Pool**: Efficient resource management with `ExecutorService`
- **Concurrent Collections**: Thread-safe client management
- **Real-time Messaging**: Subscribers receive messages immediately
- **Graceful Shutdown**: Proper cleanup of resources and connections

## Usage Instructions

### Start the Server:

```bash
java Server <port>
```

Example:

```bash
java Server 5000
```

### Start Publisher Clients:

```bash
java Client <server_ip> <port> PUBLISHER
```

Examples:

```bash
java Client localhost 5000 PUBLISHER
java Client 192.168.10.2 5000 PUBLISHER
```

### Start Subscriber Clients:

```bash
java Client <server_ip> <port> SUBSCRIBER
```

Examples:

```bash
java Client localhost 5000 SUBSCRIBER
java Client 192.168.10.2 5000 SUBSCRIBER
```

## How It Works

### **Server Architecture:**

1. **Main Thread**: Accepts new client connections
2. **Client Threads**: Each client handled by dedicated thread (`ClientHandler`)
3. **Collections Management**:
   - `publishers` - Set of publisher clients
   - `subscribers` - Set of subscriber clients
   - `allClients` - Set of all connected clients

### **Publisher Workflow:**

1. Connect to server with "PUBLISHER" role
2. Register as publisher in server collections
3. Send messages via console input
4. Messages are broadcasted to ALL subscribers
5. Publisher does NOT see messages from other publishers

### **Subscriber Workflow:**

1. Connect to server with "SUBSCRIBER" role
2. Register as subscriber in server collections
3. Listen for incoming messages from publishers
4. Display received messages in real-time
5. Can only terminate, cannot send messages

### 🔄 **Message Flow:**

```
Publisher 1 ──┐
               ├─→ Server ──┐
Publisher 2 ──┘             ├─→ Subscriber 1
                             ├─→ Subscriber 2
                             └─→ Subscriber N
```

## 🏗️ **Code Structure**

### **Server.java**

- `Server` class: Main server logic, client management
- `ClientHandler` class: Individual client thread handler
- Thread-safe collections for publishers/subscribers
- Broadcasting logic for message distribution

### **Client.java**

- Unified client with Publisher/Subscriber modes
- Separate input/output handling for each mode
- Background thread for receiving messages (Subscribers)
- Interactive console for sending messages (Publishers)

## **Example Session**

### Terminal 1 (Server):

```
>> java Server 5000
Multi-client Server started on port 5000
Waiting for client connections...
New client connected from: 127.0.0.1:63247
Client 127.0.0.1:63247 registered as SUBSCRIBER
Subscriber registered. Total subscribers: 1
New client connected from: 127.0.0.1:63248
Client 127.0.0.1:63248 registered as PUBLISHER
Publisher registered. Total publishers: 1
PUBLISHER 127.0.0.1:63248: Hello subscribers!
Broadcasting: [PUBLISHER 127.0.0.1:63248]: Hello subscribers!
Message broadcasted to 1 subscribers
```

### Terminal 2 (Subscriber):

```
>> java Client localhost 5000 SUBSCRIBER
Connected to server at localhost:5000
Client mode: SUBSCRIBER
=== SUBSCRIBER MODE ===
Listening for messages from publishers...
Type 'terminate' to exit.
========================
Welcome! You are now subscribed to publisher messages.

[PUBLISHER 127.0.0.1:63248]: Hello subscribers!
```

### Terminal 3 (Publisher):

```
PS> java Client localhost 5000 PUBLISHER
Connected to server at localhost:5000
Client mode: PUBLISHER
=== PUBLISHER MODE ===
Type messages to publish to all subscribers.
Type 'terminate' to exit.
=======================
Publish message: Hello subscribers!
Publish message: How is everyone doing?
Publish message: terminate
Terminating publisher connection...
Client disconnected.
```
