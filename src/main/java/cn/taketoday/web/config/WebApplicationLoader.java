/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright ©  TODAY & 2017 - 2019 All Rights Reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web.config;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.EventListener;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.MultipartConfigElement;
import javax.servlet.Servlet;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletSecurityElement;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebListener;
import javax.servlet.annotation.WebServlet;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import cn.taketoday.context.env.ConfigurableEnvironment;
import cn.taketoday.context.exception.BeanDefinitionStoreException;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.io.Resource;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ContextUtils;
import cn.taketoday.context.utils.DataSize;
import cn.taketoday.context.utils.ExceptionUtils;
import cn.taketoday.context.utils.OrderUtils;
import cn.taketoday.context.utils.ResourceUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.Constant;
import cn.taketoday.web.DefaultWebApplicationContext;
import cn.taketoday.web.ServletContextInitializer;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.config.initializer.DispatcherServletInitializer;
import cn.taketoday.web.config.initializer.WebFilterInitializer;
import cn.taketoday.web.config.initializer.WebListenerInitializer;
import cn.taketoday.web.config.initializer.WebServletInitializer;
import cn.taketoday.web.event.WebApplicationFailedEvent;
import cn.taketoday.web.event.WebApplicationStartedEvent;
import cn.taketoday.web.listener.RequestContextHolder;
import cn.taketoday.web.mapping.ResourceMapping;
import cn.taketoday.web.mapping.ResourceMappingRegistry;
import cn.taketoday.web.multipart.AbstractMultipartResolver;
import cn.taketoday.web.multipart.DefaultMultipartResolver;
import cn.taketoday.web.multipart.MultipartResolver;
import cn.taketoday.web.resolver.DefaultExceptionResolver;
import cn.taketoday.web.resolver.DefaultParameterResolver;
import cn.taketoday.web.resolver.ExceptionResolver;
import cn.taketoday.web.resolver.ParameterResolver;
import cn.taketoday.web.servlet.ResourceServlet;
import cn.taketoday.web.view.AbstractViewResolver;
import cn.taketoday.web.view.FreeMarkerViewResolver;
import cn.taketoday.web.view.ViewResolver;

/**
 * Initialize Web application in a server like tomcat, jetty, undertow
 * 
 * @author Today <br>
 *         2019-01-12 17:28
 */
@SuppressWarnings("serial")
public class WebApplicationLoader implements ServletContainerInitializer, Constant {

    private static final Logger log = LoggerFactory.getLogger(WebApplicationLoader.class);

    private ViewConfiguration viewConfiguration;

    /** context **/
    private static WebApplicationContext applicationContext;

    private DocumentBuilder builder;

    /**
     * Get {@link WebApplicationContext}
     * 
     * @return {@link WebApplicationContext}
     */
    public final static WebApplicationContext getWebApplicationContext() {
        return applicationContext;
    }

    /**
     * Initialize framework.
     * 
     * @throws Throwable
     *             if any Throwable occurred
     */
    private void initFrameWorkFromWebMvcXml(ServletContext servletContext, CompositeWebMvcConfiguration mvcConfiguration) throws Throwable {
        // find the configure file
        log.info("TODAY WEB Framework Is Looking For Configuration File.");

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        factory.setIgnoringComments(true);
        builder = factory.newDocumentBuilder();
        builder.setEntityResolver((publicId, systemId) -> {
            if (systemId.contains(DTD_NAME) || publicId.contains(DTD_NAME)) {
                return new InputSource(//
                        new ByteArrayInputStream("<?xml version=\"1.0\" encoding=\"UTF-8\"?>".getBytes())//
                );
            }
            return null;
        });

        String webMvcConfigLocation = getWebApplicationContext().getEnvironment().getProperty(WEB_MVC_CONFIG_LOCATION);

        if (StringUtils.isEmpty(webMvcConfigLocation)) {
            webMvcConfigLocation = servletContext.getInitParameter(WEB_MVC_CONFIG_LOCATION);
            if (StringUtils.isEmpty(webMvcConfigLocation)) {
                final String rootPath = servletContext.getRealPath("/");
                log.debug("Find Configuration File From Root Path: [{}]", rootPath);
                findConfiguration(new File(rootPath), mvcConfiguration);
                return;
            }
        }

        for (final String file : StringUtils.split(webMvcConfigLocation)) {

            final Resource resource = ResourceUtils.getResource(file);
            if (resource == null) {
                final ConfigurationException configurationException = //
                        new ConfigurationException("Your Provided Configuration File: [" + file + "], Does Not Exist");
                servletContext.log(configurationException.getMessage(), configurationException);
                throw configurationException;
            }
            try (InputStream inputStream = resource.getInputStream()) {
                registerXml(builder.parse(inputStream), file, mvcConfiguration);// fixed
            }
        }
        builder = null;
    }

