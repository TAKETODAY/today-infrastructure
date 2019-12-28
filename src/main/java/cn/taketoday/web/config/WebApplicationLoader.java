/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2019 All Rights Reserved.
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.web.config;

import static cn.taketoday.context.utils.ContextUtils.resolveProps;
import static cn.taketoday.context.utils.ContextUtils.resolveValue;
import static cn.taketoday.web.resolver.method.ConverterParameterResolver.convert;
import static cn.taketoday.web.resolver.method.DelegatingParameterResolver.delegate;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.PathMatcher;
import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.annotation.Env;
import cn.taketoday.context.annotation.Props;
import cn.taketoday.context.annotation.Value;
import cn.taketoday.context.conversion.TypeConverter;
import cn.taketoday.context.env.Environment;
import cn.taketoday.context.exception.BeanDefinitionStoreException;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.exception.NoSuchBeanDefinitionException;
import cn.taketoday.context.io.Resource;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ConvertUtils;
import cn.taketoday.context.utils.OrderUtils;
import cn.taketoday.context.utils.ResourceUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.Constant;
import cn.taketoday.web.MessageConverter;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.WebApplicationContextSupport;
import cn.taketoday.web.annotation.RequestAttribute;
import cn.taketoday.web.event.WebApplicationStartedEvent;
import cn.taketoday.web.handler.HandlerMethod;
import cn.taketoday.web.multipart.MultipartConfiguration;
import cn.taketoday.web.registry.FunctionHandlerRegistry;
import cn.taketoday.web.registry.HandlerMethodRegistry;
import cn.taketoday.web.registry.ResourceHandlerRegistry;
import cn.taketoday.web.resolver.method.ArrayParameterResolver;
import cn.taketoday.web.resolver.method.BeanParameterResolver;
import cn.taketoday.web.resolver.method.CollectionParameterResolver;
import cn.taketoday.web.resolver.method.CookieParameterResolver;
import cn.taketoday.web.resolver.method.HeaderParameterResolver;
import cn.taketoday.web.resolver.method.MapParameterResolver;
import cn.taketoday.web.resolver.method.ModelParameterResolver;
import cn.taketoday.web.resolver.method.ParameterResolver;
import cn.taketoday.web.resolver.method.ParameterResolvers;
import cn.taketoday.web.resolver.method.PathVariableParameterResolver;
import cn.taketoday.web.resolver.method.RequestBodyParameterResolver;
import cn.taketoday.web.resolver.method.StreamParameterResolver;
import cn.taketoday.web.resolver.method.ThrowableHandlerParameterResolver;
import cn.taketoday.web.view.ImageResultHandler;
import cn.taketoday.web.view.ModelAndViewResultHandler;
import cn.taketoday.web.view.ObjectResultHandler;
import cn.taketoday.web.view.ResourceResultHandler;
import cn.taketoday.web.view.ResponseBodyResultHandler;
import cn.taketoday.web.view.ResultHandler;
import cn.taketoday.web.view.ResultHandlers;
import cn.taketoday.web.view.TemplateResultHandler;
import cn.taketoday.web.view.VoidResultHandler;
import cn.taketoday.web.view.template.AbstractTemplateViewResolver;
import cn.taketoday.web.view.template.DefaultTemplateViewResolver;
import cn.taketoday.web.view.template.TemplateViewResolver;

/**
 * @author TODAY <br>
 *         2019-07-10 23:12
 */
@SuppressWarnings("serial")
public class WebApplicationLoader extends WebApplicationContextSupport implements WebApplicationInitializer, Constant {

    private static final Logger log = LoggerFactory.getLogger(WebApplicationLoader.class);

    /**
     * Get {@link WebApplicationContext}
     * 
     * @return {@link WebApplicationContext}
     */
    public WebApplicationContext obtainApplicationContext() {
        return (WebApplicationContext) super.obtainApplicationContext();
    }

