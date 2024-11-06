import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.highgui.HighGui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

public class Server2 extends WebSocketServer {
    private static final long TIMEOUT = 5000; // Timeout duration
    private List<WebSocket> clients = new ArrayList<>();
    private List<JLabel> videoLabels = new ArrayList<>();
    private List<JLabel> nameLabels = new ArrayList<>();  // New list for name labels
    private Map<WebSocket, Integer> clientIndexMap = new HashMap<>();
    private Map<WebSocket, Long> lastReceivedTime = new ConcurrentHashMap<>();
    private Map<WebSocket, String> clientNames = new HashMap<>();  // Store client names
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    // Adding a JTable to display client information (name instead of IP)
    private DefaultTableModel tableModel;
    private JTable clientTable;

    public Server2(int port) {
        super(new InetSocketAddress(port));
        initializeUI();
        startTimeoutCheck();
    }

    private void initializeUI() {
        JFrame frame = new JFrame("Video Chat Server");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 600);
        frame.setLayout(new BorderLayout());

        // Video display panel (grid layout for videos and names)
        JPanel videoPanel = new JPanel(new GridLayout(2, 2));  // 2 columns, 2 rows for video and names
        for (int i = 0; i < 4; i++) {
            JPanel clientPanel = new JPanel(new BorderLayout());
            JLabel videoLabel = new JLabel();
            videoLabel.setHorizontalAlignment(JLabel.CENTER);
            videoLabel.setVerticalAlignment(JLabel.CENTER);
            videoLabels.add(videoLabel);

            JLabel nameLabel = new JLabel("Client " + (i + 1)); // Default label for name
            nameLabel.setHorizontalAlignment(JLabel.CENTER);
            nameLabels.add(nameLabel);

            clientPanel.add(videoLabel, BorderLayout.CENTER);
            clientPanel.add(nameLabel, BorderLayout.SOUTH); // Name label below video

            videoPanel.add(clientPanel);
        }

        // Client information table
        String[] columnNames = {"Client Name"};  // Display Client Name instead of IP
        tableModel = new DefaultTableModel(columnNames, 0);
        clientTable = new JTable(tableModel);
        JScrollPane tableScrollPane = new JScrollPane(clientTable);

        frame.add(videoPanel, BorderLayout.CENTER);
        frame.add(tableScrollPane, BorderLayout.EAST);

        frame.setVisible(true);
    }

    private void startTimeoutCheck() {
        scheduler.scheduleAtFixedRate(() -> {
            long currentTime = System.currentTimeMillis();
            for (Map.Entry<WebSocket, Long> entry : lastReceivedTime.entrySet()) {
                WebSocket client = entry.getKey();
                long lastTime = entry.getValue();
                if (currentTime - lastTime > TIMEOUT) {
                    Integer index = clientIndexMap.get(client);
                    if (index != null) {
                        videoLabels.get(index).setIcon(null);
                        nameLabels.get(index).setText("Client " + (index + 1));  // Reset name
                    }
                }
            }
        }, TIMEOUT, TIMEOUT, TimeUnit.MILLISECONDS);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        clients.add(conn);
        int index = clients.size() - 1;
        clientIndexMap.put(conn, index);
        lastReceivedTime.put(conn, System.currentTimeMillis());
        System.out.println("New client connected: " + conn.getRemoteSocketAddress());

        // Add default name "Client X" to the table
//        clientNames.put(conn, "Client " + (index + 1));
//        tableModel.addRow(new Object[]{clientNames.get(conn)});
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        Integer index = clientIndexMap.remove(conn);
        tableModel.addRow(new Object[]{clientNames.get(conn).substring("CLIENT_NAME:".length()) + " - rời phòng"});
        clients.remove(conn);
        lastReceivedTime.remove(conn);
        clientNames.remove(conn);
        System.out.println("Client disconnected: " + conn.getRemoteSocketAddress());

        // Remove client from the table
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (tableModel.getValueAt(i, 0).toString().substring("CLIENT_NAME:".length()).equals(clientNames.get(conn))) {
                tableModel.removeRow(i);
                break;
            }
        }

        if (index != null) {
            videoLabels.get(index).setIcon(null);
            nameLabels.get(index).setText("Client " + (index + 1));  // Reset name when disconnected
        }

    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        if (message.startsWith("CLIENT_NAME:")) {
            String clientName = message.substring("CLIENT_NAME:".length());
            int index = clientIndexMap.get(conn);
//            clientNames.put(conn, clientName);  // Update the client's name
            updateClientNameLabel(clientName, index);
            updateTableWithClientName(conn, clientName);  // Update name in the table
//            tableModel.addRow(new Object[]{message});
            clientNames.put(conn,message);
            tableModel.addRow(new Object[]{clientNames.get(conn).substring("CLIENT_NAME:".length()) + " - vào phòng"});
        }else{
            tableModel.addRow(new Object[]{clientNames.get(conn).substring("CLIENT_NAME:".length())+ " - " + message});
        }

    }

    private void updateClientNameLabel(String clientName, int index) {
        nameLabels.get(index).setText(clientName);  // Update the corresponding name label
    }

    private void updateTableWithClientName(WebSocket conn, String clientName) {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (tableModel.getValueAt(i, 0).equals(clientNames.get(conn))) {
                tableModel.setValueAt(clientName, i, 0);  // Update the table with the new name
                break;
            }
        }
    }

    @Override
    public void onError(WebSocket webSocket, Exception e) {
        // Error handling if needed
    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {
        byte[] byteArray = new byte[message.remaining()];
        message.get(byteArray);
        // Decode image
        MatOfByte matOfByte = new MatOfByte(byteArray);
        Mat img = Imgcodecs.imdecode(matOfByte, Imgcodecs.IMREAD_COLOR);

        Integer index = clientIndexMap.get(conn);
        if (index != null) {
            updateVideoOnServer(img, index);
            lastReceivedTime.put(conn, System.currentTimeMillis());
        }
        sendToAllClients(message, conn);
    }

    private void updateVideoOnServer(Mat img, int index) {
        Mat rgbImg = new Mat();
        Imgproc.cvtColor(img, rgbImg, Imgproc.COLOR_BGR2RGB);
        ImageIcon imageIcon = new ImageIcon(HighGui.toBufferedImage(rgbImg));
        videoLabels.get(index).setIcon(imageIcon);
    }

    private void sendToAllClients(ByteBuffer message, WebSocket sender) {
        for (WebSocket client : clients) {
            if (client.isOpen() && client != sender) {
                client.send(message);
            }
        }
    }

    @Override
    public void onStart() {
        System.out.println("Server started on port: " + this.getPort());
    }

    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        Server2 server = new Server2(8887);
        server.start();
        System.out.println("Server started on port: " + server.getPort());
    }
}
