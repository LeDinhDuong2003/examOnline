import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.highgui.HighGui;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

public class Server extends WebSocketServer {
    private static final long TIMEOUT = 5000; // Thời gian timeout
    private static final String SAVE_DIR = "C:\\Users\\ASUS\\Desktop\\exam"; // Thư mục lưu tệp
    private List<WebSocket> clients = new ArrayList<>();
    private List<JLabel> videoLabels = new ArrayList<>();
    private Map<WebSocket, Integer> clientIndexMap = new HashMap<>();
    private Map<WebSocket, Long> lastReceivedTime = new ConcurrentHashMap<>();
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public Server(int port) {
        super(new InetSocketAddress(port));
        initializeUI();
        startTimeoutCheck();
    }

    private void initializeUI() {
        JFrame frame = new JFrame("Video Chat Server");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLayout(new GridLayout(2, 2));

        for (int i = 0; i < 4; i++) {
            JLabel label = new JLabel();
            label.setHorizontalAlignment(JLabel.CENTER);
            label.setVerticalAlignment(JLabel.CENTER);
            videoLabels.add(label);
            frame.add(label);
        }

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
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        Integer index = clientIndexMap.remove(conn);
        clients.remove(conn);
        lastReceivedTime.remove(conn);
        System.out.println("Client disconnected: " + conn.getRemoteSocketAddress());
        if (index != null) {
            videoLabels.get(index).setIcon(null);
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        // Không sử dụng cho video
    }

    @Override
    public void onError(WebSocket webSocket, Exception e) {

    }

    @Override
    public void onMessage(WebSocket conn , ByteBuffer message) {
        byte[] byteArray = new byte[message.remaining()];
        message.get(byteArray);

        // Giải mã hình ảnh
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
        Server server = new Server(8887);
        server.start();
        System.out.println("Server started on port: " + server.getPort());
    }
}
