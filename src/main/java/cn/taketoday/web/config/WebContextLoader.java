<<<<<<< HEAD
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
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import cn.taketoday.context.exception.BeanDefinitionStoreException;
import cn.taketoday.context.exception.NoSuchBeanDefinitionException;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.StringUtil;
import cn.taketoday.web.core.Constant;
import cn.taketoday.web.core.DefaultWebApplicationContext;
import cn.taketoday.web.core.WebApplicationContext;
import cn.taketoday.web.handler.ActionHandler;
import cn.taketoday.web.handler.DispatchHandler;
import cn.taketoday.web.multipart.AbstractMultipartResolver;
import cn.taketoday.web.multipart.DefaultMultipartResolver;
import cn.taketoday.web.resolver.DefaultExceptionResolver;
import cn.taketoday.web.resolver.DefaultParameterResolver;
import cn.taketoday.web.resolver.ExceptionResolver;
import cn.taketoday.web.servlet.ActionDispatcher;
import cn.taketoday.web.servlet.ViewDispatcher;
import cn.taketoday.web.view.AbstractViewResolver;
import cn.taketoday.web.view.JstlViewResolver;

/**
 * @author Today
 * @date 2018年6月23日 下午4:14:53
 */
@WebListener("WebContextLoader")
public final class WebContextLoader implements ServletContextListener, Constant {

	private static final long				serialVersionUID	= 4983190133174606852L;

	private Logger							log					= LoggerFactory.getLogger(WebContextLoader.class);

	private static WebApplicationContext	applicationContext;

	public WebContextLoader() {

	}

	public static WebApplicationContext getWebApplicationContext() {
		return applicationContext;
	}

	/**
	 * init framework
	 * 
	 * @throws Exception
	 */
	private void initFrameWork(String path) throws Exception {
		// find the config file
		getConfigFile(new File(path));
	}

	/**
	 * find config file
	 * 
	 * @param dir
	 * @throws Exception
	 */
	private void getConfigFile(File dir) throws Exception {

		File[] listFiles = dir.listFiles(path -> (path.isDirectory() || path.getName().endsWith(".xml")));

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

		builder.setEntityResolver((publicId, systemId) -> {
			if (systemId.contains(DTD_NAME)) {
				return new InputSource(
						new ByteArrayInputStream("<?xml version=\"1.0\" encoding=\"UTF-8\"?>".getBytes()));
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
			log.info("Found Configuration File.");
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

					log.info("Start Configure Views.");

					applicationContext.create(ViewConfig.class).init(ele); // view init
				} else if (ELEMENT_STATIC_RESOURCES.equals(nodeName)) {
					String staticMapping = ele.getAttribute(ATTR_MAPPING);
					addDefaultServletMapping(staticMapping);
				} else if (ELEMENT_MULTIPART.equals(nodeName)) {
					multipartResolver(ele);
				} else if (ELEMENT_VIEW_RESOLVER.equals(nodeName)) {
					viewResolver(ele);
				} else if (ELEMENT_EXCEPTION_RESOLVER.equals(nodeName)) {
					registerResolver(ele, ExceptionResolver.class, Constant.EXCEPTION_RESOLVER);
				} else if (ELEMENT_PARAMETER_RESOLVER.equals(nodeName)) {
					registerResolver(ele, DefaultParameterResolver.class, Constant.PARAMETER_RESOLVER);
				}
			}
		}
	}

	/**
	 * 
	 * @param element
	 * @param clazz
	 * @param name
	 * @throws ClassNotFoundException
	 * @throws BeanDefinitionStoreException
	 */
	private void registerResolver(Element element, Class<?> clazz, String name)
			throws ClassNotFoundException, BeanDefinitionStoreException {

		String class_ = element.getAttribute(Constant.ATTR_CLASS);

		Class<?> parameterResolver = null;
		if ("".equals(class_)) {
			parameterResolver = clazz;
		} else if (!clazz.getName().equals(class_)) {
			parameterResolver = Class.forName(class_);
		} else {
			parameterResolver = clazz;
		}
		// register resolver
		applicationContext.registerBean(name, parameterResolver);
		applicationContext.onRefresh();
		log.info("register [{}] onto [{}]", name, parameterResolver.getName());
	}

