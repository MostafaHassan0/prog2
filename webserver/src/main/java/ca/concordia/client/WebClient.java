package ca.concordia.client;

import java.io.*;
import java.net.Socket;

public class WebClient implements Runnable {

    private final int fromAccount;
    private final int toAccount;

    public WebClient(int fromAccount, int toAccount) {
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
    }

    public void run() {
        Socket socket = null;
        PrintWriter writer = null;
        BufferedReader reader = null;

        try {
            // Establish a connection to the server
            socket = new Socket("localhost", 5001);
            System.out.println("Connected to server");

            // Create an output stream to send the request
            OutputStream out = socket.getOutputStream();
            writer = new PrintWriter(new OutputStreamWriter(out));

            // Prepare the POST request with form data
            int transactionAmount = (int) (Math.random() * 100 + 1); // Random amount between 1 and 100
            String postData = "account=" + fromAccount + "&value=" + transactionAmount + "&toAccount=" + toAccount;

            System.out.println("Sending request: From Account=" + fromAccount + ", To Account=" + toAccount + ", Value=" + transactionAmount);

            // Introduce a random delay to simulate network delay and spread requests
            int waitTime = (int) (Math.random() * 500 + 500); // random delay between 500ms and 1000ms
            Thread.sleep(waitTime);

            // Send the POST request
            writer.println("POST /submit HTTP/1.1");
            writer.println("Host: localhost:5001");
            writer.println("Content-Type: application/x-www-form-urlencoded");
            writer.println("Content-Length: " + postData.length());
            writer.println();
            writer.println(postData);
            writer.flush();

            // Create an input stream to read the response
            InputStream in = socket.getInputStream();
            reader = new BufferedReader(new InputStreamReader(in));

            // Read and print the response
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Error in client communication: " + e.getMessage());
        } finally {
            // Close the streams and socket, with null checks
            try {
                if (reader != null) reader.close();
                if (writer != null) writer.close();
                if (socket != null) socket.close();
            } catch (IOException e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        int clientCount = 100;
        try {
            if (args.length > 0) {
                clientCount = Integer.parseInt(args[0]); // Accept client count from command-line arguments
            } else {
                System.out.print("Enter the number of threads to create: ");
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                clientCount = Integer.parseInt(reader.readLine());
            }
        } catch (Exception e) {
            System.err.println("Invalid input. Using default value of threads(100) will be used.");
        }

        System.out.println("Starting " + clientCount + " client threads...");
        for (int i = 0; i < clientCount; i++) {
            System.out.println("Creating client pair " + i);
            Thread thread1 = new Thread(new WebClient(123, 345));
            Thread thread2 = new Thread(new WebClient(345, 123));
            thread1.start();
            thread2.start();
            try {
                Thread.sleep(2); // delay between each client pair
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