    @Override
    public void onStartup(WebApplicationContext context) throws Throwable {
        setApplicationContext(context);

        final Environment environment = context.getEnvironment();

        final WebMvcConfiguration mvcConfiguration = getWebMvcConfiguration(context);

        configureResultHandler(mvcConfiguration);

        final Class<Object> loaderClass = ClassUtils.loadClass("freemarker.cache.TemplateLoader");
        if (loaderClass != null) {
            configureTemplateLoader(context.getBeans(loaderClass), mvcConfiguration);
        }

        configureTypeConverter(context.getBeans(TypeConverter.class), mvcConfiguration);
        configureParameterResolver(context.getBeans(ParameterResolver.class), mvcConfiguration);
        configureResourceHandler(context.getBean(ResourceHandlerRegistry.class), mvcConfiguration);
        configureFunctionHandler(context.getBean(FunctionHandlerRegistry.class), mvcConfiguration);

        if (environment.getProperty(ENABLE_WEB_MVC_XML, Boolean::parseBoolean, true)) {
            initFrameWorkFromWebMvcXml();
        }

        // check all resolver
        checkFrameWorkResolvers(context);

        initializerStartup(context, mvcConfiguration);

        context.publishEvent(new WebApplicationStartedEvent(context));
        if (environment.getProperty(ENABLE_WEB_STARTED_LOG, Boolean::parseBoolean, true)) {
            log.info("Your Application Started Successfully, It takes a total of [{}] ms.", //
                     System.currentTimeMillis() - context.getStartupDate()//
            );
        }

        Runtime.getRuntime().addShutdownHook(new Thread(context::close));
        System.gc();
    }

    protected void configureFunctionHandler(FunctionHandlerRegistry registry, WebMvcConfiguration mvcConfiguration) {
        mvcConfiguration.configureFunctionHandler(registry);
    }

    /**
     * Configure Freemarker's TemplateLoader s
     * 
     * @param loaders
     *            TemplateLoaders
     * @since 2.3.7
     */
    protected <T> void configureTemplateLoader(List<T> beans, WebMvcConfiguration mvcConfiguration) {
        mvcConfiguration.configureTemplateLoader(beans);
    }

    /**
     * Configure {@link TypeConverter} to resolve convert request parameters
     * 
     * @param typeConverters
     *            Type converters
     * @param mvcConfiguration
     *            All {@link WebMvcConfiguration} object
     */
    protected void configureTypeConverter(List<TypeConverter> typeConverters, WebMvcConfiguration mvcConfiguration) {

        mvcConfiguration.configureTypeConverter(typeConverters);

        ConvertUtils.addConverter(typeConverters);
    }

    /**
     * Configure {@link ResultHandler} to resolve handler method result
     * 
     * @param handlers
     *            {@link ResultHandler} registry
     * @param mvcConfiguration
     *            All {@link WebMvcConfiguration} object
     */
    protected void configureResultHandler(WebMvcConfiguration mvcConfiguration) {

        final WebApplicationContext webApplicationContext = obtainApplicationContext();

        List<ResultHandler> handlers = webApplicationContext.getBeans(ResultHandler.class);

        final TemplateViewResolver viewResolver = getTemplateViewResolver(mvcConfiguration);
        final Environment environment = webApplicationContext.getEnvironment();
        int bufferSize = Integer.parseInt(environment.getProperty(DOWNLOAD_BUFF_SIZE, "10240"));

        final MessageConverter messageConverter = webApplicationContext.getBean(MessageConverter.class);

        handlers.add(new ImageResultHandler());
        handlers.add(new ResourceResultHandler(bufferSize));
        handlers.add(new TemplateResultHandler(viewResolver));
        handlers.add(new VoidResultHandler(viewResolver, messageConverter, bufferSize));
        handlers.add(new ObjectResultHandler(viewResolver, messageConverter, bufferSize));
        handlers.add(new ModelAndViewResultHandler(viewResolver, messageConverter, bufferSize));

        handlers.add(new ResponseBodyResultHandler(messageConverter));

        mvcConfiguration.configureResultHandler(handlers);

        ResultHandlers.addHandler(handlers);
    }