	/**
	 * 
	 * @param element
	 * @throws ClassNotFoundException
	 * @throws BeanDefinitionStoreException
	 * @throws NoSuchBeanDefinitionException
	 */
	private void viewResolver(Element element)
			throws ClassNotFoundException, BeanDefinitionStoreException, NoSuchBeanDefinitionException {

		registerResolver(element, JstlViewResolver.class, Constant.VIEW_RESOLVER);

		AbstractViewResolver viewResolver = applicationContext.getBean(Constant.VIEW_RESOLVER,
				AbstractViewResolver.class);

		viewResolver.setServletContext(applicationContext.getServletContext());

		NodeList childNodes = element.getChildNodes();
		for (int j = 0; j < childNodes.getLength(); j++) {
			Node item = childNodes.item(j);
			if (item instanceof Element) {
				Element config = (Element) item;
				String eleName = config.getNodeName();
				String nodeValue = config.getTextContent();
				log.debug("Found Element -> [{}] = [{}]", eleName, nodeValue);
				switch (eleName) //
				{
				case Constant.ELEMENT_VIEW_ENCODING:
					viewResolver.setEncoding(nodeValue);
					break;
				case Constant.ELEMENT_VIEW_PREFIX:
					viewResolver.setPrefix(nodeValue);
					break;
				case Constant.ELEMENT_VIEW_SUFFIX:
					viewResolver.setSuffix(nodeValue);
					break;
				case Constant.ELEMENT_VIEW_LOCALE:
					viewResolver.setLocale(new Locale(nodeValue));
					break;
				}
			}
		}
	}

	/**
	 * 
	 * @param element
	 * @throws ClassNotFoundException
	 * @throws BeanDefinitionStoreException
	 * @throws NoSuchBeanDefinitionException
	 */
	private void multipartResolver(Element element)
			throws ClassNotFoundException, BeanDefinitionStoreException, NoSuchBeanDefinitionException {

		registerResolver(element, DefaultMultipartResolver.class, Constant.MULTIPART_RESOLVER);

		AbstractMultipartResolver multipartResolver = applicationContext.getBean(Constant.MULTIPART_RESOLVER,
				AbstractMultipartResolver.class);

		NodeList childNodes = element.getChildNodes();
		for (int j = 0; j < childNodes.getLength(); j++) {
			Node item = childNodes.item(j);
			if (item instanceof Element) {
				Element config = (Element) item;
				String elementName = config.getNodeName();
				String nodeValue = config.getTextContent();
				log.debug("Found Element -> [{}] = [{}]", elementName, nodeValue);
				switch (elementName) //
				{
				case Constant.ELEMENT_UPLOAD_FILE_SIZE_THRESHOLD:
					multipartResolver.setFileSizeThreshold(Integer.parseInt(nodeValue));
					break;
				case Constant.ELEMENT_UPLOAD_LOCATION:
					multipartResolver.setLocation(nodeValue);
					break;
				case Constant.ELEMENT_UPLOAD_MAX_FILE_SIZE:
					multipartResolver.setMaxFileSize(Long.parseLong(nodeValue));
					break;
				case Constant.ELEMENT_UPLOAD_MAX_REQUEST_SIZE:
					multipartResolver.setMaxRequestSize(Long.parseLong(nodeValue));
					break;
				case Constant.ELEMENT_UPLOAD_ENCODING:
					multipartResolver.setEncoding(nodeValue);
					break;
				}
			}
		}
	}