    /**
     * Find configuration file.
     * 
     * @param dir
     *            directory
     * @throws Exception
     */
    private void findConfiguration(File dir, CompositeWebMvcConfiguration mvcConfiguration) throws Throwable {
        log.debug("Enter [{}]", dir.getAbsolutePath());
        File[] listFiles = dir.listFiles(path -> (path.isDirectory() || path.getName().endsWith(".xml")));
        if (listFiles == null) {
            log.error("File: [{}] Does not exist", dir);
            return;
        }
        for (File file : listFiles) {
            if (file.isDirectory()) { // recursive
                findConfiguration(file, mvcConfiguration);
                continue;
            }

            try (InputStream inputStream = new FileInputStream(file)) {
                registerXml(builder.parse(inputStream), file.getAbsolutePath(), mvcConfiguration);
            }
        }
    }

    /**
     * configure with xml file
     * 
     * @param doc
     *            xml file
     * @throws Throwable
     */
    private final void registerXml(final Document doc, final String filePath, //
            CompositeWebMvcConfiguration mvcConfiguration) throws Throwable //
    {

        final Element root = doc.getDocumentElement();
        if (ROOT_ELEMENT.equals(root.getNodeName())) { // root element
            log.info("Found Configuration File: [{}].", filePath);
            configStart(root, mvcConfiguration);
        }
    }

    /**
     * start configure.
     * 
     * @param root
     *            rootElement
     * @throws Exception
     */
    private void configStart(Element root, CompositeWebMvcConfiguration mvcConfiguration) throws Throwable {

        NodeList nl = root.getChildNodes();

        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node instanceof Element) {
                Element ele = (Element) node;
                String nodeName = ele.getNodeName();

                log.debug("Found Element: [{}]", nodeName);
                switch (nodeName) //
                {
                    case ELEMENT_CONTROLLER :
                        // view configuration
                        viewConfiguration.configuration(ele);
                        break;
                    case ELEMENT_DEFAULT_SERVLET : {
                        String mapping = ele.getAttribute(ATTR_MAPPING);
                        if (StringUtils.isNotEmpty(mapping)) {
                            addDefaultServletMapping(mapping, mvcConfiguration);
                        }
                        break;
                    }
                    case ELEMENT_RESOURCES : {
//                        resourceMappingRegistry(ele);
                        break;
                    }
                    case ELEMENT_MULTIPART :
                        multipartResolver(ele, mvcConfiguration);
                        break;
                    case ELEMENT_VIEW_RESOLVER :
                        viewResolver(ele);
                        break;
                    case ELEMENT_EXCEPTION_RESOLVER :
                        registerResolver(ele, DefaultExceptionResolver.class, EXCEPTION_RESOLVER, true);
                        break;
                    case ELEMENT_PARAMETER_RESOLVER :
                        registerResolver(ele, DefaultParameterResolver.class, PARAMETER_RESOLVER, true);
                        break;
                    case ELEMENT_DISPATCHER_SERVLET : {
                        String mapping = ele.getAttribute(ATTR_MAPPING);
                        if (StringUtils.isEmpty(mapping)) {
                            log.warn("Attribute: [{}] on [{}] is Empty", ATTR_MAPPING, nodeName);
                            break;
                        }
                        DispatcherServletInitializer bean = applicationContext.getBean(DispatcherServletInitializer.class);
                        log.info("Set DispatcherServlet Url Mappings: [{}]", mapping);
                        bean.setDispatcherServletMapping(mapping);
                        break;
                    }
                    default:
                        log.warn("This element: [{}] is not supported.", nodeName);
                        break;
                }
            }
        }
    }

