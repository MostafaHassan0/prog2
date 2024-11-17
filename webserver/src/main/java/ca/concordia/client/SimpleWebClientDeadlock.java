package ca.concordia.client;

import java.io.*;
import java.net.Socket;

public class SimpleWebClientDeadlock implements Runnable {

    private final int fromAccount;
    private final int toAccount;

    public SimpleWebClientDeadlock(int fromAccount, int toAccount) {
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
            String postData = "account=" + fromAccount + "&value=1&toAccount=" + toAccount + "&toValue=1";

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
            e.printStackTrace();
        } finally {
            // Close the streams and socket, with null checks
            try {
                if (reader != null) reader.close();
                if (writer != null) writer.close();
                if (socket != null) socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        for (int i = 0; i <= 50; i++) {
            System.out.println("Creating client pair " + i);
            Thread thread1 = new Thread(new SimpleWebClientDeadlock(123, 345));
            Thread thread2 = new Thread(new SimpleWebClientDeadlock(345, 123));
            thread1.start();
            thread2.start();
            try {
                Thread.sleep(2); // delay between each client pair, the higher the delay the better
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}