	/**
	 * Register Servlet
	 * 
	 * @throws ServletException
	 * @throws NoSuchBeanDefinitionException
	 * @throws BeanDefinitionStoreException
	 */
	private void doRegisterServlet()
			throws ServletException, NoSuchBeanDefinitionException, BeanDefinitionStoreException {

		Set<String> urls = DispatchHandler.VIEW_REQUEST_MAPPING.keySet();

		ServletContext servletContext = applicationContext.getServletContext();
		if (urls.size() > 0) {
			log.info("Register Views Dispatcher.");
			Servlet viewServlet = new ViewDispatcher();
			servletContext.addServlet(Constant.VIEW_DISPATCHER, viewServlet);

			ServletRegistration registration = servletContext.getServletRegistration(Constant.VIEW_DISPATCHER);
			registration.addMapping(urls.toArray(new String[0]));
		}

		if (DispatchHandler.HANDLER_MAPPING_POOL.size() < 1) {
			return;
		}

		applicationContext.registerBean(Constant.ACTION_HANDLER, ActionHandler.class);
		applicationContext.onRefresh();

		log.info("Register Action Dispatcher.");
		Servlet actionServlet = null;
		try {
			actionServlet = new ActionDispatcher(applicationContext);
		} catch (NoSuchBeanDefinitionException ex) {
			log.error("Initialized ERROR -> [{}] caused by {}", ex.getMessage(), ex.getCause(), ex);
		}
		servletContext.addServlet(Constant.ACTION_DISPATCHER, actionServlet);

		ServletRegistration.Dynamic registration = (Dynamic) servletContext
				.getServletRegistration(Constant.ACTION_DISPATCHER);

		AbstractMultipartResolver multipartResolver = applicationContext.getBean(Constant.MULTIPART_RESOLVER,
				AbstractMultipartResolver.class);

		MultipartConfigElement multipartConfig = new MultipartConfigElement(multipartResolver.getLocation(),
				multipartResolver.getMaxFileSize(), multipartResolver.getMaxRequestSize(),
				multipartResolver.getFileSizeThreshold());

		registration.setMultipartConfig(multipartConfig);

		registration.addMapping(Constant.ACTION_DISPATCHER_MAPPING);
	}

	/**
	 * org.apache.catalina.servlets.DefaultServlet
	 * 
	 * @param staticMapping
	 */
	private void addDefaultServletMapping(String staticMapping) throws Exception {

		ServletRegistration servletRegistration = applicationContext.getServletContext()
				.getServletRegistration(Constant.DEFAULT);

		if (servletRegistration == null) { // create
			createDefaultServlet();
			servletRegistration = applicationContext.getServletContext().getServletRegistration(Constant.DEFAULT);
		}

		if (StringUtil.isEmpty(staticMapping)) {
			String[] defaultUrlPatterns = ((ActionConfig) applicationContext.create(ActionConfig.class))
					.getDefaultUrlPatterns();

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

		applicationContext.getServletContext().addServlet(Constant.DEFAULT, servlet);

		log.debug("no default servlet registration , create.");
		return servlet;
	}

	/**
	 * web application init
	 */
	@Override
	public void contextInitialized(ServletContextEvent sce) {

		long start = System.currentTimeMillis(); // start millis

		log.info("your application starts to be initialized at -> {}.",
				new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss").format(new Date()));

		applicationContext = new DefaultWebApplicationContext(ClassUtils.scanPackage(""));
		// servletContext
		ServletContext servletContext = sce.getServletContext();
		applicationContext.setServletContext(servletContext);
		String realPath = servletContext.getRealPath("/WEB-INF");

		try {
			// init start
			initFrameWork(realPath);
			log.info("Start Configure Actions.");
			applicationContext.create(ActionConfig.class).init(applicationContext);

			// check all resolver
			checkResolver();
			// register servlet
			doRegisterServlet();
			applicationContext.loadSuccess();
			// init end
		} catch (Exception ex) {
			log.error("Initialized ERROR -> [{}] caused by {}", ex.getMessage(), ex.getCause(), ex);
		}

		log.info("your application started successfully, It takes a total of {} ms.",
				System.currentTimeMillis() - start);
	}

	/**
	 * check resolver
	 * 
	 * @throws BeanDefinitionStoreException
	 */
	private void checkResolver() throws BeanDefinitionStoreException {

		if (!applicationContext.containsBean(Constant.EXCEPTION_RESOLVER)) {
			applicationContext.registerBean(Constant.EXCEPTION_RESOLVER, DefaultExceptionResolver.class);
			applicationContext.onRefresh();
			log.info("use default exception resolver -> [{}].", DefaultExceptionResolver.class);
		}
		if (!applicationContext.containsBean(Constant.MULTIPART_RESOLVER)) {
			// default multipart resolver
			applicationContext.registerBean(Constant.MULTIPART_RESOLVER, DefaultMultipartResolver.class);
			applicationContext.onRefresh();
			log.info("use default multipart resolver -> [{}].", DefaultMultipartResolver.class);
		}
		if (!applicationContext.containsBean(Constant.VIEW_RESOLVER)) {
			// use jstl view resolver
			applicationContext.registerBean(Constant.VIEW_RESOLVER, JstlViewResolver.class);
			applicationContext.onRefresh();
			log.info("use default view resolver -> [{}].", JstlViewResolver.class);
		}
		if (!applicationContext.containsBean(Constant.PARAMETER_RESOLVER)) {
			// use jstl view resolver
			applicationContext.registerBean(Constant.PARAMETER_RESOLVER, DefaultParameterResolver.class);
			applicationContext.onRefresh();
			log.info("use default parameter resolver -> [{}].", DefaultParameterResolver.class);
		}
	}

	/**
	 * destroy application
	 */
	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		log.info("your application destroyed");
	}

}
=======
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
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
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

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import cn.taketoday.context.exception.BeanDefinitionStoreException;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.exception.NoSuchBeanDefinitionException;
import cn.taketoday.context.utils.PropertiesUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.core.Constant;
import cn.taketoday.web.core.DefaultWebApplicationContext;
import cn.taketoday.web.core.WebApplicationContext;
import cn.taketoday.web.handler.ActionHandler;
import cn.taketoday.web.handler.DispatchHandler;
import cn.taketoday.web.multipart.AbstractMultipartResolver;
import cn.taketoday.web.multipart.DefaultMultipartResolver;
import cn.taketoday.web.resolver.DefaultExceptionResolver;
import cn.taketoday.web.resolver.DefaultParameterResolver;
import cn.taketoday.web.resolver.ExceptionResolver;
import cn.taketoday.web.servlet.ActionDispatcher;
import cn.taketoday.web.servlet.ViewDispatcher;
import cn.taketoday.web.view.AbstractViewResolver;
import cn.taketoday.web.view.JstlViewResolver;
import lombok.extern.slf4j.Slf4j;

/**
 * Load context.
 * 
 * @author Today <br>
 * 
 *         2018-06-23 16:14:53
 */
@Slf4j
@WebListener("WebContextLoader")
public final class WebContextLoader implements ServletContextListener, Constant {

