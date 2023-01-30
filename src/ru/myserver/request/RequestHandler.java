package ru.myserver.request;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import ru.myserver.MyServer;
import ru.myserver.response.HTTPCode;
import ru.myserver.response.Response;
import ru.myserver.servlet.MyServlet;
import ru.myserver.servlet.ServletHandler;

public class RequestHandler {
	private Map<String, String> urlMap;
	private ServletHandler servletHandler;

	public RequestHandler() {
		urlMap = new HashMap<>();

		File file = new File(MyServer.SERVLETS_HOME_DIR, "index.html");

		if (file.exists() && file.canRead())
			urlMap.put("", file.getAbsolutePath());

		findIndexHTMLs(MyServer.SERVLETS_HOME_DIR);

		servletHandler = new ServletHandler();
	}

	private void findIndexHTMLs(File dir) {
		for (File file: dir.listFiles(file -> file.isDirectory())) {
			File indexHTML = new File(file, "index.html");

			if (indexHTML.exists() && indexHTML.canRead())
				urlMap.put(MyServer.SERVLETS_HOME_DIR.toPath().relativize(file.toPath()).toString(), indexHTML.getAbsolutePath());

			findIndexHTMLs(file);
		}
	}

	public void processRequest(Request req, Response response) {
		Request request = new Request(req.getMethod(), req.getURL(), req.getParameters()) {
								public MyRequestDispatcher getRequestDispatcher(String url) {
									return RequestHandler.this.getRequestDispatcher(url);
								}
							};

		String url = req.getURL();

		if (url.startsWith("/"))
			url = url.substring(1);

		PrintWriter output = response.getOutputWriter();

		InputStream inputStream = null;

		if (urlMap.containsKey(url) && urlMap.get(url) != null) {
			output.println("HTTP/1.1 200 OK");
			output.println("Content-Type: text/html; charset=UTF-8");
			output.println();

			inputStream = getResourceStream(urlMap.get(url));

			if (inputStream != null) {
				try (Scanner scn = new Scanner(inputStream)) {
					while (scn.hasNextLine()) {
						String line = scn.nextLine();

						output.println(line);
					}

					output.flush();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} else {
			inputStream = getResourceStream(MyServer.SERVLETS_HOME + "/" + url);

			if (inputStream != null) {
				output.println("HTTP/1.1 200 OK");

				String contentType = "";

				switch (getFileFormat(url)) {
					case "css":
						contentType = "text/css";
						break;
					case "js":
						contentType = "text/javascript";
						break;
					default:
						contentType = "text/html";
						break;
				}

				output.printf("Content-Type: %s; charset=UTF-8\n", contentType);
				output.println();

				try (Scanner scn = new Scanner(inputStream)) {
					while (scn.hasNextLine()) {
						String line = scn.nextLine();
						output.println(line);
					}

					output.flush();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			else {
				MyServlet servlet = servletHandler.getServlet(url);

				if (servlet != null) {
					output.println("HTTP/1.1 200 OK");
					output.println("Content-Type: text/html; charset=UTF-8");

					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					PrintWriter printWriter = new PrintWriter(baos);

					Response response2 = new Response(printWriter);

					servlet.service(request, response2);
					printWriter.flush();

					output.println("Content-Length: " + baos.size());
					output.println("Connection: close");
					output.println();
					output.println(new String(baos.toByteArray()));

					output.flush();
				} else {
					output.printf("HTTP/1.1 %s\n", HTTPCode.NOT_FOUND_404.toString());
					output.println("Content-Type: text/html; charset=UTF-8");
					output.println();

					output.flush();
				}
			}
		}
	}

	public MyRequestDispatcher getRequestDispatcher(String url) {
		return this.new MyRequestDispatcherImpl(url);
	}

	private InputStream getResourceStream(String url) {
		File file = new File(url);

		InputStream inputStream = null;

		if (file.exists() && file.canRead()) {
			try {
				inputStream = new FileInputStream(file);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		else
			inputStream = getClass().getResourceAsStream("/" + url);

		return inputStream;
	}

	private static String getFileFormat(String path) {
		String[] parts = path.split("\\.");

		return parts[parts.length - 1];
	}

	private class MyRequestDispatcherImpl implements MyRequestDispatcher {
		private String url;

		private MyRequestDispatcherImpl(String url) {
			this.url = url;
		}

		public void redirectRequest(Request request, Response response) {
			MyServlet servlet = servletHandler.getServlet(url);

			if (servlet != null)
				servlet.service(request, response);
		}
	}
}
