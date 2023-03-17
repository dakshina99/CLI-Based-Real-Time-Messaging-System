import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.eclipse.jetty.websocket.server.WebSocketHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

public class MyWebSocketServer {

    private static final Map<String, Session> topicSubscribers = new HashMap<>();
    private static final Set<Session> activeSessions = new HashSet<>();

    public static void createTopic(String topic) {
        // Add the new topic to the map
        topicSubscribers.put(topic, null);

        System.out.println("Created topic " + topic);
    }

    public static void publishToTopic(String topic, String message) {
        // Get the set of subscribers for this topic
        Session subscriber = topicSubscribers.get(topic);

        if (subscriber != null) {
            if (subscriber.isOpen()) {
                subscriber.getRemote().sendStringByFuture(message);
            }
        } else {
            System.out.println("No subscriber for topic: " + topic);
        }
    }

    public static void main(String[] args) throws Exception {
        // Create a new Jetty server instance
        Server server = new Server();

        // Set the maximum idle time for the server to never timeout
        server.setAttribute("org.eclipse.jetty.websocket.api.WebSocketPolicy.idleTimeout", 0);

        // Create a new WebSocketHandler instance
        WebSocketHandler webSocketHandler = new WebSocketHandler() {
            @Override
            public void configure(WebSocketServletFactory factory) {
                // Set the maximum idle time for each WebSocket session to never timeout
                factory.getPolicy().setIdleTimeout(0);
                factory.register(MyWebSocketHandler.class);
            }
        };

        // Set the WebSocketHandler for the server
        server.setHandler(webSocketHandler);

        // Create a new ServerConnector
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(8080);
        connector.setHost("localhost");
        server.addConnector(connector);

        // Start the server
        server.start();

        System.out.println("Server started on port " + connector.getPort());

        // Listen for new topics/messages in a never-ending loop
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.println("Choose an option:");
            System.out.println("1. Publish a new topic");
            System.out.println("2. Publish a message to a specific topic");

            try {
                int option = Integer.parseInt(reader.readLine().trim());
                switch (option) {
                    case 1:
                        System.out.println("Enter a new topic name:");
                        String topic = reader.readLine().trim();
                        createTopic(topic);
                        break;
                    case 2:
                        System.out.println("Enter a topic name:");
                        String topicName = reader.readLine().trim();
                        System.out.println("Enter a message to publish:");
                        String message = reader.readLine().trim();
                        publishToTopic(topicName, message);
                        break;
                    default:
                        System.out.println("Invalid option");
                        break;
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input, please enter a number");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }





    @WebSocket
    public static class MyWebSocketHandler {

        private static final String MESSAGE_TYPE_SUBSCRIBE = "subscribe";
        private static final String MESSAGE_TYPE_UNSUBSCRIBE = "unsubscribe";

        @OnWebSocketConnect
        public void onWebSocketConnect(Session session) {
            // Add the new session to the list of active sessions
            activeSessions.add(session);
            System.out.println("New session connected");
            // Send a welcome message to the client
            String message = "Welcome to the WebSocket server!";
            session.getRemote().sendStringByFuture(message);
        }

        @OnWebSocketMessage
        public void onWebSocketText(Session session, String message) {
            // Handle text message
            try {
                handleMessage(session, message);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @OnWebSocketClose
        public void onWebSocketClose(Session session, int statusCode, String reason) {
            // Handle close
            activeSessions.remove(session);
        }

        @OnWebSocketError
        public void onWebSocketError(Session session, Throwable cause) {
            // Handle error
        }

        private void handleMessage(Session session, String message) throws IOException {
            String[] parts = message.split(":", 2);
            String messageType = parts[0];
            String payload = parts.length > 1 ? parts[1] : "";

            switch (messageType) {
                case MESSAGE_TYPE_SUBSCRIBE:
                    subscribeToTopic(session, payload);
                    break;
                case MESSAGE_TYPE_UNSUBSCRIBE:
                    unsubscribeFromTopic(payload);
                    break;
                default:
                    // Ignore other types of messages
                    break;
            }
        }

        private void subscribeToTopic(Session session, String topic) {
            topicSubscribers.put(topic, session);
            System.out.println("Subscribed to topic " + topic);
        }

        private void unsubscribeFromTopic(String topic) {
            topicSubscribers.remove(topic);
            System.out.println("Unsubscribed from topic " + topic);
        }
    }

}