	private static final long				serialVersionUID	= 4983190133174606852L;

	/** context **/
	private static WebApplicationContext	applicationContext;

	public WebContextLoader() {

	}

	public static WebApplicationContext getWebApplicationContext() {
		return applicationContext;
	}

	/**
	 * init framework.
	 * 
	 * @throws Exception
	 */
	private void initFrameWork(String path) throws Exception {
		// find the config file
		getConfigFile(new File(path));
	}

	/**
	 * find config file.
	 * 
	 * @param dir
	 *            directory
	 * @throws Exception
	 */
	private void getConfigFile(File dir) throws Exception {

		File[] listFiles = dir.listFiles(path -> (path.isDirectory() || path.getName().endsWith(".xml")));

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

		builder.setEntityResolver((publicId, systemId) -> {
			if (systemId.contains(DTD_NAME)) {
				return new InputSource(
						new ByteArrayInputStream("<?xml version=\"1.0\" encoding=\"UTF-8\"?>".getBytes()));
			}
			return null;
		});

		Document document = builder.parse(inputStream);
		registerXml(document);

		inputStream.close();
	}

	/**
	 * configure with xml file
	 * 
	 * @param doc
	 *            xml file
	 * @throws Exception
	 */
	private void registerXml(Document doc) throws Exception {
		Element root = doc.getDocumentElement();

		if (ROOT_ELEMENT.equals(root.getNodeName())) { // root element
			log.info("Found Configuration File.");
			configStart(root);
		}
	}

