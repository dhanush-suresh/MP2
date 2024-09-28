package swim;
import java.io.*;
import java.net.*;
import java.util.*;
import swim.Node;
class Introducer {
    private List<Node> nodes = new ArrayList<>();
    private int introducerPort;

    public Introducer(int port) {
        this.introducerPort = port;
    }

    public void startIntroducer() {
        try {
            DatagramSocket serverSocket = new DatagramSocket(introducerPort);
            byte[] receiveBuffer = new byte[1024];

            System.out.println("Introducer started on port " + introducerPort);

            while (true) {
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                serverSocket.receive(receivePacket);

                String message = new String(receivePacket.getData(), 0, receivePacket.getLength());
                String[] parts = message.split(" ");
                String messageType = parts[0];
                String nodeId = parts[1];
                String nodeIP = receivePacket.getAddress().getHostAddress();
                int nodePort = Integer.parseInt(parts[2]);

                if (messageType.equals("JOIN")) {
                    Node newNode = new Node(nodeId, nodeIP, nodePort);
                    System.out.println(nodeId + " is joining from " + nodeIP + ":" + nodePort);
                    nodes.add(newNode);
                    sendMembershipList(newNode, serverSocket);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMembershipList(Node newNode, DatagramSocket socket) {
        try {
            StringBuilder membershipList = new StringBuilder("NODES ");
            for (Node node : nodes) {
                membershipList.append(node.id).append(":").append(node.ipAddress).append(":").append(node.port).append(" ");
            }
            byte[] buffer = membershipList.toString().getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(newNode.ipAddress), newNode.port);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java Introducer <port>");
            return;
        }
        int port = Integer.parseInt(args[0]);
        Introducer introducer = new Introducer(port);
        introducer.startIntroducer();
    }
}
