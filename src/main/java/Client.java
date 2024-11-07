import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.opencv.core.Core;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Client {
    private JFrame frame;
    private JPanel panel;
    private JLabel cameraLabel;
    private JButton uploadFileButton;
    private JTextArea compilationOutputArea;
    private JTextArea textArea;
    private File selectedFile;
    private JLabel timerLabel; // Label to display the countdown timer
    private Timer timer;
    private int remainingTime; // Remaining time in seconds

    private CameraHandler cameraHandler;

    private static final String SERVER_ADDRESS = "ws://localhost:8887";
    private String clientName;  // To store the client's name
    private static boolean connected = true;

    // WebSocket Client
    private final WebSocketClient client = new WebSocketClient(URI.create(SERVER_ADDRESS)) {
        @Override
        public void onOpen(ServerHandshake handshakedata) {
            System.out.println("Connected to server");
            sendNameToServer(); // Send the name to the server when connection is established
        }

        @Override
        public void onMessage(String message) {
            // Handle text messages if needed
        }

        @Override
        public void onMessage(ByteBuffer message) {
            // Handle byte messages if needed
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            System.out.println("Disconnected from server: " + reason);
        }

        @Override
        public void onError(Exception ex) {
            connected = false;
            JOptionPane.showMessageDialog(frame, "Unable to connect to server. The program will now exit.", "Connection Error", JOptionPane.ERROR_MESSAGE);
            // Dừng chương trình sau khi nhấn OK
            System.exit(1);
//            ex.printStackTrace();
        }

    };

    public Client() {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        clientName = promptForClientName(); // Prompt for the client name before starting
        initializeUI();
        cameraHandler = new CameraHandler();
        cameraHandler.startCamera(cameraLabel, client); // Automatically start camera
    }

    private void initializeUI() {
        frame = new JFrame("Online Exam System");
        panel = new JPanel(new BorderLayout());

        // Panel for the timer label
        JPanel timerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        timerLabel = new JLabel("Time remaining: 00:00:00", SwingConstants.LEFT);
        timerLabel.setFont(new Font("Arial", Font.BOLD, 16));
        timerPanel.add(timerLabel);
        panel.add(timerPanel, BorderLayout.NORTH);

        // Panel for the camera label and text area
        JPanel centerPanel = new JPanel(new BorderLayout());

        // Camera label
        cameraLabel = new JLabel("Camera feed will be here", SwingConstants.CENTER);
        cameraLabel.setPreferredSize(new Dimension(320, 240));
        cameraLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        centerPanel.add(cameraLabel, BorderLayout.WEST);

        // Text area for displaying exercise questions
        textArea = createTextArea("This is a static text.\n"
                + "You can add information related to the exam here.\n"
                + "This text will scroll if it's too long.\n"
                + "Good luck with the exam!");
        JScrollPane textScrollPane = new JScrollPane(textArea);
        centerPanel.add(textScrollPane, BorderLayout.CENTER);

        panel.add(centerPanel, BorderLayout.CENTER);

        // Output area for compilation and execution outputs
        compilationOutputArea = createTextArea("");
        JScrollPane compilationOutputScrollPane = new JScrollPane(compilationOutputArea);
        compilationOutputScrollPane.setPreferredSize(new Dimension(400, 200));
        panel.add(compilationOutputScrollPane, BorderLayout.SOUTH);

        // Panel for exercise buttons and upload/submit actions
        JPanel buttonPanel = new JPanel(new BorderLayout());

        // Button panel for upload and submit actions
        JPanel actionButtonPanel = new JPanel();
        uploadFileButton = new JButton("Upload Java File");
        JButton submitButton = new JButton("Submit Exam");
        actionButtonPanel.add(uploadFileButton);
        actionButtonPanel.add(submitButton);
        buttonPanel.add(actionButtonPanel, BorderLayout.NORTH);

        // Panel for exercise buttons
        JPanel exercisePanel = new JPanel(new GridLayout(4, 1)); // 4 rows for 4 exercises
        JButton[] exerciseButtons = new JButton[4];

        // Create buttons for exercises
        for (int i = 0; i < 4; i++) {
            int index = i; // Final variable for use in lambda
            exerciseButtons[i] = new JButton("Exercise " + (i + 1));
            exerciseButtons[i].addActionListener(e -> showExercise(index));
            exercisePanel.add(exerciseButtons[i]);
        }

        buttonPanel.add(exercisePanel, BorderLayout.CENTER);

        panel.add(buttonPanel, BorderLayout.WEST);

        // Final setup for the frame
        frame.add(panel);
        frame.setSize(Toolkit.getDefaultToolkit().getScreenSize());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        // Action listeners for buttons
        uploadFileButton.addActionListener(e -> uploadJavaFile());
        submitButton.addActionListener(e -> JOptionPane.showMessageDialog(frame, "Exam submitted successfully!"));

        // Connect to the WebSocket server
        client.connect();

        // Start the countdown timer (e.g., for 1 hour = 3600 seconds)
        startCountdownTimer(10); // Adjust the countdown duration as needed
    }

    private String promptForClientName() {
        // Show a dialog for the user to input their name
        return JOptionPane.showInputDialog(frame, "Enter your name:", "Client Name", JOptionPane.PLAIN_MESSAGE);
    }

    private void sendNameToServer() {
        // Send the name to the server
        if (clientName != null && !clientName.isEmpty()) {
            client.send("CLIENT_NAME:" + clientName);
        } else {
            client.send("CLIENT_NAME:Unnamed");
        }
    }

    // Method to create a JTextArea
    private JTextArea createTextArea(String text) {
        JTextArea textArea = new JTextArea(text);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setEditable(false); // Set to false to make it read-only
        return textArea;
    }

    // Method to show the exercise based on button clicked
    private void showExercise(int index) {
        String[] questions = {
                "Question for Exercise 1: Tính tổng \n Nhập 2 số a và b từ bàn phím sau đó thực hiện tính tổng \n" +
                        "Input : 5 10 \n" +
                        "Output: 15 \n \n" +
                        "Định dạng tên file: Bai1.java",
                "Question for Exercise 2: Dãy số nhân đôi \n Cho một mảng có n phần tử , thực hiện nhân đôi các phần tử có ở trong mảng sau đó in ra các phần tử của mảng.\n" +
                        "Nhập n là số phần tử , n số tiếp theo là các phần tử của mảng \n" +
                        "Input : 5 \n" +
                        "      1 2 3 4 5 \n" +
                        "Output: 2 4 6 8 10 \n \n" +
                        "Định dạng tên file: Bai2.java",
                "Question for Exercise 3: Tìm số lớn nhất \n Cho một mảng có n phần tử , thực hiện tìm số lớn nhất ở trong mảng sau đó in ra số đó.\n" +
                        "Nhập n là số phần tử , n số tiếp theo là các phần tử của mảng \n" +
                        "Input : 5 \n" +
                        "      13 25 32 48 55 \n" +
                        "Output: 55 \n \n" +
                        "Định dạng tên file: Bai3.java",
                "Question for Exercise 4: Sắp xếp dãy số \n Cho một mảng có n phần tử , thực hiện sắp xếp phần tử có ở trong mảng sau đó in ra các phần tử của mảng.\n" +
                        "Nhập n là số phần tử , n số tiếp theo là các phần tử của mảng \n" +
                        "Input : 5 \n" +
                        "      4 2 1 5 3 \n" +
                        "Output: 1 2 3 4 5 \n \n" +
                        "Định dạng tên file: Bai4.java"
        };

        // Display the corresponding question in the text area
        textArea.setText(questions[index]);
    }

    private void uploadJavaFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Java Files", "java"));
        int returnValue = fileChooser.showOpenDialog(frame);

        if (returnValue == JFileChooser.APPROVE_OPTION) {
            selectedFile = fileChooser.getSelectedFile();

            // Send file to server
            try {
                sendFileToServer(selectedFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendFileToServer(File file) throws IOException {
        // Determine MIME type of the file
        String mimeType = Files.probeContentType(Paths.get(file.getAbsolutePath()));
        String fileName = file.getName();

        // Create HTTP connection
        URL url = new URL("http://localhost:8080/upload");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", mimeType != null ? mimeType : "application/octet-stream");
        connection.setRequestProperty("Content-Disposition", "form-data; name=\"file\"; filename=\"" + fileName + "\"");

        // Send file through output stream
        try (OutputStream os = connection.getOutputStream();
             FileInputStream fileInputStream = new FileInputStream(file)) {

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
        }

        // Read server response
        int responseCode = connection.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                client.send(response.toString());
                JOptionPane.showMessageDialog(frame, response.toString());
                compilationOutputArea.append(response.toString() + "\n");
            }
        } else {
            JOptionPane.showMessageDialog(frame, "Failed to upload file. HTTP code: " + responseCode);
        }
        connection.disconnect();
    }

    private void startCountdownTimer(int durationInSeconds) {
        remainingTime = durationInSeconds;
        timer = new Timer(1000, e -> {
            if (remainingTime > 0) {
                remainingTime--;
                updateTimerLabel();
            } else {
                timer.stop();
                JOptionPane.showMessageDialog(frame, "Time is up! The exam will now close.");
//                frame.dispose(); // Close the application
                System.exit(0);
            }
        });
        timer.start();
    }

    private void updateTimerLabel() {
        int hours = remainingTime / 3600;
        int minutes = (remainingTime % 3600) / 60;
        int seconds = remainingTime % 60;
        String timeString = String.format("Time remaining: %02d:%02d:%02d", hours, minutes, seconds);
        timerLabel.setText(timeString);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Client::new);
    }
}