	/**
	 * start configure.
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

				switch (nodeName) //
				{
				case ELEMENT_COMMON:
					log.info("Start Configure Views.");
					new ViewConfig().init(ele); // view init
					break;
				case ELEMENT_STATIC_RESOURCES:
					String staticMapping = ele.getAttribute(ATTR_MAPPING);
					addDefaultServletMapping(staticMapping);
					break;
				case ELEMENT_MULTIPART:
					multipartResolver(ele);
					break;
				case ELEMENT_VIEW_RESOLVER:
					viewResolver(ele);
					break;
				case ELEMENT_EXCEPTION_RESOLVER:
					registerResolver(ele, ExceptionResolver.class, EXCEPTION_RESOLVER);
					break;
				case ELEMENT_PARAMETER_RESOLVER:
					registerResolver(ele, DefaultParameterResolver.class, PARAMETER_RESOLVER);
					break;
				default:
					log.error("This element -> [{}] is not supported.", nodeName);
					break;
				}
			}
		}
	}

	/**
	 * register resolver to application context.
	 * 
	 * @param element
	 *            xml element
	 * @param clazz
	 *            default class
	 * @param name
	 *            bean name
	 * @throws ClassNotFoundException
	 * @throws BeanDefinitionStoreException
	 */
	private void registerResolver(Element element, Class<?> clazz, String name)
			throws ClassNotFoundException, BeanDefinitionStoreException {
		String class_ = element.getAttribute(ATTR_CLASS);
		Class<?> parameterResolver = null;
		if (!clazz.getName().equals(class_)) { // Custom
			parameterResolver = Class.forName(class_);
		} else {
			parameterResolver = clazz; // default
		}
		// register resolver
		applicationContext.registerBean(name, parameterResolver);
		applicationContext.onRefresh();
		log.info("register [{}] onto [{}]", name, parameterResolver.getName());
	}

	/**
	 * configure view resolver.
	 * 
	 * @param element
	 *            xml element
	 * @throws ClassNotFoundException
	 * @throws BeanDefinitionStoreException
	 * @throws NoSuchBeanDefinitionException
	 * @throws ConfigurationException
	 * @throws DOMException
	 */
	private void viewResolver(Element element) throws ClassNotFoundException, BeanDefinitionStoreException,
			NoSuchBeanDefinitionException, DOMException, ConfigurationException {

		registerResolver(element, JstlViewResolver.class, VIEW_RESOLVER);

		AbstractViewResolver viewResolver = applicationContext.getBean(VIEW_RESOLVER, AbstractViewResolver.class);

		Properties properties = applicationContext.getBeanDefinitionRegistry().getProperties();

		viewResolver.setServletContext(applicationContext.getServletContext());

		NodeList childNodes = element.getChildNodes();
		for (int j = 0; j < childNodes.getLength(); j++) {
			Node item = childNodes.item(j);
			if (item instanceof Element) {
				Element config = (Element) item;
				String eleName = config.getNodeName();
				String nodeValue = PropertiesUtils.findInProperties(properties, config.getTextContent());
				log.debug("Found Element -> [{}] = [{}]", eleName, nodeValue);
				switch (eleName) //
				{
				case ELEMENT_VIEW_ENCODING:
					viewResolver.setEncoding(nodeValue);
					break;
				case ELEMENT_VIEW_PREFIX:
					viewResolver.setPrefix(nodeValue);
					break;
				case ELEMENT_VIEW_SUFFIX:
					viewResolver.setSuffix(nodeValue);
					break;
				case ELEMENT_VIEW_LOCALE:
					viewResolver.setLocale(new Locale(nodeValue));
					break;
				default:
					log.error("This element -> [{}] is not supported.", eleName);
					break;
				}
			}
		}
	}

