import org.opencv.core.Core;

public class MainServer {
    public static void main(String[] args) throws Exception {
        // Chạy WebSocket server trong một thread riêng
        Thread websocketThread = new Thread(() -> {
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
            Server2 server = new Server2(8887);
            server.start();
            System.out.println("WebSocket Server started on port: " + server.getPort());
        });

        // Chạy File server trong một thread riêng
        Thread fileServerThread = new Thread(() -> {
            try {
                FileServer.main(null);  // Gọi main method của FileServer
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Bắt đầu cả hai server
        websocketThread.start();
        fileServerThread.start();
    }
}
