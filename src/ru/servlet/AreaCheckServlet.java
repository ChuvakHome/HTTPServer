package ru.servlet;

import ru.myserver.request.Request;
import ru.myserver.response.Response;
import ru.myserver.servlet.MyHttpServlet;

public class AreaCheckServlet extends MyHttpServlet {
	
	protected void doGet(Request request, Response response) {
		response.getOutputWriter().println("AreaCheckServlet");
	}
}