	/**
	 * 
	 * @param element
	 * @throws ClassNotFoundException
	 * @throws BeanDefinitionStoreException
	 * @throws NoSuchBeanDefinitionException
	 * @throws ConfigurationException
	 * @throws DOMException
	 */
	private void multipartResolver(Element element) throws ClassNotFoundException, BeanDefinitionStoreException,
			NoSuchBeanDefinitionException, DOMException, ConfigurationException {

		registerResolver(element, DefaultMultipartResolver.class, MULTIPART_RESOLVER);

		AbstractMultipartResolver multipartResolver = applicationContext.getBean(MULTIPART_RESOLVER,
				AbstractMultipartResolver.class);

		Properties properties = applicationContext.getBeanDefinitionRegistry().getProperties();

		NodeList childNodes = element.getChildNodes();
		for (int j = 0; j < childNodes.getLength(); j++) {
			Node item = childNodes.item(j);
			if (item instanceof Element) {
				Element config = (Element) item;
				String elementName = config.getNodeName();
				String nodeValue = PropertiesUtils.findInProperties(properties, config.getTextContent());
				log.debug("Found Element -> [{}] = [{}]", elementName, nodeValue);
				switch (elementName) //
				{
				case ELEMENT_UPLOAD_FILE_SIZE_THRESHOLD:
					multipartResolver.setFileSizeThreshold(Integer.parseInt(nodeValue));
					break;
				case ELEMENT_UPLOAD_LOCATION:
					multipartResolver.setLocation(nodeValue);
					break;
				case ELEMENT_UPLOAD_MAX_FILE_SIZE:
					multipartResolver.setMaxFileSize(Long.parseLong(nodeValue));
					break;
				case ELEMENT_UPLOAD_MAX_REQUEST_SIZE:
					multipartResolver.setMaxRequestSize(Long.parseLong(nodeValue));
					break;
				case ELEMENT_UPLOAD_ENCODING:
					multipartResolver.setEncoding(nodeValue);
					break;
				default:
					log.error("This element -> [{}] is not supported.", elementName);
					break;
				}
			}
		}
	}

	/**
	 * Register Servlet
	 * 
	 * @throws ServletException
	 * @throws NoSuchBeanDefinitionException
	 * @throws BeanDefinitionStoreException
	 */
	private void doRegisterServlet()
			throws ServletException, NoSuchBeanDefinitionException, BeanDefinitionStoreException {

		Set<String> urls = DispatchHandler.VIEW_REQUEST_MAPPING.keySet();

		ServletContext servletContext = applicationContext.getServletContext();
		if (urls.size() > 0) {
			log.info("Register Views Dispatcher.");
			Servlet viewServlet = new ViewDispatcher();
			servletContext.addServlet(VIEW_DISPATCHER, viewServlet);

			ServletRegistration registration = servletContext.getServletRegistration(VIEW_DISPATCHER);
			registration.addMapping(urls.toArray(new String[0]));
		}

		if (DispatchHandler.HANDLER_MAPPING_POOL.size() < 1) {
			return;
		}

		applicationContext.registerBean(ACTION_HANDLER, ActionHandler.class);
		applicationContext.onRefresh();

		log.info("Register Action Dispatcher.");
		Servlet actionServlet = null;
		try {
			actionServlet = new ActionDispatcher(applicationContext);
		} catch (NoSuchBeanDefinitionException ex) {
			log.error("Initialized ERROR -> [{}] caused by {}", ex.getMessage(), ex.getCause(), ex);
		}

		servletContext.addServlet(ACTION_DISPATCHER, actionServlet);

		ServletRegistration.Dynamic registration = (Dynamic) servletContext.getServletRegistration(ACTION_DISPATCHER);

		AbstractMultipartResolver multipartResolver = applicationContext.getBean(MULTIPART_RESOLVER,
				AbstractMultipartResolver.class);

		MultipartConfigElement multipartConfig = new MultipartConfigElement(multipartResolver.getLocation(),
				multipartResolver.getMaxFileSize(), multipartResolver.getMaxRequestSize(),
				multipartResolver.getFileSizeThreshold());

		registration.setMultipartConfig(multipartConfig);

		registration.addMapping(ACTION_DISPATCHER_MAPPING);

	}

