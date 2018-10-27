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

import cn.taketoday.context.exception.BeanDefinitionStoreException;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.exception.NoSuchBeanDefinitionException;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.PropertiesUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.Constant;
import cn.taketoday.web.DefaultWebApplicationContext;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.event.ApplicationStartedEvent;
import cn.taketoday.web.multipart.AbstractMultipartResolver;
import cn.taketoday.web.multipart.DefaultMultipartResolver;
import cn.taketoday.web.resolver.DefaultExceptionResolver;
import cn.taketoday.web.resolver.DefaultParameterResolver;
import cn.taketoday.web.resolver.ExceptionResolver;
import cn.taketoday.web.servlet.DispatcherServlet;
import cn.taketoday.web.servlet.ViewDispatcher;
import cn.taketoday.web.view.AbstractViewResolver;
import cn.taketoday.web.view.FreeMarkerViewResolver;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

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
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import lombok.extern.slf4j.Slf4j;

/**
 * Load context.
 * 
 * @author Today <br>
 * 
 *         2018-06-23 16:14:53
 */
@Slf4j
@WebListener("WebApplicationContextLoader")
public class WebApplicationContextLoader implements ServletContextListener, Constant {

	private static final long serialVersionUID = 4983190133174606852L;

	/** context **/
	private static WebApplicationContext applicationContext;

	private DocumentBuilder builder;

