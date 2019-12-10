package com.dongnao.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NioNoBlockServerV2 {
	static Charset charset = Charset.forName("UTF-8");
	static CharsetDecoder decoder = charset.newDecoder();

	public static void main(String[] args) throws IOException {
		int port = 1100;
		// 线程数变了
		int threads = 3;
		ExecutorService tpool = Executors.newFixedThreadPool(threads);

		// 创建selector
		Selector selector = Selector.open();

		try (ServerSocketChannel ssc = ServerSocketChannel.open();) {
			// 配置非阻塞
			ssc.configureBlocking(false);
			// 绑定监听端口
			ssc.bind(new InetSocketAddress(port));

			// ServcerSocketChannel向selector注册 连接事件
			ssc.register(selector, SelectionKey.OP_ACCEPT);
			// 连接计数
			int connectionCount = 0;

			while (true) {

				int readyChannels = selector.select();

				if (readyChannels == 0)
					continue;

				Set<SelectionKey> selectedKeys = selector.selectedKeys();

				Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

				while (keyIterator.hasNext()) {

					SelectionKey key = keyIterator.next();

					if (key.isAcceptable()) {
						// 检测到连接来了,从SelectionKey中取出ServerSocketChannel
						ServerSocketChannel sssc = (ServerSocketChannel) key
								.channel();
						// ServerSocketChannel接受连接
						SocketChannel sc = sssc.accept();
						// 配置非阻塞模式
						sc.configureBlocking(false);
						// 向selector注册读事件，并附上连接计数，标识是第几个连接
						sc.register(selector, SelectionKey.OP_READ,
								++connectionCount);

					} else if (key.isConnectable()) {
						// 这是客户端采用的

					} else if (key.isReadable()) {
						// 读就绪了，就来处理读吧
						// 把它交给线程池去处理，我是专注我的检测工作
						tpool.execute(new SocketReadProcess(key));
						// 交给线程池异步处理，防止处理不及时，重复选择，把 注册取消。
						key.cancel();

					} else if (key.isWritable()) {
						// a channel is ready for writing
					}

					keyIterator.remove(); // 处理了，一定要从selectedKey集中移除
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	static class SocketReadProcess implements Runnable {
		SelectionKey key;

		public SocketReadProcess(SelectionKey key) {
			super();
			this.key = key;
		}

		@Override
		public void run() {
			try {
				System.out.println("连接" + key.attachment() + "发来："
						+ this.readFromChannel());

				// 连接如果不要保持，就关闭
				key.channel().close();

			} catch (IOException e) {
				e.printStackTrace();
			}

		}

		private String readFromChannel() throws IOException {

			SocketChannel sc = (SocketChannel) this.key.channel();

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
