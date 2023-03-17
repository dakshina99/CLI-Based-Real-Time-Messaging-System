import java.net.URI;
import java.util.Scanner;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.WebSocketClient;

public class MyClient2 {

    public static void main(String[] args) throws Exception {
        String serverUri = "ws://localhost:8080/";

        WebSocketClient client = new WebSocketClient();
        client.start();

        MyWebSocket socket = new MyWebSocket();
        client.connect(socket, URI.create(serverUri)).get();

        Scanner scanner = new Scanner(System.in);
        while (true) {
            String message = scanner.nextLine();
            socket.sendMessage(message);
        }
    }

    @WebSocket
    public static class MyWebSocket {

        private Session session;

        @OnWebSocketConnect
        public void onConnect(Session session) {
            System.out.println("WebSocket connected to server");
            this.session = session;
            this.session.setIdleTimeout(-1); // Disable idle timeout
        }

        @OnWebSocketMessage
        public void onMessage(String message) {
            System.out.println("WebSocket message received from server: " + message);
        }

        public void sendMessage(String message) {
            if (session != null && session.isOpen()) {
                try {
                    session.getRemote().sendString(message);
                    System.out.println("Sent message to server: " + message);
                } catch (Exception e) {
                    System.err.println("Failed to send message to server: " + e.getMessage());
                }
            }
        }
    }
}
