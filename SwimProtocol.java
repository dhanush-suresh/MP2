import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;


class SwimProtocol {
    
    private List<Node> nodes = new ArrayList<>();
    private Node selfNode;
    private int pingTimeout = 3000; // 3 seconds ping timeout

    public SwimProtocol(Node selfNode) {
        this.selfNode = selfNode;
    }

    public void joinIntroducer(String introducerIp, int introducerPort) {
        try {
            DatagramSocket socket = new DatagramSocket(); // UDP
            String message = "JOIN " + selfNode.id + " " + selfNode.port;
            byte[] buffer = message.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(introducerIp), introducerPort);
            socket.send(packet);

            // Receive membership list
            byte[] receiveBuffer = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
            socket.receive(receivePacket);
            String membershipMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());
            processMembershipList(membershipMessage);
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processMembershipList(String membershipMessage) {
        if (membershipMessage.startsWith("NODES")) {
            String[] parts = membershipMessage.split(" ");
            for (int i = 1; i < parts.length; i++) {
                String[] nodeInfo = parts[i].split(":");
                String nodeId = nodeInfo[0];
                String ipAddress = nodeInfo[1];
                int port = Integer.parseInt(nodeInfo[2]);
                Node node = new Node(nodeId, ipAddress, port);
                nodes.add(node);
            }
            System.out.println("Joined the network. Current nodes: " + nodes.size());
        }
    }

    public void startProtocol() {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(this::pingRandomNode, 0, 2, TimeUnit.SECONDS);
        executor.scheduleAtFixedRate(this::checkForFailures, 0, 2, TimeUnit.SECONDS);
        new Thread(this::startServer).start();
    }

    public void pingRandomNode() {
        if (nodes.isEmpty()) return;
        Node randomNode = nodes.get(new Random().nextInt(nodes.size()));
        if (!randomNode.id.equals(selfNode.id)) {
            selfNode.sendPing(randomNode);
        }
    }

    public void checkForFailures() {
        long currentTime = System.currentTimeMillis();
        for (Node node : nodes) {
            if (node.isAlive && currentTime - node.lastHeartbeat > pingTimeout) {
                System.out.println("Node " + node.id + " is suspected to have failed!");
                node.fail();
            }
        }
    }

    public void startServer() {
        try {
            DatagramSocket serverSocket = new DatagramSocket(selfNode.port);
            byte[] receiveBuffer = new byte[1024];

            while (true) {
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                serverSocket.receive(receivePacket);

                String message = new String(receivePacket.getData(), 0, receivePacket.getLength());
                String[] parts = message.split(" ");
                String messageType = parts[0];
                String fromNodeId = parts[1];

                if (messageType.equals("PING")) {
                    System.out.println("Received PING from " + fromNodeId);
                    for (Node node : nodes) {
                        if (node.id.equals(fromNodeId)) {
                            node.receivePing(fromNodeId);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: java SwimProtocol <node-id> <introducer-ip> <introducer-port>");
            return;
        }

        String nodeId = args[0];
        String introducerIp = args[1];
        int introducerPort = Integer.parseInt(args[2]);

        Node selfNode = new Node(nodeId, "127.0.0.1", 5000 + new Random().nextInt(1000));
        SwimProtocol swimProtocol = new SwimProtocol(selfNode);

        // Join the network via introducer
        swimProtocol.joinIntroducer(introducerIp, introducerPort);

        // Start the SWIM protocol
        swimProtocol.startProtocol();
    }
}
