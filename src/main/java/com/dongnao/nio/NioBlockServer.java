package com.dongnao.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NioBlockServer {
	static Charset charset = Charset.forName("UTF-8");
	static CharsetDecoder decoder = charset.newDecoder();

	public static void main(String[] args) {
		int port = 1100;
		int threads = 100;
		ExecutorService tpool = Executors.newFixedThreadPool(threads);

		try (ServerSocketChannel ssc = ServerSocketChannel.open();) {
			// 绑定监听端口
			ssc.bind(new InetSocketAddress(port));

			while (true) {
				try {
					// 接收连接会阻塞
					SocketChannel sc = ssc.accept();
					// 丢给线程池处理
					tpool.execute(new SocketProcess(sc));

				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	static class SocketProcess implements Runnable {
		SocketChannel sc;

		public SocketProcess(SocketChannel sc) {
			super();
			this.sc = sc;
		}

		@Override
		public void run() {
			try {

				/*// 创建一个缓冲区
				int bfsize = 1024;
				ByteBuffer rbf = ByteBuffer.allocateDirect(bfsize);

				// 读 会阻塞
				int leng = sc.read(rbf); // ? 如何知道数据读完了
				System.out.println(leng);

				// 转为读模式
				rbf.flip();
				// 转成CharBuffer，再转为字符串。
				System.out.println(decoder.decode(rbf).toString());*/

				System.out.println(this.readFromChannel());

				sc.close();

			} catch (IOException e) {
				e.printStackTrace();
			}

		}

		private String readFromChannel() throws IOException {

			// int leng = sc.read(rbf); 判断读数据是否读完需考虑4种情况：
			
			// 总结：多次读 while(leng != -1)
			// 注意：如果读出的字节需要转成其他类型的，如字符串，则需要一个更大的buffer来存放。

			// 创建一个缓冲区
			int bfsize = 1024;
			ByteBuffer rbf = ByteBuffer.allocateDirect(bfsize);
			// 更大的buffer
			ByteBuffer longBf = null;
			int leng = 0;
			// 读的次数计数
			int count = 0;
			while ((sc.read(rbf)) != -1) {
				count++;
				ByteBuffer temp = ByteBuffer
						.allocateDirect(bfsize * (count + 1));
				if (longBf != null) {
					longBf.flip();
					temp.put(longBf);
				}

				longBf = temp;
				// 将buffer转为读模式
				rbf.flip();
				longBf.put(rbf);
				rbf.clear();
			}

			if (longBf != null) {
				// 转为读模式
				longBf.flip();
				// 转成CharBuffer，再转为字符串。
				return decoder.decode(longBf).toString();
			}

			return null;
		}
	}
}
