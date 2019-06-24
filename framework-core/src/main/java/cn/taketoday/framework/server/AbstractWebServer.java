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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.framework.server;

import java.io.File;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.MultipartConfigElement;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletSecurityElement;
import javax.servlet.SessionCookieConfig;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.ServletSecurity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.taketoday.context.Ordered;
import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.env.ConfigurableEnvironment;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.OrderUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.framework.Constant;
import cn.taketoday.framework.WebServerApplicationContext;
import cn.taketoday.framework.annotation.Starter;
import cn.taketoday.framework.aware.WebServerApplicationContextAware;
import cn.taketoday.framework.bean.ErrorPage;
import cn.taketoday.framework.bean.MimeMappings;
import cn.taketoday.framework.config.CompositeWebApplicationConfiguration;
import cn.taketoday.framework.config.CompressionConfiguration;
import cn.taketoday.framework.config.DefaultServletConfiguration;
import cn.taketoday.framework.config.JspServletConfiguration;
import cn.taketoday.framework.config.SessionConfiguration;
import cn.taketoday.framework.config.SessionCookieConfiguration;
import cn.taketoday.framework.config.WebApplicationConfiguration;
import cn.taketoday.framework.config.WebDocumentConfiguration;
import cn.taketoday.framework.utils.ApplicationUtils;
import cn.taketoday.web.ServletContextInitializer;
import cn.taketoday.web.config.initializer.OrderedInitializer;
import cn.taketoday.web.config.initializer.WebServletInitializer;
import lombok.Getter;
import lombok.Setter;

/**
 * @author TODAY <br>
 *         2019-01-26 11:08
 */
