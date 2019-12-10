package com.dongnao.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Scanner;

public class NioClient {

	static Charset charset = Charset.forName("UTF-8");

	public static void main(String[] args) {
		try (SocketChannel sc = SocketChannel.open();) {
			// 连接 会阻塞
			boolean connected = sc
					.connect(new InetSocketAddress("localhost", 1100));

			System.out.println("connected=" + connected);

			// 写
			Scanner scanner = new Scanner(System.in);
			System.out.println("请输入：");
			String mess = scanner.nextLine();
			ByteBuffer bf = ByteBuffer.wrap(mess.getBytes(charset));

			while (bf.hasRemaining()) {
				int writedCount = sc.write(bf);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
