package ru.servlet;

import ru.myserver.request.Request;
import ru.myserver.response.Response;
import ru.myserver.servlet.MyHttpServlet;

public class Test1 extends MyHttpServlet {

	protected void doGet(Request request, Response response) {
		response.getOutputWriter().println("<!DOCTYPE html>");
		response.getOutputWriter().println("<html>");
		response.getOutputWriter().println("<head>");
		response.getOutputWriter().println("<title>Test1 Servlet</title>");
		response.getOutputWriter().println("</head>");
		response.getOutputWriter().println("<body>");
		response.getOutputWriter().println("Hello World");
		response.getOutputWriter().println("</body>");
		response.getOutputWriter().println("</html>");
	}
}