    protected TemplateViewResolver getTemplateViewResolver(final WebMvcConfiguration mvcConfiguration) {

        final WebApplicationContext applicationContext = obtainApplicationContext();
        TemplateViewResolver templateViewResolver = applicationContext.getBean(TemplateViewResolver.class);

        if (templateViewResolver == null) {
            applicationContext.registerBean(DefaultTemplateViewResolver.class.getName(), DefaultTemplateViewResolver.class);
            templateViewResolver = applicationContext.getBean(TemplateViewResolver.class);
        }

        configureTemplateViewResolver(templateViewResolver, mvcConfiguration);
        return templateViewResolver;
    }

    /**
     * @param templateResolver
     *            {@link TemplateViewResolver} object
     * @param mvcConfiguration
     *            All {@link WebMvcConfiguration} object
     */
    protected void configureTemplateViewResolver(TemplateViewResolver templateResolver, WebMvcConfiguration mvcConfiguration) {
        if (templateResolver instanceof AbstractTemplateViewResolver) {
            mvcConfiguration.configureTemplateViewResolver((AbstractTemplateViewResolver) templateResolver);
        }
    }

    /**
     * Configure {@link ParameterResolver}s to resolve handler method arguments
     * 
     * @param resolvers
     *            Resolvers registry
     * @param mvcConfiguration
     *            All {@link WebMvcConfiguration} object
     */
    protected void configureParameterResolver(List<ParameterResolver> resolvers, WebMvcConfiguration mvcConfiguration) {

        // Use ConverterParameterResolver to resolve primitive types
        // --------------------------------------------------------------------------

        resolvers.add(convert((m) -> m.is(String.class), (s) -> s));
        resolvers.add(convert((m) -> m.is(Long.class) || m.is(long.class), Long::parseLong));
        resolvers.add(convert((m) -> m.is(Integer.class) || m.is(int.class), Integer::parseInt));
        resolvers.add(convert((m) -> m.is(Short.class) || m.is(short.class), Short::parseShort));
        resolvers.add(convert((m) -> m.is(Float.class) || m.is(float.class), Float::parseFloat));
        resolvers.add(convert((m) -> m.is(Double.class) || m.is(double.class), Double::parseDouble));
        resolvers.add(convert((m) -> m.is(Boolean.class) || m.is(boolean.class), Boolean::parseBoolean));

        // For some useful context annotations @off
        // -------------------------------------------- 

        resolvers.add(delegate((m) -> m.isAnnotationPresent(RequestAttribute.class), //
              (ctx, m) -> ctx.attribute(m.getName())//
        ));

        resolvers.add(delegate((m) -> m.isAnnotationPresent(Value.class), //
              (ctx, m) -> resolveValue(m.getAnnotation(Value.class), m.getParameterClass())//
        ));
        resolvers.add(delegate((m) -> m.isAnnotationPresent(Env.class), //
              (ctx, m) -> resolveValue(m.getAnnotation(Env.class), m.getParameterClass())//
        ));

        final WebApplicationContext context = obtainApplicationContext();
        final Properties properties = context.getEnvironment().getProperties();

        resolvers.add(delegate((m) -> m.isAnnotationPresent(Props.class), //
               (ctx, m) -> resolveProps(m.getAnnotation(Props.class), m.getParameterClass(), properties)//
        ));

        resolvers.add(delegate((m) -> m.isAnnotationPresent(Autowired.class), //@off
              (ctx, m) -> {
                  final Autowired autowired = m.getAnnotation(Autowired.class);
                  final String name = autowired.value();

                  final Object bean;
                  if (StringUtils.isEmpty(name)) {
                      bean = context.getBean(m.getParameterClass());
                  }
                  else {
                      bean = context.getBean(name, m.getParameterClass());
                  }
                  if (bean == null && autowired.required()) {
                      throw new NoSuchBeanDefinitionException(m.getParameterClass());
                  }
                  return bean;
              }
        ));

        // HandlerMethod @on
        resolvers.add(delegate((m) -> m.is(HandlerMethod.class), (ctx, m) -> m.getHandlerMethod()));

        // For cookies
        // ------------------------------------------
        resolvers.add(new CookieParameterResolver());
        resolvers.add(new CookieParameterResolver.CookieArrayParameterResolver());
        resolvers.add(new CookieParameterResolver.CookieAnnotationParameterResolver());
        resolvers.add(new CookieParameterResolver.CookieCollectionParameterResolver());

        // For multipart
        // -------------------------------------------

        configureMultipart(resolvers, context.getBean(MultipartConfiguration.class), mvcConfiguration);

        // Header
        resolvers.add(new HeaderParameterResolver());

        resolvers.add(new MapParameterResolver());
        resolvers.add(new ModelParameterResolver());
        resolvers.add(new ArrayParameterResolver());
        resolvers.add(new StreamParameterResolver());

        final PathMatcher pathMatcher = context.getBean(HandlerMethodRegistry.class).getPathMatcher();
        
        resolvers.add(new PathVariableParameterResolver(pathMatcher));
        
        final MessageConverter messageConverter = context.getBean(MessageConverter.class);
        resolvers.add(new RequestBodyParameterResolver(messageConverter));
        resolvers.add(new ThrowableHandlerParameterResolver());

        resolvers.add(new CollectionParameterResolver());
        resolvers.add(new BeanParameterResolver());

        // User customize parameter resolver
        // ------------------------------------------

        mvcConfiguration.configureParameterResolver(resolvers); // user configure

        ParameterResolvers.addResolver(resolvers);
    }

