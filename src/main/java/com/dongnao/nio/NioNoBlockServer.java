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

public class NioNoBlockServer {
	static Charset charset = Charset.forName("UTF-8");
	static CharsetDecoder decoder = charset.newDecoder();

	public static void main(String[] args) {
		int port = 1100;
		int threads = 100;
		ExecutorService tpool = Executors.newFixedThreadPool(threads);

		try (ServerSocketChannel ssc = ServerSocketChannel.open();) {
			// 配置非阻塞
			ssc.configureBlocking(false);
			// 绑定监听端口
			ssc.bind(new InetSocketAddress(port));
			while (true) {
				try {
					// 注意：接收连接不会阻塞了，没有连接就会返回null
					SocketChannel sc = ssc.accept();

					if (sc == null) {
						// 还没有连接，可以干点别的事情，用sleep代替了。
						System.out.println(" 还没有连接，可以干点别的事情.....");
						Thread.sleep(1000L);
						continue;
					}

					// 我们把连接的sockeChannel 也配置成非阻塞的
					sc.configureBlocking(false); // 这样读数据时，就不会阻塞，没数据就会返回0
					// 丢给线程池处理
					tpool.execute(new SocketProcess(sc));

				} catch (IOException | InterruptedException e) {
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
				System.out.println(this.readFromChannel());
				sc.close();

			} catch (IOException e) {
				e.printStackTrace();
			}

		}

		private String readFromChannel() throws IOException {

			// int leng = sc.read(rbf); 判断读数据是否读完需考虑4种情况：
			// 1、数据的长度 < buffer的长度 一次读，判断 leng < bfsize 不再读了
			// 2、数据的长度 = buffer的长度 读两次，第二次 leng == -1 leng < bfsize 不再读了
			// 3、数据的长度 > buffer的长度 多次读，判断 leng < bfsize 不再读了
			// 4、没数据可读：阻塞模式下会阻塞直到读到数据，非阻塞模式下会立马返回0。
			// 总结：多次读 while(leng == bfsize)
			// 注意：如果读出的字节需要转成其他类型的，如字符串，则需要一个更大的buffer来存放。

			// 创建一个缓冲区
			int bfsize = 1024;
			ByteBuffer rbf = ByteBuffer.allocateDirect(bfsize);
			// 更大的buffer
			ByteBuffer longBf = null;
			int leng = 0;
			// 读的次数计数
			int count = 0;

			while (true) {
				leng = sc.read(rbf);
				if (leng == 0) {
					// 干点别的吧，数据还没送来
					System.out.println("干点别的吧，数据还没送来.....");
					try {
						Thread.sleep(1000L);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					continue;
				}

				while (leng != -1) {
					count++;
					ByteBuffer temp = ByteBuffer
							.allocateDirect(bfsize * (count + 1));
					if (longBf != null) {
						longBf.flip();
						temp.put(longBf);
					}

					longBf = temp;

					rbf.flip();
					longBf.put(rbf);
					rbf.clear();

					// 继续读
					leng = sc.read(rbf);
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
}
