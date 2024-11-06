import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileServer {
    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/upload", new FileUploadHandler());
        server.setExecutor(null); // Tự động sử dụng thread pool mặc định
        server.start();
        System.out.println("File Server started on port 8080");
    }

    static class FileUploadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Lấy input stream từ HTTP request body
            InputStream inputStream = exchange.getRequestBody();

            // Đảm bảo thư mục đã tồn tại trước khi lưu file
            File uploadDir = new File("C:\\Users\\ASUS\\Desktop\\exam");
            if (!uploadDir.exists()) {
                uploadDir.mkdirs(); // Tạo thư mục nếu chưa tồn tại
            }

            // Đọc tên tệp và loại tệp từ header Content-Disposition (nếu có)
            String contentDisposition = exchange.getRequestHeaders().getFirst("Content-Disposition");
            String fileName = "uploaded_file.java"; // Tên mặc định

            if (contentDisposition != null && contentDisposition.contains("filename=")) {
                // Trích xuất tên tệp từ header Content-Disposition
                String[] parts = contentDisposition.split(";");
                for (String part : parts) {
                    if (part.trim().startsWith("filename")) {
                        fileName = part.split("=")[1].trim().replace("\"", "");
                    }
                }
            }

            // Kiểm tra MIME type hoặc phần mở rộng của file
            String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
            if (contentType != null && contentType.contains("java")) {
                fileName = fileName + ".java";  // Đảm bảo tệp Java có đuôi đúng
            }

            // Đảm bảo tên tệp không có ký tự đặc biệt (tránh lỗi)
            Path filePath = Paths.get(uploadDir.getAbsolutePath(), fileName);

            // Lưu file vào thư mục trên Desktop
            try (FileOutputStream fileOutputStream = new FileOutputStream(filePath.toFile())) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    fileOutputStream.write(buffer, 0, bytesRead);
                }
            }

            // Đóng input stream
            inputStream.close();

            // Kiểm tra xem tệp input đã có sẵn trong cùng thư mục với tên tương ứng chưa
            String inputFileName = fileName.replace(".java", ".txt"); // Tệp input có cùng tên nhưng phần mở rộng .txt
            Path inputFilePath = Paths.get(uploadDir.getAbsolutePath(), inputFileName);

            if (!Files.exists(inputFilePath)) {
                String response = "Input file with name " + inputFileName + " does not exist!";
                exchange.sendResponseHeaders(400, response.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
                return;
            }

            // Kiểm tra xem tệp output đã có sẵn trong cùng thư mục với tên tương ứng chưa
            String outputFileName = fileName.replace(".java", "_output.txt"); // Tệp output có cùng tên nhưng phần mở rộng .txt
            Path outputFilePath = Paths.get(uploadDir.getAbsolutePath(), outputFileName);

            // Thực thi file Java và lấy đầu ra
            String output = executeJavaFile(filePath, inputFilePath);

            // So sánh đầu ra với nội dung của tệp output, loại bỏ khoảng trống
            String comparisonResult = compareOutputWithFile(output, outputFilePath);

            // Trả về phản hồi cho client
            String response = "";
            if(comparisonResult.equals("Output matches the expected output.")){
                response = fileName+ "      AC        ";
            }else if(comparisonResult.equals("Output does not match the expected output.")){
                response = fileName + "      WA        ";
            }else{
                response = fileName + "      ERROR        ";
            }

            exchange.sendResponseHeaders(200, response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }

        // Phương thức để biên dịch và chạy file Java
        private String executeJavaFile(Path javaFilePath, Path inputFilePath) {
            try {
                // Biên dịch tệp Java
                String javaFileName = javaFilePath.getFileName().toString().replace(".java", "");
                Process compileProcess = new ProcessBuilder("javac", javaFilePath.toString()).start();
                compileProcess.waitFor();

                // Kiểm tra nếu biên dịch thành công
                Path classFilePath = Paths.get(javaFilePath.getParent().toString(), javaFileName + ".class");
                if (!Files.exists(classFilePath)) {
                    return "Java compilation failed!";
                }

                // Chạy chương trình Java và cung cấp input từ file
                Process runProcess = new ProcessBuilder("java", javaFileName)
                        .directory(javaFilePath.getParent().toFile())
                        .redirectInput(inputFilePath.toFile()) // Cung cấp input từ file .txt
                        .start();
                runProcess.waitFor();

                // Lấy kết quả đầu ra
                BufferedReader reader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
                return output.toString();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                return "Error during execution: " + e.getMessage();
            }
        }

        // Phương thức để so sánh đầu ra với nội dung trong tệp output, loại bỏ tất cả khoảng trắng
        private String compareOutputWithFile(String output, Path outputFilePath) {
            try {
                // Đọc nội dung của tệp output dự kiến
                if (!Files.exists(outputFilePath)) {
                    return "Output file " + outputFilePath.toString() + " does not exist!";
                }

                String expectedOutput = new String(Files.readAllBytes(outputFilePath));

                // Loại bỏ tất cả khoảng trắng trong cả đầu ra và output file
                String normalizedOutput = output.replaceAll("\\s+", "");
                System.out.println(normalizedOutput);
                String normalizedExpectedOutput = expectedOutput.replaceAll("\\s+", "");
                System.out.println(normalizedExpectedOutput);

                // So sánh đầu ra của chương trình Java với tệp output đã loại bỏ khoảng trắng
                if (normalizedOutput.equals(normalizedExpectedOutput)) {
                    return "Output matches the expected output.";
                } else {
                    return "Output does not match the expected output.";
                }
            } catch (IOException e) {
                e.printStackTrace();
                return "Error reading output file: " + e.getMessage();
            }
        }
    }
}
