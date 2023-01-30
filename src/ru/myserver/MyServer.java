package ru.myserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ru.myserver.request.Request;
import ru.myserver.request.RequestHandler;
import ru.myserver.response.Response;

public class MyServer {
	public static final String SERVER_HOME;
	public static final File SERVER_HOME_DIR;

	public static final String SERVLETS_HOME;
	public static final File SERVLETS_HOME_DIR;

	static {
		File file = new File(MyServer.class.getProtectionDomain().getCodeSource().getLocation().getFile());

		SERVER_HOME_DIR = file.getParentFile();
		SERVER_HOME = SERVER_HOME_DIR.getPath();

		SERVLETS_HOME_DIR = new File(SERVER_HOME, "WEB");
		SERVLETS_HOME = SERVLETS_HOME_DIR.getPath();
	}

	private void prepareToStart() {
		if (SERVLETS_HOME_DIR.exists()) {
			if (!SERVLETS_HOME_DIR.isDirectory())
			{
				SERVLETS_HOME_DIR.delete();
				SERVLETS_HOME_DIR.mkdir();
			}
		} else {
			System.out.println("Creating dir SERVLETS_HOME...");
			SERVLETS_HOME_DIR.mkdir();
			System.out.println("SERVLETS_HOME dir successfully created");
		}
	}

	public void startServerMultiThread(int port) throws IOException {
		ExecutorService service = Executors.newCachedThreadPool();

		ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.bind(new InetSocketAddress(port));
		Selector selector = Selector.open();
		serverSocketChannel.configureBlocking(false);
		serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

		RequestHandler requestHandler = new RequestHandler();

		while (true) {
			selector.selectNow();

			Iterator<SelectionKey> iter = selector.selectedKeys().iterator();

			while (iter.hasNext()) {
				SelectionKey key = iter.next();
				iter.remove();

				if (key.isValid()) {
					if (key.isAcceptable()) {
						service.execute(() -> {
							try {
								SocketChannel socketChannel = serverSocketChannel.accept();

								if (socketChannel == null)
									return;

								Socket socket = socketChannel.socket();

								System.out.println("Connection established!");

								try (BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
									 PrintWriter output = new PrintWriter(socket.getOutputStream())) {

									int contentLength = 0;

									List<String> lines = new ArrayList<String>();

									while (true) {
										String line = br.readLine();

										lines.add(line);

										if (line == null)
											break;

										if (line.startsWith("Content-Length: "))
											contentLength = Integer.parseInt(line.substring("Content-Length: ".length()));
										else if (line.isEmpty()) {
											if (contentLength > 0) {
												char[] buffer = new char[contentLength];

												br.read(buffer);

												lines.add(String.copyValueOf(buffer));
											}

											break;
										}
									}

									lines.forEach(System.out::println);

									Request req = new Request(lines.toArray(new String[0]));
									Response resp = new Response(output);

									requestHandler.processRequest(req, resp);
								}
							} catch (Exception e) {
								e.printStackTrace();
							}
						});
					}
				}
			}
		}
	}

	public void startServer(int port) throws IOException
	{
		try (ServerSocket serverSocket = new ServerSocket(port)) {
			prepareToStart();

			System.out.printf("Server started at port %d\n", port);

			List<String> lines = new ArrayList<>();
			RequestHandler requestHandler = new RequestHandler();

			while (true) {
				Socket socket = serverSocket.accept();

				try (BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					 PrintWriter output = new PrintWriter(socket.getOutputStream())) {
					while (!input.ready());

					while (input.ready()) {
						String line = input.readLine();

						lines.add(line);
					}

					socket.shutdownInput();

					Request req = new Request(lines.toArray(new String[0]));
					Response resp = new Response(output);

					requestHandler.processRequest(req, resp);

					output.flush();
					socket.shutdownOutput();
					socket.close();
					output.close();

					lines.clear();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
