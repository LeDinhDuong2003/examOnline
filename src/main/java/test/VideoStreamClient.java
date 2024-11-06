package test;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class VideoStreamClient {
    static { System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 8888;

    public static void main(String[] args) throws Exception {
        DatagramSocket socket = new DatagramSocket();
        InetAddress serverAddress = InetAddress.getByName(SERVER_ADDRESS);

        JFrame frame = new JFrame("Video Stream Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JLabel videoLabel = new JLabel();
        frame.add(videoLabel);
        frame.setSize(800, 600);
        frame.setVisible(true);

        // Thread to receive video packets
        new Thread(() -> {
            try {
                byte[] receiveBuffer = new byte[65535];
                while (true) {
                    DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                    socket.receive(receivePacket);

                    ByteArrayInputStream bais = new ByteArrayInputStream(receivePacket.getData(), 0, receivePacket.getLength());
                    BufferedImage image = ImageIO.read(bais);

                    if (image != null) {
                        ImageIcon imageIcon = new ImageIcon(image);
                        videoLabel.setIcon(imageIcon);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        // Thread to send video packets captured from webcam
        new Thread(() -> {
            try {
                VideoCapture capture = new VideoCapture(0);
                if (!capture.isOpened()) {
                    System.out.println("Error: Camera not found.");
                    return;
                }

                Mat frameMat = new Mat();
                while (true) {
                    capture.read(frameMat);
                    if (!frameMat.empty()) {
                        byte[] imageData = matToByteArray(frameMat);
                        DatagramPacket sendPacket = new DatagramPacket(imageData, imageData.length, serverAddress, SERVER_PORT);
                        socket.send(sendPacket);

                        Thread.sleep(100); // Adjust frame rate as needed
                    }
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static byte[] matToByteArray(Mat frame) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(matToBufferedImage(frame), "jpg", baos);
        return baos.toByteArray();
    }

    private static BufferedImage matToBufferedImage(Mat frame) {
        int type = frame.channels() > 1 ? BufferedImage.TYPE_3BYTE_BGR : BufferedImage.TYPE_BYTE_GRAY;
        BufferedImage image = new BufferedImage(frame.width(), frame.height(), type);
        frame.get(0, 0, ((DataBufferByte) image.getRaster().getDataBuffer()).getData());
        return image;
    }
}