    protected void configureMultipart(List<ParameterResolver> resolvers,
                                      MultipartConfiguration multipartConfiguration, WebMvcConfiguration mvcConfiguration) {

        Objects.requireNonNull(multipartConfiguration, "Multipart Config Can't be null");
        mvcConfiguration.configureMultipart(multipartConfiguration);
    }

    /**
     * @param registry
     * @param mvcConfiguration
     *            All {@link WebMvcConfiguration} object
     */
    protected void configureResourceHandler(ResourceHandlerRegistry registry, WebMvcConfiguration mvcConfiguration) {
        mvcConfiguration.configureResourceHandler(registry);
    }

    /**
     * Invoke all {@link WebApplicationInitializer}s
     * 
     * @param context
     *            {@link ApplicationContext} object
     * @throws Throwable
     *             If any initialize exception occurred
     */
    protected void initializerStartup(final WebApplicationContext context, //
                                      final WebMvcConfiguration mvcConfiguration) throws Throwable //
    {
        final List<WebApplicationInitializer> initializers = context.getBeans(WebApplicationInitializer.class);

        configureInitializer(initializers, mvcConfiguration);

        for (final WebApplicationInitializer initializer : initializers) {
            initializer.onStartup(context);
        }
    }

    /**
     * Configure {@link WebApplicationInitializer}
     * 
     * @param initializers
     *            {@link WebApplicationInitializer}s
     * @param config
     *            {@link CompositeWebMvcConfiguration}
     */
    protected void configureInitializer(List<WebApplicationInitializer> initializers, WebMvcConfiguration config) {

        config.configureInitializer(initializers);
        OrderUtils.reversedSort(initializers);
    }

    /**
     * Get {@link WebMvcConfiguration}
     * 
     * @param applicationContext
     *            {@link ApplicationContext} object
     */
    protected WebMvcConfiguration getWebMvcConfiguration(ApplicationContext applicationContext) {
        return new CompositeWebMvcConfiguration(applicationContext.getBeans(WebMvcConfiguration.class));
    }

