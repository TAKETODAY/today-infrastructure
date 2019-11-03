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
package cn.taketoday.web.servlet;

import java.io.File;
import java.io.FileFilter;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.EventListener;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.MultipartConfigElement;
import javax.servlet.Servlet;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletSecurityElement;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebListener;
import javax.servlet.annotation.WebServlet;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.context.utils.ExceptionUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.Constant;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.config.WebApplicationInitializer;
import cn.taketoday.web.config.WebApplicationLoader;
import cn.taketoday.web.config.WebMvcConfiguration;
import cn.taketoday.web.event.WebApplicationFailedEvent;
import cn.taketoday.web.mapping.ResourceMapping;
import cn.taketoday.web.mapping.ResourceMappingRegistry;
import cn.taketoday.web.resolver.method.ParameterResolver;
import cn.taketoday.web.resolver.method.ServletParameterResolver;
import cn.taketoday.web.servlet.initializer.WebFilterInitializer;
import cn.taketoday.web.servlet.initializer.WebListenerInitializer;
import cn.taketoday.web.servlet.initializer.WebServletInitializer;
import cn.taketoday.web.view.FreeMarkerViewResolver;
import cn.taketoday.web.view.ViewResolver;

/**
 * Initialize Web application in a server like tomcat, jetty, undertow
 * 
 * @author TODAY <br>
 *         2019-01-12 17:28
 */
@SuppressWarnings("serial")
public class WebServletApplicationLoader extends WebApplicationLoader implements ServletContainerInitializer {

    private static final Logger log = LoggerFactory.getLogger(WebServletApplicationLoader.class);

    @Override
    protected ServletWebMvcConfiguration getWebMvcConfiguration(ApplicationContext applicationContext) {
        return new ServletCompositeWebMvcConfiguration(applicationContext.getBeans(WebMvcConfiguration.class));
    }

    @Override
    public WebServletApplicationContext getWebApplicationContext() {
        return (WebServletApplicationContext) super.getWebApplicationContext();
    }

    @Override
    protected String getWebMvcConfigLocation() throws Throwable {
        String webMvcConfigLocation = super.getWebMvcConfigLocation();

        if (StringUtils.isEmpty(webMvcConfigLocation)) {
            webMvcConfigLocation = getServletContext().getInitParameter(WEB_MVC_CONFIG_LOCATION);
        }

        if (StringUtils.isEmpty(webMvcConfigLocation)) { // scan from '/'
            final String rootPath = getServletContext().getRealPath("/");

            final HashSet<String> paths = new HashSet<>();

            final File dir = new File(rootPath);
            if (dir.exists()) {
                log.trace("Finding Configuration File From Root Path: [{}]", rootPath);

                scanXml(dir, paths, (path -> (path.isDirectory() || path.getName().endsWith(".xml"))));
                return StringUtils.arrayToString(paths.toArray(Constant.EMPTY_STRING_ARRAY));
            }
            return null;
        }
        return webMvcConfigLocation;
    }

    /**
     * @return {@link ServletContext} or null if {@link ApplicationContext} not
     *         initialize
     */
    protected ServletContext getServletContext() {
        return getWebApplicationContext().getServletContext();
    }

    /**
     * Find configuration file.
     * 
     * @param dir
     *            directory
     * @throws Throwable
     */
    protected void scanXml(final File dir, final Set<String> files, FileFilter filter) throws Throwable {

        log.trace("Enter [{}]", dir.getAbsolutePath());

        final File[] listFiles = dir.listFiles(filter);
        if (listFiles == null) {
            log.error("File: [{}] Does not exist", dir);
            return;
        }
        for (final File file : listFiles) {
            if (file.isDirectory()) { // recursive
                scanXml(file, files, filter);
            }
            else {
                files.add(file.getAbsolutePath());
            }
        }
    }

