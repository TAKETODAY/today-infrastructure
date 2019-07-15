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
import javax.servlet.ServletRegistration;
import javax.servlet.ServletSecurityElement;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebListener;
import javax.servlet.annotation.WebServlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.taketoday.context.exception.ConfigurationException;
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
import cn.taketoday.web.servlet.initializer.WebFilterInitializer;
import cn.taketoday.web.servlet.initializer.WebListenerInitializer;
import cn.taketoday.web.servlet.initializer.WebServletInitializer;
import cn.taketoday.web.utils.WebUtils;

/**
 * Initialize Web application in a server like tomcat, jetty, undertow
 * 
 * @author TODAY <br>
 *         2019-01-12 17:28
 */
@SuppressWarnings("serial")
public class WebServletApplicationLoader extends WebApplicationLoader implements ServletContainerInitializer {

    private static final Logger log = LoggerFactory.getLogger(WebServletApplicationLoader.class);

    private ServletContext servletContext;

    @Override
    protected ServletWebMvcConfiguration getWebMvcConfiguration() {
        return new ServletCompositeWebMvcConfiguration(applicationContext.getBeans(WebMvcConfiguration.class));
    }

    @Override
    protected String getWebMvcConfigLocation() throws Throwable {
        String webMvcConfigLocation = super.getWebMvcConfigLocation();

        if (StringUtils.isEmpty(webMvcConfigLocation)) {
            webMvcConfigLocation = servletContext.getInitParameter(WEB_MVC_CONFIG_LOCATION);
        }

        if (StringUtils.isEmpty(webMvcConfigLocation)) {
            final String rootPath = servletContext.getRealPath("/");
            log.debug("Finding Configuration File From Root Path: [{}]", rootPath);
            findConfiguration(new File(rootPath));
            return null;
        }
        return webMvcConfigLocation;
    }

    /**
     * Prepare {@link WebServletApplicationContext}
     * 
     * @param classes
     *            classes to scan
     * @param servletContext
     *            {@link ServletContext}
     * @return startup Date
     */
    protected WebApplicationContext prepareApplicationContext() {

        final Object attribute = servletContext.getAttribute(KEY_WEB_APPLICATION_CONTEXT);
        if (attribute instanceof WebServletApplicationContext) {
            return applicationContext = (WebServletApplicationContext) attribute;
        }

        final long startupDate = System.currentTimeMillis();
        log.info("Your application starts to be initialized at: [{}].", //
                new SimpleDateFormat(Constant.DEFAULT_DATE_FORMAT).format(startupDate));

        // fix: applicationContext NullPointerException
        WebServletApplicationContext applicationContext = new StandardWebServletApplicationContext();
        WebUtils.setWebApplicationContext(applicationContext);

        applicationContext.setServletContext(servletContext);
        applicationContext.loadContext(Constant.BLANK);

        return WebApplicationLoader.applicationContext = applicationContext;
    }

    @Override
    public void onStartup(Set<Class<?>> classes, ServletContext servletContext) throws ServletException {

        this.servletContext = Objects.requireNonNull(servletContext, "ServletContext can't be null");

        final WebApplicationContext applicationContext = prepareApplicationContext();
        try {

            try {
                servletContext.setRequestCharacterEncoding(DEFAULT_ENCODING);
                servletContext.setResponseCharacterEncoding(DEFAULT_ENCODING);
            }
            catch (Throwable e) {
                // Waiting for Jetty 10.0.0
            }

            onStartup(applicationContext);
        }
        catch (Throwable ex) {
            ex = ExceptionUtils.unwrapThrowable(ex);
            applicationContext.publishEvent(new WebApplicationFailedEvent(applicationContext, ex));
            log.error("Your Application Initialized ERROR: [{}]", ex.getMessage(), ex);
            throw new ConfigurationException(ex);
        }
    }

    @Override
    protected void configureParameterResolver(List<ParameterResolver> parameterResolvers, WebMvcConfiguration mvcConfiguration) {

//        parameterResolvers.add(new ParameterResolver() {
//            @Override
//            public boolean supports(MethodParameter parameter) {
//                return parameter.isAnnotationPresent(Session.class);
//            }
//
//            @Override
//            public Object resolveParameter(RequestContext requestContext, MethodParameter parameter) throws Throwable {
//                return requestContext.nativeSession(HttpSession.class).getAttribute(parameter.getName());
//            }
//        });
//        

        super.configureParameterResolver(parameterResolvers, mvcConfiguration);
    }

    @Override
    protected List<WebApplicationInitializer> getInitializers(WebApplicationContext applicationContext) {
        final List<WebApplicationInitializer> contextInitializers = super.getInitializers(applicationContext);

        configureResourceRegistry(contextInitializers, getWebMvcConfiguration());

        applyFilter(applicationContext, contextInitializers);
        applyServlet(applicationContext, contextInitializers);
        applyListener(applicationContext, contextInitializers);

        return contextInitializers;
    }

    protected void configureResourceRegistry(List<WebApplicationInitializer> contextInitializers, //
            ServletWebMvcConfiguration configuration)//
    {

        if (!applicationContext.containsBeanDefinition(ResourceServlet.class)) {
            applicationContext.registerBean(Constant.RESOURCE_SERVLET, ResourceServlet.class);
        }

        final ResourceServlet resourceServlet = applicationContext.getBean(ResourceServlet.class);

        WebServletInitializer<ResourceServlet> resourceServletInitializer = new WebServletInitializer<>(resourceServlet);

        final Set<String> urlMappings = new HashSet<>();
        final ResourceMappingRegistry resourceMappingRegistry = applicationContext.getBean(ResourceMappingRegistry.class);
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

        configuration.configureResourceServletUrlMappings(urlMappings);
        resourceServletInitializer.setUrlMappings(urlMappings);
        resourceServletInitializer.setName(Constant.RESOURCE_SERVLET);

        log.info("Set ResourceServlet Url Mappings: [{}]", urlMappings);
        contextInitializers.add(resourceServletInitializer);
    }

    private void applyFilter(final WebApplicationContext applicationContext, Collection<WebApplicationInitializer> contextInitializers) {

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

    private void applyServlet(final WebApplicationContext applicationContext,
            Collection<WebApplicationInitializer> contextInitializers) //
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

    private void applyListener(final WebApplicationContext applicationContext,
            Collection<WebApplicationInitializer> contextInitializers)//
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
    protected class ServletCompositeWebMvcConfiguration //
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

        @Override
        public void configureDefaultServlet(ServletRegistration servletRegistration) {
            for (WebMvcConfiguration webMvcConfiguration : getWebMvcConfigurations()) {
                if (webMvcConfiguration instanceof ServletWebMvcConfiguration) {
                    ((ServletWebMvcConfiguration) webMvcConfiguration).configureDefaultServlet(servletRegistration);
                }
            }
        }

    }
}
