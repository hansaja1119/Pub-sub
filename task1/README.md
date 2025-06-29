# Client-Server Socket Application

This is a Java-based client-server socket application that demonstrates communication between a server and client using TCP sockets.

## Features

- **Server**: Listens for client connections on a specified port
- **Client**: Connects to the server and sends messages
- **Command Line Interface**: Simple CLI for both server and client
- **Graceful Termination**: Client can disconnect by typing "terminate"

## How to Run

#### Start the Server:

```bash
java Server <port>
```

Example:

```bash
java Server 5000
```

#### Start the Client:

```bash
java Client <server_ip> <port>
```

Examples:

```bash
java Client localhost 5000
java Client 192.168.10.2 5000
```

## Usage Instructions

1. **Start the Server first**: The server must be running before any client can connect.

2. **Connect the Client**: Run the client with the server's IP address and port number.

3. **Send Messages**: After connecting, type any message in the client terminal. The message will appear on the server's console.

4. **Terminate Connection**: Type "terminate" (without quotes) in the client to disconnect and close both client and server.

## Example Session

### Terminal 1 (Server):

```
>> java Server 5000
Server started on port 5000
Waiting for client connection...
Client connected from: 127.0.0.1
Client: Hello from client!
Client: How are you?
Client requested termination. Closing connection...
Server stopped.
```

### Terminal 2 (Client):

```
>> java Client localhost 5000
Connected to server at localhost:5000
Type messages to send to server. Type 'terminate' to exit.
Enter message: Hello from client!
Enter message: How are you?
Enter message: terminate
Terminating client connection...
Client disconnected.
```

## Technical Details

- **Programming Language**: Java
- **Protocol**: TCP
- **Socket Types**: ServerSocket (server), Socket (client)
- **Input/Output**: BufferedReader and PrintWriter for message handling
- **Threading**: Single-threaded (server handles one client at a time)

## File Structure

- `Server.java` - Server implementation
- `Client.java` - Client implementation