    /**
     * Prepare {@link WebServletApplicationContext}
     * 
     * @param servletContext
     *            {@link ServletContext}
     * @return {@link WebServletApplicationContext}
     */
    protected WebServletApplicationContext prepareApplicationContext(ServletContext servletContext) {

        WebServletApplicationContext ret = getWebApplicationContext();

        if (ret == null) {

            final long startupDate = System.currentTimeMillis();
            log.info("Your application starts to be initialized at: [{}].", //
                     new SimpleDateFormat(Constant.DEFAULT_DATE_FORMAT).format(startupDate));

            ret = new StandardWebServletApplicationContext();

            ((ConfigurableWebServletApplicationContext) ret).setServletContext(servletContext);
            ret.loadContext(Constant.BLANK);
        }
        else if (ret instanceof ConfigurableWebServletApplicationContext && ret.getServletContext() == null) {
            ((ConfigurableWebServletApplicationContext) ret).setServletContext(servletContext);
            log.info("ServletContext: [{}] Configure Success.", servletContext);
        }
        return ret;
    }

    @Override
    public void onStartup(Set<Class<?>> classes, ServletContext servletContext) throws ServletException {

        Objects.requireNonNull(servletContext, "ServletContext can't be null");

        final WebApplicationContext context = prepareApplicationContext(servletContext);
        try {

            try {
                servletContext.setRequestCharacterEncoding(DEFAULT_ENCODING);
                servletContext.setResponseCharacterEncoding(DEFAULT_ENCODING);
            }
            catch (Throwable e) {} // Waiting for Jetty 10.0.0

            onStartup(context);
        }
        catch (Throwable ex) {
            ex = ExceptionUtils.unwrapThrowable(ex);
            context.publishEvent(new WebApplicationFailedEvent(context, ex));
            log.error("Your Application Initialized ERROR: [{}]", ex.toString(), ex);
            throw new ConfigurationException(ex);
        }
    }

    @Override
    protected void configureParameterResolver(List<ParameterResolver> resolvers, WebMvcConfiguration mvcConfiguration) {

        // Servlet cookies parameter
        // ----------------------------

        resolvers.add(new ServletParameterResolver.ServletCookieParameterResolver());
        resolvers.add(new ServletParameterResolver.ServletCookieArrayParameterResolver());
        resolvers.add(new ServletParameterResolver.ServletCookieCollectionParameterResolver());
        resolvers.add(new ServletParameterResolver.ServletContextAttributeParameterResolver());

        // Servlet components parameter
        // ----------------------------

        resolvers.add(new ServletParameterResolver.HttpSessionParameterResolver());
        resolvers.add(new ServletParameterResolver.ServletContextParameterResolver());
        resolvers.add(new ServletParameterResolver.ServletRequestParameterResolver());
        resolvers.add(new ServletParameterResolver.ServletResponseParameterResolver());

        // Attributes
        // ------------------------

        resolvers.add(new ServletParameterResolver.HttpSessionAttributeParameterResolver());
        resolvers.add(new ServletParameterResolver.ServletContextAttributeParameterResolver());

        super.configureParameterResolver(resolvers, mvcConfiguration);
    }

    @Override
    protected void checkFrameWorkResolvers(WebApplicationContext applicationContext) {

        if (!applicationContext.containsBeanDefinition(ViewResolver.class)) {
            // use freemarker view resolver
            applicationContext.registerBean(VIEW_RESOLVER, FreeMarkerViewResolver.class);
            applicationContext.refresh(VIEW_RESOLVER);
            log.info("Use default view resolver: [{}].", FreeMarkerViewResolver.class);
        }

        super.checkFrameWorkResolvers(applicationContext);
    }

    @Override
    protected void configureInitializer(List<WebApplicationInitializer> initializers, WebMvcConfiguration config) {

        final WebServletApplicationContext webApplicationContext = getWebApplicationContext();
        configureResourceRegistry(initializers, getWebMvcConfiguration(webApplicationContext));

        configureFilter(webApplicationContext, initializers);
        configureServlet(webApplicationContext, initializers);
        configureListener(webApplicationContext, initializers);

        super.configureInitializer(initializers, config);
    }

