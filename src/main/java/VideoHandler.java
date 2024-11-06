import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.List;

public class VideoHandler implements Runnable {
    private Socket clientSocket;
    private List<JLabel> videoLabels;
    private static int clientCount = 0; // Đếm số lượng client

    public VideoHandler(Socket socket, List<JLabel> videoLabels) {
        this.clientSocket = socket;
        this.videoLabels = videoLabels;
    }

    @Override
    public void run() {
        try {
            InputStream input = clientSocket.getInputStream();
            DataInputStream dis = new DataInputStream(input);

            while (true) {
                int length = dis.readInt(); // Đọc kích thước của mảng byte
                byte[] imageBytes = new byte[length];
                dis.readFully(imageBytes); // Đọc mảng byte

                BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageBytes)); // Chuyển đổi byte[] thành BufferedImage
                // Hiển thị video trong giao diện
                if (clientCount < videoLabels.size()) {
                    ImageIcon imageIcon = new ImageIcon(img);
                    videoLabels.get(clientCount).setIcon(imageIcon);
                    clientCount++;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