	/**
	 * org.apache.catalina.servlets.DefaultServlet
	 * 
	 * @param staticMapping
	 */
	private void addDefaultServletMapping(String staticMapping) throws Exception {

		ServletRegistration servletRegistration = applicationContext.getServletContext()
				.getServletRegistration(DEFAULT);

		if (servletRegistration == null) { // create
			createDefaultServlet();
			servletRegistration = applicationContext.getServletContext().getServletRegistration(DEFAULT);
		}

		if (StringUtils.isEmpty(staticMapping)) {
			ActionConfig actionConfig = applicationContext.getBean(ACTION_CONFIG, ActionConfig.class);

			String[] defaultUrlPatterns = actionConfig.getDefaultUrlPatterns();

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

		applicationContext.getServletContext().addServlet(DEFAULT, servlet);

		log.debug("no default servlet registration , create.");
		return servlet;
	}

	/**
	 * 
	 * 
	 * @throws Exception
	 */
	private void checkDefaultServlet() throws Exception {

		ServletRegistration servletRegistration = applicationContext.getServletContext()
				.getServletRegistration(DEFAULT);

		if (servletRegistration == null) { // create
			createDefaultServlet();
			servletRegistration = applicationContext.getServletContext().getServletRegistration(DEFAULT);
		}

		Collection<String> mappings = servletRegistration.getMappings();

		if (mappings.size() > 1) {
			return;// registered
		}
		ActionConfig actionConfig = applicationContext.getBean(ACTION_CONFIG, ActionConfig.class);
		String[] defaultUrlPatterns = actionConfig.getDefaultUrlPatterns();

		servletRegistration.addMapping(defaultUrlPatterns);

		log.debug("add default servlet default mapping -> {}.", Arrays.toString(defaultUrlPatterns));
	}

	/**
	 * check resolver
	 * 
	 * @throws BeanDefinitionStoreException
	 */
	private void checkResolver() throws BeanDefinitionStoreException {

		if (!applicationContext.containsBean(EXCEPTION_RESOLVER)) {
			applicationContext.registerBean(EXCEPTION_RESOLVER, DefaultExceptionResolver.class);
			applicationContext.onRefresh();
			log.info("use default exception resolver -> [{}].", DefaultExceptionResolver.class);
		}
		if (!applicationContext.containsBean(MULTIPART_RESOLVER)) {
			// default multipart resolver
			applicationContext.registerBean(MULTIPART_RESOLVER, DefaultMultipartResolver.class);
			applicationContext.onRefresh();
			log.info("use default multipart resolver -> [{}].", DefaultMultipartResolver.class);
		}
		if (!applicationContext.containsBean(VIEW_RESOLVER)) {
			// use jstl view resolver
			applicationContext.registerBean(VIEW_RESOLVER, JstlViewResolver.class);
			applicationContext.onRefresh();
			log.info("use default view resolver -> [{}].", JstlViewResolver.class);
		}
		if (!applicationContext.containsBean(PARAMETER_RESOLVER)) {
			// use default parameter resolver
			applicationContext.registerBean(PARAMETER_RESOLVER, DefaultParameterResolver.class);
			applicationContext.onRefresh();
			log.info("use default parameter resolver -> [{}].", DefaultParameterResolver.class);
		}
	}

	/**
	 * web application init
	 */
	@Override
	public void contextInitialized(ServletContextEvent sce) {

		long start = System.currentTimeMillis(); // start millis

		log.info("your application starts to be initialized at -> {}.",
				new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss").format(new Date()));

		applicationContext = new DefaultWebApplicationContext();

		// servletContext
		ServletContext servletContext = sce.getServletContext();
		applicationContext.setServletContext(servletContext);

		String realPath = servletContext.getRealPath(WEB_INF);

		try {

			// init start
			initFrameWork(realPath);
			log.info("Start Configure Actions.");
			ActionConfig actionConfig = applicationContext.getBean(ACTION_CONFIG, ActionConfig.class);
			actionConfig.init();

			// check all resolver
			checkResolver();
			checkDefaultServlet();

			// register servlet
			doRegisterServlet();
			applicationContext.removeBean(ACTION_CONFIG);
			applicationContext.loadSuccess();
			// init end
		} catch (Exception ex) {
			log.error("Initialized ERROR -> [{}] caused by {}", ex.getMessage(), ex.getCause(), ex);
		}

		log.info("Your Application Started Successfully, It takes a total of {} ms.",
				System.currentTimeMillis() - start);
	}

	/**
	 * destroy application.
	 */
	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		applicationContext.close();
		log.info("your application destroyed");
	}

}
>>>>>>> 2.2.x
