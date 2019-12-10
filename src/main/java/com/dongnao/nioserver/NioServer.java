package com.dongnao.nioserver;

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

public class NioServer {

	private static Charset charset = Charset.forName("UTF-8");
	private static CharsetDecoder decoder = charset.newDecoder();

	public static void main(String[] args) throws IOException {

		int port = 1100;

		// 极少的线程
		int threads = 3;
		ExecutorService tpool = Executors.newFixedThreadPool(threads);

		// 1、得到一个selector
		Selector selector = Selector.open();

		try (ServerSocketChannel ssc = ServerSocketChannel.open()) {
			ssc.bind(new InetSocketAddress(port));
			// 2 注册到selector
			// 要非阻塞
			ssc.configureBlocking(false);
			// ssc向selector 注册，监听连接到来。
			ssc.register(selector, SelectionKey.OP_ACCEPT);

			// 连接计数
			int connectionCount = 0;

			// 3、循环选择就绪的通道
			while (true) {

				// 阻塞等待就绪的事件
				int readyChannels = selector.select();

				// 因为select()阻塞可以被中断
				if (readyChannels == 0) {
					continue;
				}

				// 取到就绪的key集合
				Set<SelectionKey> selectedKeys = selector.selectedKeys();

				Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

				while (keyIterator.hasNext()) {

					SelectionKey key = keyIterator.next();

					if (key.isAcceptable()) {
						// a connection was accepted by a ServerSocketChannel.
						ServerSocketChannel sssc = (ServerSocketChannel) key
								.channel();

						// 接受连接
						SocketChannel cc = sssc.accept();

						// 请selector 帮忙检测数据到了
						// 设置非阻塞
						cc.configureBlocking(false);
						// 向selector 注册
						cc.register(selector, SelectionKey.OP_READ,
								++connectionCount);

					} else if (key.isConnectable()) {
						// a connection was established with a remote server.

					} else if (key.isReadable()) {
						// a channel is ready for reading
						// 4、读取数据进行处理
						// 交各线程池去处理
						tpool.execute(new SocketReadProcess(key));

						// 取消一下注册，防止线程池处理不及时，没有注销掉
						key.cancel();

					} else if (key.isWritable()) {
						// a channel is ready for writing
					}

					keyIterator.remove(); // 处理了，一定要从selectedKey集中移除
				}

			}

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
				System.out.println(
						"连接" + key.attachment() + "发来：" + readFromChannel());

				// 如果连接不需要了，就关闭
				key.channel().close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		private String readFromChannel() throws IOException {

			SocketChannel sc = (SocketChannel) key.channel();

			int bfsize = 1024;
			ByteBuffer rbf = ByteBuffer.allocateDirect(bfsize);

			// 定义一个更大的buffer
			ByteBuffer bigBf = null;
			// 读的次数计数
			int count = 0;
			while ((sc.read(rbf)) != -1) {
				count++;

				ByteBuffer temp = ByteBuffer
						.allocateDirect(bfsize * (count + 1));

				if (bigBf != null) {
					// 将buffer有写转为读模式
					bigBf.flip();
					temp.put(bigBf);
				}

				bigBf = temp;

				// 将这次读到的数据放入大buffer
				rbf.flip();
				bigBf.put(rbf);
				// 为下次读，清理。
				rbf.clear();
				// 读出的是字节，要转为字符串
			}

			if (bigBf != null) {
				bigBf.flip();
				return decoder.decode(bigBf).toString();
			}

			return null;
		}

	}
}
