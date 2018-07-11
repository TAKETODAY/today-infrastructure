/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Today & 2017 - 2018 All Rights Reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web.config;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Set;

import javax.servlet.MultipartConfigElement;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletRegistration.Dynamic;
import javax.servlet.annotation.WebListener;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import cn.taketoday.context.core.Constant;
import cn.taketoday.context.utils.StringUtil;
import cn.taketoday.web.handler.DispatchHandler;
import cn.taketoday.web.servlet.ActionDispatcher;
import cn.taketoday.web.servlet.ViewDispatcher;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Today
 * @date 2018年6月23日 下午4:14:53
 */
@Slf4j
@WebListener("WebContextLoader")
public final class WebContextLoader implements ServletContextListener, Constant {

	private static final long	serialVersionUID		= 4983190133174606852L;

	/** configuration factory */
	ConfigurationFactory		configurationFactory	= ConfigurationFactory.createFactory();

	public WebContextLoader() {
		
	}

	/**
	 * init framework
	 * 
	 * @throws Exception
	 */
	private void initFrameWork() throws Exception {
		String realPath = configurationFactory.getServletContext().getRealPath("/WEB-INF"); // get real path
		// find the config file
		getConfigFile(new File(realPath));

	}

	private void getConfigFile(File dir) throws Exception {

		File[] listFiles = dir.listFiles(path -> (
			path.isDirectory() || path.getName().endsWith(".xml") || path.getName().endsWith(".properties")
		));
		
		for (File file : listFiles) {
			if (file.isDirectory()) { // recursive
				getConfigFile(file);
				continue;
			}
			InputStream inputStream = new FileInputStream(file);
			loadXml(inputStream);
		}
		
	}

	/**
	 * load xml file.
	 * 
	 * @param inputStream
	 *            xml input stream
	 * @throws Exception
	 */
	private void loadXml(InputStream inputStream) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setIgnoringComments(true);
		DocumentBuilder builder = factory.newDocumentBuilder();
		
		builder.setEntityResolver((publicId, systemId)-> {
			if (systemId.contains(DTD_NAME)) {
				return new InputSource(new ByteArrayInputStream("<?xml version=\"1.0\" encoding=\"UTF-8\"?>".getBytes()));
			}
			return null;
		});
		
		Document document = builder.parse(inputStream);
		registerXml(document);

		inputStream.close();
	}

	/**
	 * 
	 * @param doc
	 * @throws Exception
	 */
	private void registerXml(Document doc) throws Exception {
		Element root = doc.getDocumentElement();

		if (ROOT_ELEMENT.equals(root.getNodeName())) { // root element
			log.info("Found configuration file.");
			configStart(root);
		}
		
	}

	/**
	 * start configure
	 * 
	 * @param root
	 *            rootElement
	 * @throws Exception
	 */
	private void configStart(Element root) throws Exception {

		NodeList nl = root.getChildNodes();

		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			if (node instanceof Element) {
				Element ele = (Element) node;
				String nodeName = ele.getNodeName();
				if (ELEMENT_COMMON.equals(nodeName)) {
					log.info("start configure Views.");
					configurationFactory.createViewConfig().init(ele); // view init
				} else if (ELEMENT_STATIC_RESOURCES.equals(nodeName)) {
					String staticMapping = ele.getAttribute(ATTR_MAPPING);

					addDefaultServletMapping(staticMapping);
				} else if (ELEMENT_COMPONENT_SCAN.equals(nodeName)) {
					String basePackage = ele.getAttribute(ATTR_BASE_PACKAGE);
					String suffix = ele.getAttribute(ATTR_SUFFIX);
					String prefix = ele.getAttribute(ATTR_PREFIX);

					configurationFactory.setPrefix(prefix);
					configurationFactory.setSuffix(suffix);

					log.info("start configure Actions.");
					configurationFactory.createActionConfig().init(basePackage);
				}
			}
		}
		// register servlet
		doRegisterServlet();
	}
	

	/**
	 * Register Servlet
	 * 
	 * @throws ServletException
	 */
	private void doRegisterServlet() throws ServletException {

		Set<String> urls = DispatchHandler.VIEW_REQUEST_MAPPING.keySet();

		ServletContext servletContext = configurationFactory.getServletContext();

		if (urls.size() > 0) {
			log.info("register view dispatcher.");
			Servlet viewServlet = new ViewDispatcher();
			servletContext.addServlet("ViewDispatcher", viewServlet);

			ServletRegistration registration = servletContext.getServletRegistration("ViewDispatcher");
			registration.addMapping(urls.toArray(new String[0]));

		}

		if (DispatchHandler.HANDLER_MAPPING_POOL.size() < 1) {
			return;
		}

		log.info("register action dispatcher.");
		Servlet actionServlet = new ActionDispatcher();
		servletContext.addServlet("ActionDispatcher", actionServlet);

		ServletRegistration.Dynamic registration = (Dynamic) servletContext.getServletRegistration("ActionDispatcher");

		MultipartConfigElement multipartConfig = new MultipartConfigElement(configurationFactory.getLocation(),
				configurationFactory.getMaxFileSize(), configurationFactory.getMaxRequestSize(),
				configurationFactory.getFileSizeThreshold());

		registration.setMultipartConfig(multipartConfig);

		registration.addMapping("/");

	}

	/**
	 * org.apache.catalina.servlets.DefaultServlet
	 * 
	 * @param staticMapping
	 */
	private void addDefaultServletMapping(String staticMapping) throws Exception {

		ServletRegistration servletRegistration = configurationFactory.getServletContext()
				.getServletRegistration("default");

		if (servletRegistration == null) { // create
			createDefaultServlet();
			servletRegistration = configurationFactory.getServletContext().getServletRegistration("default");
		}

		if (StringUtil.isEmpty(staticMapping)) {
			String[] defaultUrlPatterns = configurationFactory.getDefaultUrlPatterns();
			servletRegistration.addMapping(defaultUrlPatterns);
			log.debug("add default servlet default mapping -> {}.", Arrays.toString(defaultUrlPatterns));
			return;
		}

		servletRegistration.addMapping(staticMapping);

		log.debug("add default servlet mapping -> {}.", servletRegistration.getMappings());

	}

	/**
	 * create default servlet to handle static resource
	 * 
	 * @return
	 * @throws Exception
	 */
	private Servlet createDefaultServlet() throws Exception {

		// create
		Class<?> default_ = Class.forName("org.apache.catalina.servlets.DefaultServlet");
		Servlet servlet = (Servlet) default_.getConstructor().newInstance();

		configurationFactory.getServletContext().addServlet("default", servlet);

		log.debug("no default servlet registration , create.");

		return servlet;
	}

	/**
	 * web application init
	 */
	@Override
	public void contextInitialized(ServletContextEvent sce) {

		long start = System.currentTimeMillis(); // start millis
		log.info("your application start initializing.");

		configurationFactory.setServletContext(sce.getServletContext());

		sce.getServletContext().setAttribute("CDN", "//weixiub.oss-cn-beijing.aliyuncs.com");

		try {
			
			//init start 
			initFrameWork();
			
			if (DispatchHandler.HANDLER_MAPPING_POOL.size() < 1) {
				log.info("there is no config file use default.");
				configurationFactory.createActionConfig().init(""); // scan all the package
			}
			// init end
		} catch (Exception ex) {
			log.error("Initialized ERROR -> [{}] caused by {}", ex.getMessage(), ex.getCause(), ex);
		}

		log.info("your application started successfully, It takes a total of {} ms.",
				System.currentTimeMillis() - start);
	}

	/**
	 * destroy application
	 */
	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		log.info("your application destroyed");
	}

}
