# Topic-based Publisher-Subscriber Client-Server Application

This advanced client-server application implements **topic-based message filtering** in a Publisher-Subscriber pattern with support for **multiple concurrent topics**.

## Features

### **Requirements Implemented:**

- **Topic-based Filtering**: Messages are routed only to subscribers of the same topic
- **Fourth Command Argument**: Clients specify their topic as the 4th parameter
- **Multiple Topics**: Different publishers and subscribers can work with different topics simultaneously
- **Concurrent Topic Support**: Server handles unlimited topics concurrently
- **Topic Statistics**: Real-time monitoring of active topics and client counts

### **Technical Enhancements:**

- **Topic-based Collections**: Separate maps for publishers/subscribers by topic
- **Message Routing**: Smart message delivery based on topic matching
- **Topic Management**: Automatic topic creation and cleanup
- **Enhanced Logging**: Detailed topic information in all server logs
- **Client Validation**: Topic validation and formatting

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
java Client <server_ip> <port> PUBLISHER <topic>
```

Examples:

```bash
java Client localhost 5000 PUBLISHER SPORTS
java Client localhost 5000 PUBLISHER NEWS
java Client localhost 5000 PUBLISHER WEATHER
```

### Start Subscriber Clients:

```bash
java Client <server_ip> <port> SUBSCRIBER <topic>
```

Examples:

```bash
java Client localhost 5000 SUBSCRIBER SPORTS
java Client localhost 5000 SUBSCRIBER NEWS
java Client localhost 5000 SUBSCRIBER WEATHER
```

## How Topic-based Filtering Works

### **Server Architecture:**

```java
// Topic-based collections
Map<String, Set<ClientHandler>> publishersByTopic
Map<String, Set<ClientHandler>> subscribersByTopic
```

### **Message Flow:**

```
Publisher (SPORTS) ──┐
                     ├─→ Server ──┐
Publisher (NEWS) ────┘             ├─→ Subscriber (SPORTS)
                                   ├─→ Subscriber (SPORTS)
                                   └─→ Subscriber (NEWS)
```

### **Topic Filtering Logic:**

1. **Publisher** sends message with topic "SPORTS"
2. **Server** identifies all subscribers for topic "SPORTS"
3. **Message** is delivered ONLY to "SPORTS" subscribers
4. **Subscribers** of "NEWS" or "WEATHER" do NOT receive this message

## **Code Architecture**

### **Server.java Enhancements:**

- `Map<String, Set<ClientHandler>> publishersByTopic` - Publishers grouped by topic
- `Map<String, Set<ClientHandler>> subscribersByTopic` - Subscribers grouped by topic
- `broadcastToTopicSubscribers()` - Topic-specific message broadcasting
- `displayTopicStatistics()` - Real-time topic monitoring

### **Client.java Enhancements:**

- Fourth command line argument for topic specification
- Topic validation and formatting (uppercase, underscore replacement)
- Enhanced UI showing current topic in all displays
- Timestamp-based message display for better tracking

### **ClientHandler.java Enhancements:**

- Two-step registration: client type → topic
- Topic-aware client management
- Enhanced logging with topic information

## **Message Format**

Messages now include topic information:

```
[TOPIC:SPORTS] [PUBLISHER 192.168.1.100:54321]: Goal scored by Team A!
[TOPIC:NEWS] [PUBLISHER 192.168.1.101:54322]: Breaking news update
[TOPIC:WEATHER] [PUBLISHER 192.168.1.102:54323]: Temperature is 25°C
```

## **Topic Management Features**

### **Automatic Topic Creation:**

- Topics are created when first client (publisher or subscriber) joins
- No need to pre-define topics on server

### **Automatic Topic Cleanup:**

- Topics are removed when last client disconnects
- Prevents memory leaks from abandoned topics

### **Topic Statistics:**

```
=== CURRENT TOPIC STATISTICS ===
Active Topics: 3
  Topic 'SPORTS': 2 publishers, 3 subscribers
  Topic 'NEWS': 1 publishers, 5 subscribers
  Topic 'WEATHER': 0 publishers, 2 subscribers
Total clients: 13
================================
```

## **Example Session**

### Terminal 1 (Server):

```
>> java Main server 5000
Topic-based Publisher-Subscriber Server started on port 5000
Supports topic-based message filtering
Waiting for client connections...
Client 127.0.0.1:63247 registered as SUBSCRIBER for topic: SPORTS
Subscriber registered for topic 'SPORTS'. Total subscribers on this topic: 1
Client 127.0.0.1:63248 registered as PUBLISHER for topic: SPORTS
Publisher registered for topic 'SPORTS'. Total publishers on this topic: 1
PUBLISHER 127.0.0.1:63248 (topic: SPORTS): Goal scored!
Broadcasting on topic 'SPORTS': [TOPIC:SPORTS] [PUBLISHER 127.0.0.1:63248]: Goal scored!
Message broadcasted to 1 subscribers on topic: SPORTS
```

### Terminal 2 (Sports Subscriber):

```
>> java Main client localhost 5000 SUBSCRIBER SPORTS
Connected to server at localhost:5000
Client mode: SUBSCRIBER
Topic: SPORTS
========================================
=== SUBSCRIBER MODE - TOPIC: SPORTS ===
Listening for messages from publishers on topic: SPORTS
You will only receive messages published to this topic.
Type 'terminate' to exit.
=============================================
Welcome! You are now subscribed to topic: SPORTS

[18:45:23] [TOPIC:SPORTS] [PUBLISHER 127.0.0.1:63248]: Goal scored!
```

### Terminal 3 (Sports Publisher):

```
>> java Main client localhost 5000 PUBLISHER SPORTS
Connected to server at localhost:5000
Client mode: PUBLISHER
Topic: SPORTS
========================================
=== PUBLISHER MODE - TOPIC: SPORTS ===
Type messages to publish to all subscribers of topic: SPORTS
Your messages will only be sent to subscribers of this topic.
Type 'terminate' to exit.
============================================
Publish to SPORTS: Goal scored!
Publish to SPORTS: Match is getting exciting!
Publish to SPORTS: terminate
Terminating publisher connection...
```
