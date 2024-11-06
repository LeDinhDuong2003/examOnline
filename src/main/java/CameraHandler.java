
import org.java_websocket.client.WebSocketClient;
import org.opencv.core.Mat;
import org.opencv.highgui.HighGui;
import org.opencv.videoio.VideoCapture;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class CameraHandler {
    private boolean isCameraRunning;
    private Timer timer;
    private VideoCapture camera; // VideoCapture cho camera
    private static final String SERVER_ADDRESS = "ws://localhost:8887";

    public void startCamera(JLabel cameraLabel, WebSocketClient client) {
        if (isCameraRunning) {
            return;
        }
        isCameraRunning = true;

        // Khởi tạo camera
        camera = new VideoCapture(0); // 0 để mở camera mặc định
        if (!camera.isOpened()) {
            JOptionPane.showMessageDialog(null, "Không thể mở camera!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        Mat frameFromCamera = new Mat();
        // Gửi video đến server
        new Thread(() -> {
            while (true) {
                if (camera.isOpened()) {
                    camera.read(frameFromCamera);
                    BufferedImage image = matToBufferedImage(frameFromCamera);
                    byte[] imageBytes = bufferedImageToBytes(image); // Chuyển đổi BufferedImage thành byte[]
                    client.send(imageBytes); // Gửi dữ liệu ảnh đến server
                }
            }
        }).start();

        timer = new Timer(100, e -> {
            Mat frame = new Mat();
            camera.read(frame); // Đọc khung hình từ camera
            if (!frame.empty()) {
                // Chuyển đổi Mat thành ImageIcon
                ImageIcon icon = new ImageIcon(HighGui.toBufferedImage(frame));
                cameraLabel.setIcon(icon); // Cập nhật JLabel với hình ảnh từ camera
            }
        });
        timer.start();
    }

    public void stopCamera() {
        isCameraRunning = false;
        if (timer != null) {
            timer.stop();
        }

        if (camera != null) {
            camera.release(); // Giải phóng camera
        }
    }
    private static BufferedImage matToBufferedImage(Mat matrix) {
        int type = BufferedImage.TYPE_BYTE_GRAY;
        if (matrix.channels() > 1) {
            type = BufferedImage.TYPE_3BYTE_BGR;
        }
        int bufferSize = matrix.channels() * matrix.cols() * matrix.rows();
        byte[] b = new byte[bufferSize];
        matrix.get(0, 0, b); // Lấy dữ liệu từ Mat vào byte[]
        BufferedImage image = new BufferedImage(matrix.cols(), matrix.rows(), type);
        image.getRaster().setDataElements(0, 0, matrix.cols(), matrix.rows(), b);
        return image;
    }

    private static byte[] bufferedImageToBytes(BufferedImage image) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "jpg", baos); // Ghi BufferedImage thành byte[]
            return baos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}