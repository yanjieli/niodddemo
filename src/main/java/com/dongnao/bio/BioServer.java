package com.dongnao.bio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;

public class BioServer {

    private  static Charset charset = Charset.forName("UTF-8");
    public static void main(String[] args) {
        int port = 1111;


        try ( ServerSocket  socketServer = new ServerSocket(port)){

            while (true){
                //接收连接，如果没有连接建立，这里会阻塞
                Socket socket = socketServer.accept();

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(),charset)
                );
                String msg = null;
                //连接进来后，会在这里等待客户端发送消息。
                while ((msg =reader.readLine())!=null){
                    System.out.println(msg);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }



}
