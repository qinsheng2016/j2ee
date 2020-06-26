package com.qinsheng.io.bio.v2;

import java.io.IOException;
import java.net.Socket;

/**
 * @Author: qinsheng
 * @Date: 2020/6/27 00:46
 */
public class Client {

    public static void main(String[] args) throws IOException {
        Socket s = new Socket("127.0.0.1", 9999);
        s.getOutputStream().write("HelloServer".getBytes());
        s.getOutputStream().flush();

        System.out.println("write over, waiting for msg back....");
        byte[] bytes = new byte[1024];
        int len = s.getInputStream().read(bytes);
        System.out.println(new String(bytes, 0, len));
        s.close();
    }

}