@Getter
@Setter
public abstract class AbstractWebServer implements //
        ConfigurableWebServer, WebServerApplicationContextAware {

    private static final Logger log = LoggerFactory.getLogger(AbstractWebServer.class);

    private int port = 8080;
    private String host = "localhost";
    private String contextPath = "";
    private String serverHeader = null;
    private boolean enableHttp2 = false;

    private String displayName = "Web-App";

    private String deployName = "deploy-web-app";

    /** Application Class */
    private Class<?> startupClass;

    @Autowired
    private CompressionConfiguration compression;

    @Autowired
    private SessionConfiguration sessionConfiguration;

    @Autowired(required = false)
    private JspServletConfiguration jspServletConfiguration;

    @Autowired(required = false)
    private DefaultServletConfiguration defaultServletConfiguration;

    private Set<ErrorPage> errorPages = new LinkedHashSet<>();

    private Set<String> welcomePages = new LinkedHashSet<>();

    private Map<Locale, Charset> localeCharsetMappings = new HashMap<>();
    private List<ServletContextInitializer> initializers = new ArrayList<>();
    private final MimeMappings mimeMappings = new MimeMappings(MimeMappings.DEFAULT);

    @Autowired
    private WebDocumentConfiguration webDocumentConfiguration;

    /**
     * Context init parameters
     */
    private final Map<String, String> contextInitParameters = new HashMap<>();

    protected WebServerApplicationContext applicationContext;

    private AtomicBoolean started = new AtomicBoolean(false);

    private CompositeWebApplicationConfiguration webApplicationConfiguration;

    @Override
    public void initialize(ServletContextInitializer... contextInitializers) throws Throwable {

        // prepare initialize
        prepareInitialize();
        // initialize server context
        initializeContext(contextInitializers);
        // context initialized
        contextInitialized();
        // finish initialized
        finishInitialize();
    }

    /**
     * Context Initialized
     * 
     * @throws Throwable
     */
    protected void contextInitialized() throws Throwable {

    }

    /**
     * Finish initialized
     */
    protected void finishInitialize() {

    }

    /**
     * Add jsp to context
     * 
     * @throws Throwable
     */
    protected void addJspServlet() throws Throwable {

        final JspServletConfiguration jspServletConfiguration = this.jspServletConfiguration;

        if (jspServletConfiguration == null) {
            return;
        }
        // config jsp servlet
        getWebApplicationConfiguration().configureJspServlet(jspServletConfiguration);

        if (jspServletConfiguration.isEnable()) {

            final Servlet jspServlet = ClassUtils.newInstance(jspServletConfiguration.getClassName());

            if (jspServlet != null) {

                log.info("Jsp is enabled, use jsp servlet: [{}]", jspServlet.getServletInfo());

                WebServletInitializer<Servlet> initializer = new WebServletInitializer<>(jspServlet);

                initializer.setName("jsp");
                initializer.setOrder(Ordered.HIGHEST_PRECEDENCE);
                initializer.addUrlMappings(jspServletConfiguration.getUrlMappings());
                initializer.setInitParameters(jspServletConfiguration.getInitParameters());

                initializers.add(initializer);
            }
        }
    }

    /**
     * Add default servlet
     */
    protected void addDefaultServlet() {

        final DefaultServletConfiguration defaultServletConfiguration = this.defaultServletConfiguration;

        if (defaultServletConfiguration == null) {
            return;
        }
        // config default servlet
        getWebApplicationConfiguration().configureDefaultServlet(defaultServletConfiguration);

        if (defaultServletConfiguration.isEnable()) {

            final Servlet defaultServlet = getDefaultServlet();
            if (defaultServlet != null) {

                log.info("Default servlet is enabled, use servlet: [{}]", defaultServlet.getServletInfo());

                WebServletInitializer<Servlet> initializer = new WebServletInitializer<>(defaultServlet);

                initializer.setName(Constant.DEFAULT);
                initializer.setOrder(Ordered.HIGHEST_PRECEDENCE);
                initializer.addUrlMappings(defaultServletConfiguration.getUrlMappings());
                initializer.setInitParameters(defaultServletConfiguration.getInitParameters());

                initializers.add(initializer);
            }
        }
    }

    protected ServletContextInitializer[] getAllInitializers(ServletContextInitializer... initializers) {

        final List<ServletContextInitializer> mergedInitializers = new ArrayList<>();

        mergedInitializers.add((servletContext) -> this.contextInitParameters.forEach(servletContext::setInitParameter));

        mergedInitializers.addAll(Arrays.asList(initializers));
        mergedInitializers.addAll(this.initializers);

        mergedInitializers.add(new OrderedInitializer() {
            @Override
            public void onStartup(ServletContext servletContext) throws Throwable {

                getWebApplicationConfiguration().configureSession(sessionConfiguration);

                final SessionCookieConfiguration cookie = sessionConfiguration.getCookieConfiguration();
                final SessionCookieConfig config = servletContext.getSessionCookieConfig();

                config.setName(cookie.getName());
                config.setPath(cookie.getPath());
                config.setSecure(cookie.isSecure());
                config.setDomain(cookie.getDomain());
                config.setComment(cookie.getComment());
                config.setHttpOnly(cookie.isHttpOnly());

                config.setMaxAge((int) cookie.getMaxAge().getSeconds());

                if (sessionConfiguration.getTrackingModes() != null) {
                    servletContext.setSessionTrackingModes(new HashSet<>(Arrays.asList(sessionConfiguration.getTrackingModes())));
                }
            }
        });
        // config initializers
        getWebApplicationConfiguration().configureServletContextInitializer(mergedInitializers);

        OrderUtils.reversedSort(mergedInitializers);

        return mergedInitializers.toArray(new ServletContextInitializer[0]);
    }

    /**
     * Get Default Servlet Instance
     * 
     * @return
     */
    protected abstract Servlet getDefaultServlet();

    /**
     * @throws Throwable
     */
    protected void prepareInitialize() throws Throwable {

        WebServerApplicationContext applicationContext = getApplicationContext();

        final Class<?> startupClass = applicationContext.getStartupClass();
        final Starter starter = startupClass.getAnnotation(Starter.class);

        final ConfigurableEnvironment environment = applicationContext.getEnvironment();

        environment.setProperty(Constant.ENABLE_WEB_STARTED_LOG, "false");
        String webMvcConfigLocation = environment.getProperty(Constant.WEB_MVC_CONFIG_LOCATION);
        if (StringUtils.isNotEmpty(webMvcConfigLocation)) {
            environment.setProperty(Constant.ENABLE_WEB_MVC_XML, "true");
        }
        if (starter != null) {

            if (StringUtils.isEmpty(webMvcConfigLocation)) {
                webMvcConfigLocation = starter.webMvcConfigLocation();
                if (StringUtils.isNotEmpty(webMvcConfigLocation)) {
                    environment.setProperty(Constant.ENABLE_WEB_MVC_XML, "true");
                    environment.setProperty(Constant.WEB_MVC_CONFIG_LOCATION, webMvcConfigLocation);
                }
            }
        }
        MultipartConfig multipartConfig = startupClass.getAnnotation(MultipartConfig.class);
        if (multipartConfig != null) {

            if (applicationContext.containsBeanDefinition(Constant.MULTIPART_CONFIG_ELEMENT)) {
                log.info("Multiple: [{}] Overriding its bean definition", MultipartConfigElement.class.getName());
            }
            applicationContext.registerSingleton(Constant.MULTIPART_CONFIG_ELEMENT, //
                    new MultipartConfigElement(multipartConfig));

            applicationContext.registerBean(Constant.MULTIPART_CONFIG_ELEMENT, MultipartConfigElement.class);
        }

        ServletSecurity servletSecurity = startupClass.getAnnotation(ServletSecurity.class);
        if (servletSecurity != null) {

            if (applicationContext.containsBeanDefinition(Constant.SERVLET_SECURITY_ELEMENT)) {
                log.info("Multiple: [{}] Overriding its bean definition", ServletSecurityElement.class.getName());
            }

            applicationContext.registerSingleton(Constant.SERVLET_SECURITY_ELEMENT, //
                    new ServletSecurityElement(servletSecurity));

            applicationContext.registerBean(Constant.SERVLET_SECURITY_ELEMENT, ServletSecurityElement.class);
        }

        addDefaultServlet();

        addJspServlet();
    }

    /**
     * Prepare {@link ServletContext}
     * 
     * @param contextInitializers
     * @throws Throwable
     */
    protected void initializeContext(ServletContextInitializer... contextInitializers) throws Throwable {

    }

    protected boolean isZeroOrLess(Duration sessionTimeout) {
        return sessionTimeout == null || sessionTimeout.isNegative() || sessionTimeout.isZero();
    }

    @Override
    public void setWebServerApplicationContext(WebServerApplicationContext applicationContext) {
        this.applicationContext = applicationContext;

        if (startupClass == null) {
            startupClass = applicationContext.getStartupClass();
        }

        final List<WebApplicationConfiguration> webApplicationConfigurations = //
                applicationContext.getBeans(WebApplicationConfiguration.class);

        OrderUtils.reversedSort(webApplicationConfigurations);

        this.webApplicationConfiguration = //
                new CompositeWebApplicationConfiguration(webApplicationConfigurations);
    }

    /**
     * Get base temporal directory
     * 
     * @return base temporal directory
     */
    protected File getTemporalDirectory() {
        return getTemporalDirectory(null);
    }

    /**
     * Get a temporal directory with sub directory
     * 
     * @return temporal directory with sub directory
     */
    protected File getTemporalDirectory(String dir) {
        return ApplicationUtils.getTemporalDirectory(startupClass, dir);
    }

}
