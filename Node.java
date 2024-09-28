package swim;
import java.io.*;
import java.net.*;

class Node {
    String id;
    boolean isAlive;
    long lastHeartbeat;
    String ipAddress;
    int port;

    Node(String id, String ipAddress, int port) {
        this.id = id;
        this.isAlive = true;
        this.lastHeartbeat = System.currentTimeMillis();
        this.ipAddress = ipAddress;
        this.port = port;
    }

    public void sendPing(Node targetNode) {
        try {
            DatagramSocket socket = new DatagramSocket();
            String message = "PING " + this.id;
            byte[] buffer = message.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(targetNode.ipAddress), targetNode.port);
            socket.send(packet);
            System.out.println("Pinging " + targetNode.id);
            socket.close();
        } catch (IOException e) {
            System.out.println("Failed to ping " + targetNode.id);
        }
    }

    public void receivePing(String fromNodeId) {
        System.out.println("Received ping from " + fromNodeId);
        this.lastHeartbeat = System.currentTimeMillis();
    }

    public void fail() {
        this.isAlive = false;
    }
}
