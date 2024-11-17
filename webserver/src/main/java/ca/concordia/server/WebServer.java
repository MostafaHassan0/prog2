package ca.concordia.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.*;
import java.util.concurrent.Semaphore;

public class WebServer {
    private static final int MAX_CONNECTIONS = 1000; // Limit to 1000 concurrent clients
    private final Semaphore connectionSemaphore = new Semaphore(MAX_CONNECTIONS);
    private final Map<Integer, Account> accounts = new HashMap<>(); // Store accounts in memory
    private final Semaphore transferSemaphore = new Semaphore(1); // Ensure transfer operations are thread-safe

    public void start() throws IOException {
        // Load accounts from a file before starting the server
        loadAccounts("webserver/src/main/resources/accounts.txt");

        ServerSocket serverSocket = new ServerSocket(5000);
        System.out.println("Server is listening on port 5001");

        while (true) {
            try {
                // Accept a connection from a client
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected");

                // Handle each client connection in a new thread
                new Thread(() -> handleClient(clientSocket)).start();
            } catch (IOException e) {
                System.err.println("Error accepting client connection: " + e.getMessage());
            }
        }
    }

    private void handleClient(Socket clientSocket) {
        try {
            // Acquire the semaphore before processing the client request
            connectionSemaphore.acquire();
            System.out.println("Semaphore acquired. Processing client: " + clientSocket);

            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            OutputStream out = clientSocket.getOutputStream();

            // Read the first line to determine the type of request (GET or POST)
            String request = in.readLine();
            if (request != null) {
                if (request.startsWith("GET")) {
                    handleGetRequest(out);
                } else if (request.startsWith("POST")) {
                    handlePostRequest(in, out);
                }
            }

            // Close resources after handling the request
            in.close();
            out.close();
            clientSocket.close();

        } catch (IOException | InterruptedException e) {
            System.err.println("Error handling client: " + e.getMessage());
        } finally {
            // Release the semaphore to allow other clients to connect
            connectionSemaphore.release();
            System.out.println("Semaphore released for client: " + clientSocket);
        }
    }

    private void handleGetRequest(OutputStream out) throws IOException {
        System.out.println("Handling GET request");
        String response = "HTTP/1.1 200 OK\r\n\r\n" +
                "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "<title>Concordia Transfers</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "\n" +
                "<h1>Welcome to Concordia Transfers</h1>\n" +
                "<p>Select the account and amount to transfer</p>\n" +
                "\n" +
                "<form action=\"/submit\" method=\"post\">\n" +
                "        <label for=\"account\">Account:</label>\n" +
                "        <input type=\"text\" id=\"account\" name=\"account\"><br><br>\n" +
                "\n" +
                "        <label for=\"value\">Value:</label>\n" +
                "        <input type=\"text\" id=\"value\" name=\"value\"><br><br>\n" +
                "\n" +
                "        <label for=\"toAccount\">To Account:</label>\n" +
                "        <input type=\"text\" id=\"toAccount\" name=\"toAccount\"><br><br>\n" +
                "\n" +
                "        <label for=\"toValue\">To Value:</label>\n" +
                "        <input type=\"text\" id=\"toValue\" name=\"toValue\"><br><br>\n" +
                "\n" +
                "        <input type=\"submit\" value=\"Submit\">\n" +
                "    </form>\n" +
                "</body>\n" +
                "</html>\n";
        out.write(response.getBytes());
        out.flush();
    }

    private void handlePostRequest(BufferedReader in, OutputStream out) throws IOException {
        StringBuilder requestBody = new StringBuilder();
        int contentLength = 0;
        String line;

        // Read headers to get content length
        while ((line = in.readLine()) != null && !line.isEmpty()) {
            if (line.startsWith("Content-Length")) {
                contentLength = Integer.parseInt(line.substring(line.indexOf(' ') + 1));
            }
        }

        for (int i = 0; i < contentLength; i++) {
            requestBody.append((char) in.read());
        }

        System.out.println("Request Body: " + requestBody.toString());

        String[] params = requestBody.toString().split("&");
        int account = -1, value = 0, toAccount = -1, toValue = 0;

        for (String param : params) {
            String[] parts = param.split("=");
            if (parts.length == 2) {
                String key = URLDecoder.decode(parts[0], "UTF-8");
                String val = URLDecoder.decode(parts[1], "UTF-8");

                switch (key) {
                    case "account":
                        account = Integer.parseInt(val);
                        break;
                    case "value":
                        value = Integer.parseInt(val);
                        break;
                    case "toAccount":
                        toAccount = Integer.parseInt(val);
                        break;

                    case "toValue":
                        toValue = Integer.parseInt(val);
                        break;
                }
            }
        }

        String response;
        String responseContent = "<html><body><h1>Thank you for using Concordia Transfers</h1>" +
                "<h2>Received Form Inputs:</h2>"+
                "<p>Account: " + account + "</p>" +
                "<p>Value: " + value + "</p>" +
                "<p>To Account: " + toAccount + "</p>" +
                "<p>To Value: " + toValue + "</p>" +
                "</body></html>";
        if (processTransfer(account, value, toAccount, toValue)) {
            response = "HTTP/1.1 200 OK\r\n\r\nTransfer successful!" + " Content-Length: " + responseContent.length() + "\r\n" +
                    "Content-Type: text/html\r\n\r\n" +
                    responseContent;
        } else {

            response = "HTTP/1.1 400 Bad Request\r\n\r\nTransfer failed!"+ " Content-Length: " + responseContent.length() + "\r\n" +
                    "Content-Type: text/html\r\n\r\n" +
                    responseContent;
        }

        out.write(response.getBytes());
        out.flush();
    }

    private boolean processTransfer(int fromAccount, int value, int toAccount, int toValue) {
        try {
            transferSemaphore.acquire();
            Account src = accounts.get(fromAccount);
            Account dest = accounts.get(toAccount);

            if (src == null || dest == null || src.getBalance() < value) {
                return false;
            }

            src.withdraw(value);
            dest.deposit(value);
            return true;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            transferSemaphore.release();
        }
    }

    private void loadAccounts(String filename) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                int id = Integer.parseInt(parts[0].trim());
                int balance = Integer.parseInt(parts[1].trim());
                accounts.put(id, new Account(balance, id));
            }
        }
    }






    public static void main(String[] args) {
        WebServer server = new WebServer();
        try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
