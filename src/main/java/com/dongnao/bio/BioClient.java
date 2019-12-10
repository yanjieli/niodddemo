package com.dongnao.bio;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Scanner;

public class BioClient implements  Runnable{
    private  String address;
    private  int  port;


    public BioClient(String address, int port) {
        super();
        this.address=address;
        this.port = port;
    }

    public static void main(String[] args) {
        BioClient client = new BioClient("localhost",1100);
        client.run();
    }

    @Override
    public void run() {
        try(Socket socket = new Socket(address,port);
            OutputStream outputStream = socket.getOutputStream()) {


            Scanner scanner = new Scanner(System.in);
            System.out.println("请输入:");
            String msg = scanner.nextLine();
            outputStream.write(msg.getBytes("UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