    /**
     * Configure ResourceMapping
     * 
     * @param contextInitializers
     *            All {@link WebApplicationInitializer}s
     * @param servletWebMvcConfiguration
     *            ServletWebMvcConfiguration
     */
    protected void configureResourceRegistry(List<WebApplicationInitializer> contextInitializers,
                                             ServletWebMvcConfiguration servletWebMvcConfiguration)//
    {
        final WebServletApplicationContext context = getWebApplicationContext();

        final Set<String> urlMappings = new HashSet<>();
        final ResourceMappingRegistry resourceMappingRegistry = context.getBean(ResourceMappingRegistry.class);
        final List<ResourceMapping> resourceHandlerMappings = resourceMappingRegistry.getResourceMappings();

        for (final ResourceMapping resourceMapping : resourceHandlerMappings) {
            final String[] pathPatterns = resourceMapping.getPathPatterns();
            for (final String pathPattern : pathPatterns) {
                if (pathPattern.endsWith("/**")) {
                    urlMappings.add(pathPattern.substring(0, pathPattern.length() - 1));
                }
                else {
                    urlMappings.add(pathPattern);
                }
            }
        }
        servletWebMvcConfiguration.configureResourceServletUrlMappings(urlMappings);

        if (!urlMappings.isEmpty()) {

            if (!context.containsBeanDefinition(ResourceServlet.class)) {
                context.registerBean(Constant.RESOURCE_SERVLET, ResourceServlet.class);
            }

            WebServletInitializer<ResourceServlet> resource = new WebServletInitializer<>(context.getBean(ResourceServlet.class));
            resource.setUrlMappings(urlMappings);
            resource.setName(Constant.RESOURCE_SERVLET);

            log.info("Set ResourceServlet Url Mappings: [{}]", urlMappings);
            contextInitializers.add(resource);
        }
    }

    /**
     * Configure {@link Filter}
     * 
     * @param applicationContext
     *            {@link ApplicationContext}
     * @param contextInitializers
     *            {@link WebApplicationInitializer}s
     */
    protected void configureFilter(final WebApplicationContext applicationContext, //
                                   final List<WebApplicationInitializer> contextInitializers) //
    {

        List<Filter> filters = applicationContext.getAnnotatedBeans(WebFilter.class);
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

    /**
     * Configure {@link Servlet}
     * 
     * @param applicationContext
     *            {@link ApplicationContext}
     * @param contextInitializers
     *            {@link WebApplicationInitializer}s
     */
    protected void configureServlet(final WebApplicationContext applicationContext,
                                    final List<WebApplicationInitializer> contextInitializers) //
    {

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

    /**
     * Configure listeners
     * 
     * @param applicationContext
     *            {@link ApplicationContext}
     * @param contextInitializers
     *            {@link WebApplicationInitializer}s
     */
    protected void configureListener(final WebApplicationContext applicationContext,
                                     final List<WebApplicationInitializer> contextInitializers)//
    {
        Collection<EventListener> eventListeners = applicationContext.getAnnotatedBeans(WebListener.class);
        for (EventListener eventListener : eventListeners) {
            contextInitializers.add(new WebListenerInitializer<>(eventListener));
        }
    }

    /**
     * @author TODAY <br>
     *         2019-05-17 17:46
     */
    public static class ServletCompositeWebMvcConfiguration //
            extends CompositeWebMvcConfiguration implements ServletWebMvcConfiguration {

        public ServletCompositeWebMvcConfiguration(List<WebMvcConfiguration> webMvcConfigurations) {
            super(webMvcConfigurations);
        }

        @Override
        public void configureResourceServletUrlMappings(Set<String> urlMappings) {
            for (WebMvcConfiguration webMvcConfiguration : getWebMvcConfigurations()) {
                if (webMvcConfiguration instanceof ServletWebMvcConfiguration) {
                    ((ServletWebMvcConfiguration) webMvcConfiguration).configureResourceServletUrlMappings(urlMappings);
                }
            }
        }

    }
}
