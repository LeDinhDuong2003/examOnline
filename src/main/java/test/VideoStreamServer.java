package test;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;
import javax.swing.*;

public class VideoStreamServer {
    private static final int PORT = 8888;
    private static List<ClientInfo> clients = new ArrayList<>();
    private static Map<InetAddress, JLabel> clientLabels = new ConcurrentHashMap<>();

    public static void main(String[] args) throws Exception {
        DatagramSocket socket = new DatagramSocket(PORT);
        JFrame frame = new JFrame("Video Chat Server");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLayout(new GridLayout(2, 2)); // Create a grid layout for displaying video

        // Create JLabels to display video from clients
        for (int i = 0; i < 4; i++) { // Create 4 JLabels for 4 clients
            JLabel label = new JLabel();
            label.setHorizontalAlignment(JLabel.CENTER);
            label.setVerticalAlignment(JLabel.CENTER);
            frame.add(label);
        }

        frame.setVisible(true);

        byte[] receiveBuffer = new byte[65535];

        while (true) {
            DatagramPacket packet = new DatagramPacket(receiveBuffer, receiveBuffer.length);
            socket.receive(packet);

            InetAddress clientAddress = packet.getAddress();
            int clientPort = packet.getPort();
            ClientInfo clientInfo = new ClientInfo(clientAddress, clientPort);

            if (!clients.contains(clientInfo)) {
                clients.add(clientInfo);
                System.out.println("New client connected: " + clientAddress + ":" + clientPort);
            }

            // Update the label for this client
            int clientIndex = clients.indexOf(clientInfo);
            JLabel label = (JLabel) frame.getContentPane().getComponent(clientIndex);
            clientLabels.put(clientAddress, label);

            BufferedImage img = ImageIO.read(new ByteArrayInputStream(packet.getData(), 0, packet.getLength()));
            if (img != null) {
                ImageIcon imageIcon = new ImageIcon(img);
                label.setIcon(imageIcon);
            }

            // Forward the packet to all other clients
            for (ClientInfo client : clients) {
                if (!client.equals(clientInfo)) {
                    DatagramPacket forwardPacket = new DatagramPacket(
                            packet.getData(), packet.getLength(),
                            client.getAddress(), client.getPort());
                    socket.send(forwardPacket);
                }
            }
        }
    }

    static class ClientInfo {
        private InetAddress address;
        private int port;

        public ClientInfo(InetAddress address, int port) {
            this.address = address;
            this.port = port;
        }

        public InetAddress getAddress() {
            return address;
        }

        public int getPort() {
            return port;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ClientInfo that = (ClientInfo) o;
            return port == that.port && address.equals(that.address);
        }

        @Override
        public int hashCode() {
            return address.hashCode() * 31 + port;
        }
    }
}