    /**
     * Initialize framework.
     * 
     * @throws Throwable
     *             if any Throwable occurred
     */
    protected void initFrameWorkFromWebMvcXml() throws Throwable {

        // find the configure file
        log.info("TODAY WEB Framework Is Looking For Configuration File.");

        final String webMvcConfigLocation = getWebMvcConfigLocation();

        if (StringUtils.isEmpty(webMvcConfigLocation)) {
            log.info("Configuration File does not exist.");
            return;
        }

        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        factory.setIgnoringComments(true);

        final DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setEntityResolver((publicId, systemId) -> {
            if (systemId.contains(DTD_NAME) || publicId.contains(DTD_NAME)) {
                return new InputSource(new ByteArrayInputStream("<?xml version=\"1.0\" encoding=\"UTF-8\"?>".getBytes()));
            }
            return null;
        });
        final ViewConfiguration viewConfiguration = new ViewConfiguration(obtainApplicationContext());

        for (final String file : StringUtils.split(webMvcConfigLocation)) {

            final Resource resource = ResourceUtils.getResource(file);

            if (resource == null || !resource.exists()) {
                throw new ConfigurationException("Your Provided Configuration File: [" + file + "], Does Not Exist");
            }
            try (final InputStream inputStream = resource.getInputStream()) {
                registerFromXml(builder.parse(inputStream), resource.toString(), viewConfiguration);// fixed
            }
        }
    }

    protected String getWebMvcConfigLocation() throws Throwable {
        return obtainApplicationContext().getEnvironment().getProperty(WEB_MVC_CONFIG_LOCATION);
    }

    /**
     * configure with xml file
     * 
     * @param doc
     *            xml file
     * @param viewConfiguration
     * @throws Throwable
     */
    protected void registerFromXml(final Document doc, final String filePath, //
                                   final ViewConfiguration viewConfiguration) throws Throwable //
    {
        final Element root = doc.getDocumentElement();
        if (ROOT_ELEMENT.equals(root.getNodeName())) { // root element
            log.info("Found Configuration File: [{}].", filePath);
            configureStart(root, viewConfiguration);
        }
    }

    /**
     * Start configure.
     * 
     * @param root
     *            Root element
     */
    protected void configureStart(final Element root, final ViewConfiguration viewConfiguration) throws Throwable {

        final NodeList nl = root.getChildNodes();
        final int length = nl.getLength();

        for (int i = 0; i < length; i++) {
            final Node node = nl.item(i);
            if (node instanceof Element) {
                final Element ele = (Element) node;
                final String nodeName = ele.getNodeName();

                log.debug("Found Element: [{}]", nodeName);

                if (ELEMENT_CONTROLLER.equals(nodeName)) {
                    viewConfiguration.configuration(ele);
                } // ELEMENT_RESOURCES // TODO
                else {
                    log.warn("This This element: [{}] is not supported in this version: [{}].", nodeName, Constant.WEB_VERSION);
                }
            }
        }
    }

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
    protected Class<?> registerResolver(Element element, Class<?> defaultClass, String name, boolean refresh) //
            throws ClassNotFoundException, BeanDefinitionStoreException //
    {
        String attrClass = element.getAttribute(ATTR_CLASS); // class="cn.taketoday..."

        Class<?> resolverClass = null;
        if (defaultClass.getName().equals(attrClass)) { // Custom
            resolverClass = defaultClass; // default
        }
        else {
            resolverClass = ClassUtils.forName(attrClass);
        }
        // register resolver
        final WebApplicationContext webApplicationContext = obtainApplicationContext();
        webApplicationContext.registerBean(name, resolverClass);
        log.info("Register [{}] onto [{}]", name, resolverClass.getName());

        if (refresh) {
            webApplicationContext.refresh(name);
        }
        return resolverClass;
    }

    /**
     * Check resolvers
     */
    protected void checkFrameWorkResolvers(WebApplicationContext applicationContext) {}

}
