package com.qinsheng.io.bio;

import java.io.*;
import java.net.Socket;

public class BioClient {

    public static void main(String[] args) {
        try {
            Socket socket = new Socket("192.168.169.66", 9090);
            OutputStream outputStream = socket.getOutputStream();

            InputStream in = System.in;
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));

            while (true) {
                String line = reader.readLine();
                if(line != null) {
                    byte[] bytes = line.getBytes();
                    for (byte b : bytes) {
                        outputStream.write(b);
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