	public WebApplicationContextLoader() throws ParserConfigurationException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setIgnoringComments(true);
		builder = factory.newDocumentBuilder();
		builder.setEntityResolver((publicId, systemId) -> {
			if (systemId.contains(DTD_NAME) || publicId.contains(DTD_NAME)) {
				return new InputSource(
						new ByteArrayInputStream("<?xml version=\"1.0\" encoding=\"UTF-8\"?>".getBytes()));
			}
			return null;
		});
	}

	public static WebApplicationContext getWebApplicationContext() {
		return applicationContext;
	}

	/**
	 * init framework.
	 * 
	 * @throws Exception
	 */
	private void initFrameWork(ServletContext servletContext) throws Exception {
		// find the configure file
		log.info("TODAY WEB Framework Is Looking For Configuration File.");

		String webMvcConfigLocation = servletContext.getInitParameter(WEB_MVC_CONFIG_LOCATION);

		if (StringUtils.isNotEmpty(webMvcConfigLocation)) {
			String[] files = webMvcConfigLocation.split("[;|,]");
			if (files == null || files.length == 0) {
				files = new String[] { webMvcConfigLocation };
			}
			for (String file : files) {
				URL resource = ClassUtils.getClassLoader().getResource(file);
				if (resource == null) {
					throw new ConfigurationException("You Provided Configuration File: [{}], Does Not Exist",
							webMvcConfigLocation);
				}
				try (InputStream inputStream = new FileInputStream(resource.getFile())) {
					registerXml(builder.parse(inputStream), webMvcConfigLocation);
				}
			}
			return;
		}
		findConfigFile(new File(servletContext.getRealPath(WEB_INF)));
	}

	/**
	 * Find configuration file.
	 * 
	 * @param dir
	 *            directory
	 * @throws Exception
	 */
	private void findConfigFile(File dir) throws Exception {

		log.debug("Enter [{}].", dir.getAbsolutePath());

		File[] listFiles = dir.listFiles(path -> (path.isDirectory() || path.getName().endsWith(".xml")));

		for (File file : listFiles) {
			if (file.isDirectory()) { // recursive
				findConfigFile(file);
				continue;
			}
			try (InputStream inputStream = new FileInputStream(file)) {
				registerXml(builder.parse(inputStream), file.getAbsolutePath());
			}
		}
	}

	/**
	 * configure with xml file
	 * 
	 * @param doc
	 *            xml file
	 * @throws Exception
	 */
	private void registerXml(Document doc, String filePath) throws Exception {
		Element root = doc.getDocumentElement();
		if (ROOT_ELEMENT.equals(root.getNodeName())) { // root element
			log.info("Found Configuration File: [{}].", filePath);
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
					case ELEMENT_COMMON :
						log.info("Start Configure Views.");
						// view init
						new ViewConfig().init(ele, applicationContext.getServletContext().getContextPath());
						break;
					case ELEMENT_STATIC_RESOURCES :
						String staticMapping = ele.getAttribute(ATTR_MAPPING);
						addDefaultServletMapping(staticMapping);
						break;
					case ELEMENT_MULTIPART :
						multipartResolver(ele);
						break;
					case ELEMENT_VIEW_RESOLVER :
						viewResolver(ele);
						break;
					case ELEMENT_EXCEPTION_RESOLVER :
						registerResolver(ele, ExceptionResolver.class, EXCEPTION_RESOLVER);
						break;
					case ELEMENT_PARAMETER_RESOLVER :
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
		applicationContext.registerBeanDefinition(name, parameterResolver);
		applicationContext.refresh(name);
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

		registerResolver(element, FreeMarkerViewResolver.class, VIEW_RESOLVER);

		AbstractViewResolver viewResolver = applicationContext.getBean(VIEW_RESOLVER, AbstractViewResolver.class);

		Properties properties = applicationContext.getBeanDefinitionRegistry().getProperties();

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
					case ELEMENT_VIEW_ENCODING :
						viewResolver.setEncoding(nodeValue);
						break;
					case ELEMENT_VIEW_PREFIX :
						viewResolver.setPrefix(nodeValue);
						break;
					case ELEMENT_VIEW_SUFFIX :
						viewResolver.setSuffix(nodeValue);
						break;
					case ELEMENT_VIEW_LOCALE :
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
				String nodeValue = PropertiesUtils.findInProperties(//
						properties, PropertiesUtils.findInProperties(properties, config.getTextContent())//
				);
				log.debug("Found Element -> [{}] = [{}]", elementName, nodeValue);
				switch (elementName) //
				{
					case ELEMENT_UPLOAD_FILE_SIZE_THRESHOLD :
						multipartResolver.setFileSizeThreshold(Integer.parseInt(nodeValue));
						break;
					case ELEMENT_UPLOAD_LOCATION :
						multipartResolver.setLocation(nodeValue);
						break;
					case ELEMENT_UPLOAD_MAX_FILE_SIZE :
						multipartResolver.setMaxFileSize(Long.parseLong(nodeValue));
						break;
					case ELEMENT_UPLOAD_MAX_REQUEST_SIZE :
						multipartResolver.setMaxRequestSize(Long.parseLong(nodeValue));
						break;
					case ELEMENT_UPLOAD_ENCODING :
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

		Set<String> urls = ViewDispatcher.VIEW_REQUEST_MAPPING.keySet();

		ServletContext servletContext = applicationContext.getServletContext();
		if (urls.size() > 0) {// register
			log.info("Register View Dispatcher Servlet: [{}].", ViewDispatcher.class);
			applicationContext.registerBeanDefinition(VIEW_DISPATCHER, ViewDispatcher.class);
			applicationContext.refresh(VIEW_DISPATCHER);

			Servlet viewServlet = applicationContext.getBean(ViewDispatcher.class);

			servletContext.addServlet(VIEW_DISPATCHER, viewServlet);
			ServletRegistration registration = servletContext.getServletRegistration(VIEW_DISPATCHER);
			String contextPath = servletContext.getContextPath();

			registration.addMapping(urls.stream()//
					.map(ac -> ac.replaceFirst(contextPath, ""))//
					.collect(Collectors.toSet())//
					.toArray(new String[0]));
		}

		if (DispatcherServlet.HANDLER_MAPPING_POOL.size() < 1) {
			return;
		}

		if (!applicationContext.containsBeanDefinition(DISPATCHER_SERVLET)) {
			log.info("Register Dispatcher Servlet: [{}].", DispatcherServlet.class);
			applicationContext.registerBeanDefinition(DISPATCHER_SERVLET, DispatcherServlet.class);
			applicationContext.refresh(DISPATCHER_SERVLET);
		}

		servletContext.addServlet(DISPATCHER_SERVLET, applicationContext.getBean(DISPATCHER_SERVLET, Servlet.class));

		Dynamic registration = (Dynamic) servletContext.getServletRegistration(DISPATCHER_SERVLET);

		// set multipartResolver
		AbstractMultipartResolver multipartResolver = applicationContext.getBean(MULTIPART_RESOLVER,
				AbstractMultipartResolver.class);

		MultipartConfigElement multipartConfig = new MultipartConfigElement(//
				multipartResolver.getLocation(), //
				multipartResolver.getMaxFileSize(), //
				multipartResolver.getMaxRequestSize(), //
				multipartResolver.getFileSizeThreshold()//
		);
		registration.setMultipartConfig(multipartConfig);
		registration.addMapping(DISPATCHER_SERVLET_MAPPING);
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
			log.debug("Add default servlet default mapping: {}.", Arrays.toString(defaultUrlPatterns));
			return;
		}

		servletRegistration.addMapping(staticMapping);

		log.debug("Add default servlet mapping: [{}].", servletRegistration.getMappings());
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

		log.debug("No default servlet registration , Create.");
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

		log.debug("Add default servlet default mapping: {}.", Arrays.toString(defaultUrlPatterns));
	}

	/**
	 * check resolver
	 * 
	 * @throws BeanDefinitionStoreException
	 */
	private void checkResolver() throws BeanDefinitionStoreException {

		if (!applicationContext.containsBeanDefinition(EXCEPTION_RESOLVER)) {
			applicationContext.registerBeanDefinition(EXCEPTION_RESOLVER, DefaultExceptionResolver.class);
			applicationContext.refresh(EXCEPTION_RESOLVER);
			log.info("Use default exception resolver: [{}].", DefaultExceptionResolver.class);
		}
		if (!applicationContext.containsBeanDefinition(MULTIPART_RESOLVER)) {
			// default multipart resolver
			applicationContext.registerBeanDefinition(MULTIPART_RESOLVER, DefaultMultipartResolver.class);
			applicationContext.refresh(MULTIPART_RESOLVER);
			log.info("Use default multipart resolver: [{}].", DefaultMultipartResolver.class);
		}
		if (!applicationContext.containsBeanDefinition(VIEW_RESOLVER)) {
			// use freemarker view resolver
			applicationContext.registerBeanDefinition(VIEW_RESOLVER, FreeMarkerViewResolver.class);
			applicationContext.refresh(VIEW_RESOLVER);
			log.info("Use default view resolver: [{}].", FreeMarkerViewResolver.class);
		}
		if (!applicationContext.containsBeanDefinition(PARAMETER_RESOLVER)) {
			// use default parameter resolver
			applicationContext.registerBeanDefinition(PARAMETER_RESOLVER, DefaultParameterResolver.class);
			applicationContext.refresh(PARAMETER_RESOLVER);
			log.info("Use default parameter resolver: [{}].", DefaultParameterResolver.class);
		}
	}

	/**
	 * web application init
	 */
	@Override
	public void contextInitialized(ServletContextEvent sce) {

		long start = System.currentTimeMillis(); // start millis

		log.info("Your Application Starts To Be Initialized At: [{}].",
				new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

		ServletContext servletContext = sce.getServletContext();

		servletContext.setRequestCharacterEncoding(DEFAULT_ENCODING);
		servletContext.setResponseCharacterEncoding(DEFAULT_ENCODING);

		applicationContext = new DefaultWebApplicationContext(servletContext);

		try {
			// init start
			initFrameWork(servletContext);
			log.info("Start Configure Actions.");

			ActionConfig actionConfig = applicationContext.getBean(ACTION_CONFIG, ActionConfig.class);

			actionConfig.init();

			// check all resolver
			checkResolver();

			checkDefaultServlet();

			// register servlet
			doRegisterServlet();

			removeFrameWorkBeanDefinitions();
			builder = null;
			applicationContext.loadSuccess();

			applicationContext.publishEvent(new ApplicationStartedEvent(applicationContext));

			log.info("Your Application Started Successfully, It takes a total of [{}] ms.",
					System.currentTimeMillis() - start);
			// init end
		} catch (Throwable ex) {
			log.error("Initialized ERROR: [{}] caused by {}", ex.getMessage(), ex.getCause(), ex);
		}
	}

	/**
	 * 
	 * @throws NoSuchBeanDefinitionException
	 */
	private void removeFrameWorkBeanDefinitions() throws NoSuchBeanDefinitionException {
		applicationContext.removeBeanDefinition(ACTION_CONFIG);
		applicationContext.removeBeanDefinition(VIEW_RESOLVER);
		applicationContext.removeBeanDefinition(VIEW_DISPATCHER);
		applicationContext.removeBeanDefinition(EXCEPTION_RESOLVER);
		applicationContext.removeBeanDefinition(DISPATCHER_SERVLET);
		applicationContext.removeBeanDefinition(MULTIPART_RESOLVER);
		applicationContext.removeBeanDefinition(PARAMETER_RESOLVER);
	}

}
