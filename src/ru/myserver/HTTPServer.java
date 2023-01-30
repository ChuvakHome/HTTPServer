package ru.myserver;

public class HTTPServer {
	public static void main(String[] args) throws Exception {
		int port = 443;

		if (args.length > 0)
			port = Integer.parseInt(args[0]);

		new MyServer().startServerMultiThread(port);
	}
}