//    private void resourceMappingRegistry(Element ele) {
//        final ResourceMappingRegistry resourceMappingRegistry = applicationContext.getBean(ResourceMappingRegistry.class);
//
////        resourceMappingRegistry.
//
//    }

    /**
     * Register resolver to application context.
     * 
     * @param element
     *            xml element
     * @param defaultClass
     *            default class
     * @param name
     *            bean name
     * @param refresh
     *            refresh ?
     * @return Resolver's Class
     * @throws ClassNotFoundException
     * @throws BeanDefinitionStoreException
     */
    private static Class<?> registerResolver(Element element, Class<?> defaultClass, String name, boolean refresh) //
            throws ClassNotFoundException, BeanDefinitionStoreException //
    {
        String attrClass = element.getAttribute(ATTR_CLASS); // class="cn.taketoday..."

        Class<?> resolverClass = null;
        if (!defaultClass.getName().equals(attrClass)) { // Custom
            resolverClass = ClassUtils.forName(attrClass);
        }
        else {
            resolverClass = defaultClass; // default
        }
        // register resolver
        applicationContext.registerBean(name, resolverClass);
        log.info("Register [{}] onto [{}]", name, resolverClass.getName());

        if (refresh) {
            applicationContext.refresh(name);
        }
        return resolverClass;
    }

    /**
     * configure view resolver.
     * 
     * @param element
     *            xml element
     * @throws Throwable
     */
    private void viewResolver(Element element) throws Throwable {

        registerResolver(element, FreeMarkerViewResolver.class, VIEW_RESOLVER, false);

        final Object viewResolver = getWebApplicationContext().getBean(VIEW_RESOLVER);

        if (viewResolver instanceof AbstractViewResolver) {
            doAbstractViewResolver(element, (AbstractViewResolver) viewResolver);
        }
    }

    /**
     * 
     * @param element
     *            xml element
     * @param abstractViewResolver
     *            Abstract View Resolver instance
     */
    private void doAbstractViewResolver(Element element, AbstractViewResolver abstractViewResolver) {

        final Properties properties = applicationContext.getEnvironment().getProperties();
        final NodeList childNodes = element.getChildNodes();
        final int length = childNodes.getLength();

        for (int j = 0; j < length; j++) {
            Node item = childNodes.item(j);
            if (item instanceof Element) {
                Element config = (Element) item;
                String eleName = config.getNodeName();
                String nodeValue = ContextUtils.resolvePlaceholder(properties, config.getTextContent());

                log.debug("Found Element: [{}] = [{}]", eleName, nodeValue);
                switch (eleName) //
                {
                    case ELEMENT_VIEW_ENCODING :
                        abstractViewResolver.setEncoding(nodeValue);
                        break;
                    case ELEMENT_VIEW_PREFIX :
                        abstractViewResolver.setPrefix(nodeValue);
                        break;
                    case ELEMENT_VIEW_SUFFIX :
                        abstractViewResolver.setSuffix(nodeValue);
                        break;
                    case ELEMENT_VIEW_LOCALE :
                        abstractViewResolver.setLocale(new Locale(nodeValue));
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
     * @param mvcConfiguration
     * @throws Throwable
     */
    private void multipartResolver(Element element, CompositeWebMvcConfiguration mvcConfiguration) throws Throwable {

        final WebApplicationContext applicationContext = getWebApplicationContext();
        final Class<?> multipartResolverClass = //
                registerResolver(element, DefaultMultipartResolver.class, MULTIPART_RESOLVER, false);

        final Object multipartResolver = ClassUtils.newInstance(multipartResolverClass);

        if (multipartResolver instanceof AbstractMultipartResolver) {
            doAbstractMultipartResolver(element, (AbstractMultipartResolver) multipartResolver, mvcConfiguration);
        }
        // refresh resolver
        applicationContext.registerSingleton(MULTIPART_RESOLVER, multipartResolver);
        applicationContext.refresh(MULTIPART_RESOLVER);
    }

    /**
     * @param element
     *            xml element
     * @param abstractMultipartResolver
     *            Abstract Multipart Resolver instance
     * @param mvcConfiguration
     */
    private void doAbstractMultipartResolver(Element element, //
            AbstractMultipartResolver abstractMultipartResolver, CompositeWebMvcConfiguration mvcConfiguration)//
    {

        final Properties properties = getWebApplicationContext().getEnvironment().getProperties();
        final NodeList childNodes = element.getChildNodes();

        for (int j = 0; j < childNodes.getLength(); j++) {
            final Node item = childNodes.item(j);
            if (item instanceof Element) {

                Element multipart = (Element) item;
                String elementName = multipart.getNodeName();
                String nodeValue = ContextUtils.resolvePlaceholder(properties, multipart.getTextContent());

                log.debug("Found Element: [{}] = [{}]", elementName, nodeValue);
                switch (elementName) //
                {
                    case ELEMENT_UPLOAD_LOCATION :
                        abstractMultipartResolver.setLocation(nodeValue);
                        break;
                    case ELEMENT_UPLOAD_ENCODING :
                        abstractMultipartResolver.setEncoding(nodeValue);
                        break;
                    case ELEMENT_UPLOAD_MAX_FILE_SIZE :
                        abstractMultipartResolver.setMaxFileSize(DataSize.parse(nodeValue).toBytes());
                        break;
                    case ELEMENT_UPLOAD_MAX_REQUEST_SIZE :
                        abstractMultipartResolver.setMaxRequestSize(DataSize.parse(nodeValue).toBytes());
                        break;
                    case ELEMENT_UPLOAD_FILE_SIZE_THRESHOLD :
                        abstractMultipartResolver.setFileSizeThreshold((int) DataSize.parse(nodeValue).toBytes());
                        break;
                    default:
                        log.error("This element -> [{}] is not supported.", elementName);
                        break;
                }
            }
        }
        mvcConfiguration.configMultipartResolver(abstractMultipartResolver);
    }

    private static void addDefaultServletMapping(String staticMapping, CompositeWebMvcConfiguration mvcConfiguration) throws Throwable {

        if (StringUtils.isEmpty(staticMapping)) {
            throw new ConfigurationException("Static sources mapping can't be empty, please check your configuration");
        }

        final ServletContext servletContext = getWebApplicationContext().getServletContext();
        final ServletRegistration servletRegistration = servletContext.getServletRegistration(DEFAULT);

        if (servletRegistration == null) {
            throw new ConfigurationException("There isn't a default servlet, please check your configuration");
        }

        servletRegistration.addMapping(StringUtils.split(staticMapping));

        mvcConfiguration.configDefaultServlet(servletRegistration);

        log.debug("Add default servlet mapping: [{}].", servletRegistration.getMappings());
    }

    /**
     * Check resolvers
     * 
     * @param mvcConfiguration
     * 
     * @throws BeanDefinitionStoreException
     */
    private static void checkFrameWorkResolvers(CompositeWebMvcConfiguration mvcConfiguration) {

        WebApplicationContext applicationContext = getWebApplicationContext();

        if (!applicationContext.containsBeanDefinition(ExceptionResolver.class)) {
            applicationContext.registerBean(EXCEPTION_RESOLVER, DefaultExceptionResolver.class);
            applicationContext.refresh(EXCEPTION_RESOLVER);
            log.info("Use default exception resolver: [{}].", DefaultExceptionResolver.class);
        }

        if (!applicationContext.containsBeanDefinition(MultipartResolver.class)) {
            // default multipart resolver
            applicationContext.registerBean(MULTIPART_RESOLVER, DefaultMultipartResolver.class);
            applicationContext.refresh(MULTIPART_RESOLVER);
            log.info("Use default multipart resolver: [{}].", DefaultMultipartResolver.class);
        }
        if (!applicationContext.containsBeanDefinition(ViewResolver.class)) {
            // use freemarker view resolver
            applicationContext.registerBean(VIEW_RESOLVER, FreeMarkerViewResolver.class);
            applicationContext.refresh(VIEW_RESOLVER);
            log.info("Use default view resolver: [{}].", FreeMarkerViewResolver.class);
        }
        if (!applicationContext.containsBeanDefinition(ParameterResolver.class)) {
            // use default parameter resolver
            applicationContext.registerBean(PARAMETER_RESOLVER, DefaultParameterResolver.class);
            applicationContext.refresh(PARAMETER_RESOLVER);
            log.info("Use default parameter resolver: [{}].", DefaultParameterResolver.class);
        }

        final AbstractViewResolver bean = applicationContext.getBean(AbstractViewResolver.class);
        if (bean != null) {
            mvcConfiguration.configViewResolver(bean);
        }
    }

    /**
     * Prepare {@link WebApplicationContext}
     * 
     * @param classes
     *            classes to scan
     * @param servletContext
     *            {@link ServletContext}
     * @return startup Date
     */
    private long prepareApplicationContext(ServletContext servletContext) {

        final Object attribute = servletContext.getAttribute(KEY_WEB_APPLICATION_CONTEXT);
        if (attribute instanceof WebApplicationContext) {
            applicationContext = (WebApplicationContext) attribute;
            return applicationContext.getStartupDate();
        }

        final long startupDate = System.currentTimeMillis();
        log.info("Your application starts to be initialized at: [{}].", //
                new SimpleDateFormat(Constant.DEFAULT_DATE_FORMAT).format(startupDate));

        // fix: applicationContext NullPointerException
        applicationContext = new DefaultWebApplicationContext();
        applicationContext.setServletContext(servletContext);
        applicationContext.loadContext(Constant.BLANK);
        return startupDate;
    }

    @Override
    public void onStartup(Set<Class<?>> classes, ServletContext servletContext) throws ServletException {

        Objects.requireNonNull(servletContext, "ServletContext can't be null");

        final long start = prepareApplicationContext(servletContext);
        try {

            final WebApplicationContext applicationContext = getWebApplicationContext();
            final ConfigurableEnvironment environment = applicationContext.getEnvironment();

            try {
                servletContext.setRequestCharacterEncoding(DEFAULT_ENCODING);
                servletContext.setResponseCharacterEncoding(DEFAULT_ENCODING);
            }
            catch (Throwable e) {
                // Waiting for Jetty 10.0.0
            }

            CompositeWebMvcConfiguration mvcConfiguration = //
                    new CompositeWebMvcConfiguration(applicationContext.getBeans(WebMvcConfiguration.class));

            if (environment.getProperty(ENABLE_WEB_MVC_XML, Boolean::parseBoolean, true)) {
                this.viewConfiguration = applicationContext.getBean(VIEW_CONFIG, ViewConfiguration.class);
                initFrameWorkFromWebMvcXml(servletContext, mvcConfiguration);
            }

            // check all resolver
            checkFrameWorkResolvers(mvcConfiguration);

            // register servlet
            List<ServletContextInitializer> contextInitializers = //
                    applicationContext.getBeans(ServletContextInitializer.class);

            configResourceRegistry(contextInitializers, mvcConfiguration);

            applyFilter(applicationContext, contextInitializers);
            applyServlet(applicationContext, contextInitializers);
            applyListener(applicationContext, contextInitializers);

            if (environment.getProperty(ENABLE_REQUEST_CONTEXT, Boolean::parseBoolean, false)) {
                contextInitializers.add(new WebListenerInitializer<RequestContextHolder>(new RequestContextHolder()));
            }

            OrderUtils.reversedSort(contextInitializers);

            for (final ServletContextInitializer servletContextInitializer : contextInitializers) {
                servletContextInitializer.onStartup(servletContext);
            }

            applicationContext.publishEvent(new WebApplicationStartedEvent(applicationContext));
            if (environment.getProperty(ENABLE_WEB_STARTED_LOG, Boolean::parseBoolean, true)) {
                log.info("Your Application Started Successfully, It takes a total of [{}] ms.", //
                        System.currentTimeMillis() - start//
                );
            }

            Runtime.getRuntime().addShutdownHook(new Thread(applicationContext::close));
        }
        catch (Throwable ex) {
            ex = ExceptionUtils.unwrapThrowable(ex);
            applicationContext.publishEvent(new WebApplicationFailedEvent(applicationContext, ex));
            log.error("Your Application Initialized ERROR: [{}]", ex.getMessage(), ex);
            throw new ConfigurationException(ex);
        }
    }

    protected void configResourceRegistry(List<ServletContextInitializer> contextInitializers, //
            CompositeWebMvcConfiguration configuration)//
    {

        final ResourceMappingRegistry resourceMappingRegistry = applicationContext.getBean(ResourceMappingRegistry.class);

        configuration.configResourceMappings(resourceMappingRegistry);

        if (resourceMappingRegistry.isEmpty()) {
            return;
        }

        if (!applicationContext.containsBeanDefinition(ResourceServlet.class)) {
            applicationContext.registerBean(Constant.RESOURCE_SERVLET, ResourceServlet.class);
        }

        final ResourceServlet resourceServlet = applicationContext.getBean(ResourceServlet.class);

        WebServletInitializer<ResourceServlet> resourceServletInitializer = new WebServletInitializer<>(resourceServlet);

        final Set<String> urlMappings = new HashSet<>();

        final List<ResourceMapping> resourceHandlerMappings = resourceMappingRegistry.getResourceHandlerMappings();
        for (ResourceMapping resourceMapping : resourceHandlerMappings) {
            final String[] pathPatterns = resourceMapping.getPathPatterns();
            for (String pathPattern : pathPatterns) {
                if (pathPattern.endsWith("/**")) {
                    urlMappings.add(pathPattern.substring(0, pathPattern.length() - 1));
                }
                else {
                    urlMappings.add(pathPattern);
                }
            }
        }

        configuration.configResourceServletUrlMappings(urlMappings);
        resourceServletInitializer.setUrlMappings(urlMappings);
        resourceServletInitializer.setName(Constant.RESOURCE_SERVLET);

        log.info("Set ResourceServlet Url Mappings: [{}]", urlMappings);
        contextInitializers.add(resourceServletInitializer);
    }

    private void applyFilter(final WebApplicationContext applicationContext, Collection<ServletContextInitializer> contextInitializers) {

        Collection<Filter> filters = applicationContext.getAnnotatedBeans(WebFilter.class);
        for (Filter filter : filters) {

            final Class<?> beanClass = filter.getClass();

            WebFilterInitializer<Filter> webFilterInitializer = new WebFilterInitializer<>(filter);

            WebFilter webFilter = beanClass.getAnnotation(WebFilter.class);

            final Set<String> urlPatterns = new HashSet<>();
            Collections.addAll(urlPatterns, webFilter.value());
            Collections.addAll(urlPatterns, webFilter.urlPatterns());

            webFilterInitializer.addUrlMappings(StringUtils.toStringArray(urlPatterns));

            webFilterInitializer.addServletNames(webFilter.servletNames());
            webFilterInitializer.setAsyncSupported(webFilter.asyncSupported());

            for (WebInitParam initParam : webFilter.initParams()) {
                webFilterInitializer.addInitParameter(initParam.name(), initParam.value());
            }

            String name = webFilter.filterName();
            if (StringUtils.isEmpty(name)) {
                final String displayName = webFilter.displayName();
                if (StringUtils.isEmpty(displayName)) {
                    name = applicationContext.getBeanName(beanClass);
                }
                else {
                    name = displayName;
                }
            }

            webFilterInitializer.setName(name);
            webFilterInitializer.setDispatcherTypes(webFilter.dispatcherTypes());

            contextInitializers.add(webFilterInitializer);
        }
    }

    private void applyServlet(final WebApplicationContext applicationContext, Collection<ServletContextInitializer> contextInitializers) {

        Collection<Servlet> servlets = applicationContext.getAnnotatedBeans(WebServlet.class);

        for (Servlet servlet : servlets) {

            final Class<?> beanClass = servlet.getClass();

            WebServletInitializer<Servlet> webServletInitializer = new WebServletInitializer<>(servlet);

            WebServlet webServlet = beanClass.getAnnotation(WebServlet.class);

            String[] urlPatterns = webServlet.urlPatterns();
            if (StringUtils.isArrayEmpty(urlPatterns)) {
                urlPatterns = new String[] { applicationContext.getBeanName(beanClass) };
            }
            webServletInitializer.addUrlMappings(urlPatterns);
            webServletInitializer.setLoadOnStartup(webServlet.loadOnStartup());
            webServletInitializer.setAsyncSupported(webServlet.asyncSupported());

            for (WebInitParam initParam : webServlet.initParams()) {
                webServletInitializer.addInitParameter(initParam.name(), initParam.value());
            }

            final MultipartConfig multipartConfig = beanClass.getAnnotation(MultipartConfig.class);
            if (multipartConfig != null) {
                webServletInitializer.setMultipartConfig(new MultipartConfigElement(multipartConfig));
            }
            final ServletSecurity servletSecurity = beanClass.getAnnotation(ServletSecurity.class);
            if (servletSecurity != null) {
                webServletInitializer.setServletSecurity(new ServletSecurityElement(servletSecurity));
            }

            String name = webServlet.name();
            if (StringUtils.isEmpty(name)) {

                final String displayName = webServlet.displayName();
                if (StringUtils.isEmpty(displayName)) {
                    name = applicationContext.getBeanName(beanClass);
                }
                else {
                    name = displayName;
                }
            }
            webServletInitializer.setName(name);

            contextInitializers.add(webServletInitializer);
        }
    }

    private void applyListener(final WebApplicationContext applicationContext, Collection<ServletContextInitializer> contextInitializers) {

        Collection<EventListener> eventListeners = applicationContext.getAnnotatedBeans(WebListener.class);
        for (EventListener eventListener : eventListeners) {
            contextInitializers.add(new WebListenerInitializer<>(eventListener));
        }
    }

}
