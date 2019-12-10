package com.dongnao.bio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BIOServerV3 {

	private static Charset charset = Charset.forName("UTF-8");

	public static void main(String[] args) {
		int port = 1111;
		int threads = 100;
		ExecutorService tpool = Executors.newFixedThreadPool(threads);

		try (ServerSocket ss = new ServerSocket(port);) {
			while (true) {
				Socket s = ss.accept();
				// 丢到线程池中去跑
				tpool.execute(new SocketProcess(s));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		tpool.shutdown();
	}

	static class SocketProcess implements Runnable {
		Socket s;

		public SocketProcess(Socket s) {
			super();
			this.s = s;
		}

		@Override
		public void run() {
			try (BufferedReader reader = new BufferedReader(
					new InputStreamReader(s.getInputStream(), charset));) {
				// 接收数据、打印
				String mess = null;
				while ((mess = reader.readLine()) != null) {
					System.out.println(mess);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

	}
}
