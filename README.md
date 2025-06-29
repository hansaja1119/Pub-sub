# Client-Server Socket Programming

This repository contains three progressive tasks implementing client-server socket applications in Java, evolving from basic communication to advanced topic-based messaging systems.

## Task Overview

### [Task 1: Basic Client-Server Application](task1/)

**Simple one-to-one socket communication**

- Single client connects to server
- Client sends messages to server
- Messages displayed on server console
- Graceful termination with "terminate" command

**Usage:**

```bash
java Server 5000
java Client localhost 5000
```

### [Task 2: Publisher-Subscriber System](task2/)

**Multi-client messaging with role-based communication**

- Multiple concurrent client connections
- Publisher/Subscriber roles
- Publishers broadcast to ALL subscribers
- Publishers don't see each other's messages
- Thread-safe multi-threading

**Usage:**

```bash
java Server 5000
java Client localhost 5000 PUBLISHER
java Client localhost 5000 SUBSCRIBER
```

### [Task 3: Topic-based Filtered Messaging](task3/)

**Advanced messaging with topic-based filtering**

- Topic-based message filtering
- Multiple concurrent topics
- Publishers send only to matching topic subscribers
- Real-time topic statistics
- Automatic topic management

**Usage:**

```bash
java Server 5000
java Client localhost 5000 PUBLISHER SPORTS
java Client localhost 5000 SUBSCRIBER SPORTS
```
