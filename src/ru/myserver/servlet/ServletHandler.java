package ru.myserver.servlet;

import static ru.myserver.MyServer.SERVLETS_HOME;
import static ru.myserver.MyServer.SERVLETS_HOME_DIR;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

public class ServletHandler {
	private Map<String, String> servletsMapper;
	private Map<String, MyServlet> servlets;
	private ServletClassLoader classLoader;

	public ServletHandler() {
		servletsMapper = new HashMap<>();

		findServels();

		servlets = new HashMap<>();

		try {
			classLoader = new ServletClassLoader(SERVLETS_HOME_DIR.toURI().toURL());
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

	private void findServels() {
		for (File file: SERVLETS_HOME_DIR.listFiles(file -> file.isDirectory())) {

			if (file.isDirectory()) {
				File config = new File(file, "servlet.config");
				File classes = new File(file, "classes");

				if (config.exists() && !config.isDirectory() && config.canRead() &&
					classes.exists() && classes.isDirectory() && classes.canRead())
					parseConfig(config);
			}
		}
	}

	private void parseConfig(File config) {
		try {
			ResourceBundle resourceBundle = new PropertyResourceBundle(new FileInputStream(config));
			String servlets = resourceBundle.getString("servlets");

			String[] servletArray = servlets.split(",");

			String parent = SERVLETS_HOME_DIR.toPath().relativize(config.getParentFile().toPath()).toString();

			for (String s: servletArray) {
				s = s.trim().substring(1);
				s = s.substring(0, s.length() - 1);

				String[] pairs = s.split("\\s\\s*");

				String url = null;
				String className = null;

				for (String pair: pairs) {
					String[] entry = pair.split("=");

					String key = entry[0];
					String value = entry[1];

					switch (key) {
						case "class":
							className = value;
							break;
						case "url":
							url = value;
							break;
					}
				}

				if (url != null && className != null)
					servletsMapper.put(parent + "/" + url, className);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public MyServlet getServlet(String url) {
		final String servletDir = url.split("/", 2)[0];

		if (servletsMapper.containsKey(url)) {
			String servletClassName = servletsMapper.get(url);

			if (servlets.containsKey(servletClassName)) {
				return servlets.get(servletClassName);
			} else {
				File classFile = new File(SERVLETS_HOME, servletDir + "/classes/" + servletClassName.replace('.', '/') + ".class");

				if (classFile.exists()) {
					if (classLoader.findResource(classFile.getPath()) == null) {
						Class<?> clazz = classLoader.defineClass(servletClassName, classFile);

						if (clazz != null && MyServlet.class.isAssignableFrom(clazz)) {
							try {
								MyServlet servlet = (MyServlet) clazz.newInstance();
								servlet.init(servletDir);

								servlets.put(servletClassName, servlet);

								return servlet;
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
		}

		return null;
	}

	private static class ServletClassLoader extends URLClassLoader {
		private ServletClassLoader(URL... urls) {
			super(urls);
		}

		private Class<?> defineClass(String className, File classFile) {
			try {
				byte[] b = new byte[(int) classFile.length()];

				InputStream is = new FileInputStream(classFile);
				is.read(b);
				is.close();

				Class<?> clazz = defineClass(className, b, 0, b.length);

				return clazz;
			} catch (Exception e) {
				e.printStackTrace();
			}

			return null;
		}

		public Class<?> loadClass(String className) {
			try {
				return loadClass(className, true);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}

			return null;
		}
	}
}
