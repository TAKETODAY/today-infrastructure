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

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.MultipartConfigElement;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletSecurityElement;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.ServletSecurity;

import cn.taketoday.context.Ordered;
import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.framework.Constant;
import cn.taketoday.framework.ServletWebServerApplicationContext;
import cn.taketoday.framework.WebServerApplicationContext;
import cn.taketoday.framework.config.DefaultServletConfiguration;
import cn.taketoday.framework.config.JspServletConfiguration;
import cn.taketoday.framework.config.SessionConfiguration;
import cn.taketoday.framework.config.SessionCookieConfiguration;
import cn.taketoday.web.config.WebApplicationInitializer;
import cn.taketoday.web.servlet.initializer.OrderedServletContextInitializer;
import cn.taketoday.web.servlet.initializer.WebServletInitializer;
import lombok.Getter;
import lombok.Setter;

/**
 * @author TODAY <br>
 *         2019-01-26 11:08
 */
@Getter
@Setter
public abstract class AbstractServletWebServer extends AbstractWebServer implements ConfigurableWebServer {

    private static final Logger log = LoggerFactory.getLogger(AbstractServletWebServer.class);

    private int port = 8080;
    private String host = "localhost";
    private String contextPath = "";
    private String serverHeader = null;
    private boolean enableHttp2 = false;

    private String displayName = "Web-App";

    private String deployName = "deploy-web-app";
    @Autowired
    private SessionConfiguration sessionConfiguration;

    @Autowired(required = false)
    private JspServletConfiguration jspServletConfiguration;

    @Autowired(required = false)
    private DefaultServletConfiguration defaultServletConfiguration;

    private Map<Locale, Charset> localeCharsetMappings = new HashMap<>();

    /**
     * Context init parameters
     */
    private final Map<String, String> contextInitParameters = new HashMap<>();

    @Override
    protected abstract ServletWebServerApplicationContext getApplicationContext();

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

                initializer.setName(jspServletConfiguration.getName());
                initializer.setOrder(Ordered.HIGHEST_PRECEDENCE);
                initializer.addUrlMappings(jspServletConfiguration.getUrlMappings());
                initializer.setInitParameters(jspServletConfiguration.getInitParameters());

                getContextInitializers().add(initializer);
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

                getContextInitializers().add(initializer);
            }
        }
    }

    @Override
    protected List<WebApplicationInitializer> getMergedInitializers() {

        final List<WebApplicationInitializer> contextInitializers = getContextInitializers();

        contextInitializers.add(new OrderedServletContextInitializer() {
            @Override
            public void onStartup(ServletContext servletContext) throws Throwable {
                getContextInitParameters().forEach(servletContext::setInitParameter);
            }
        });

        contextInitializers.add(new OrderedServletContextInitializer() {

            @Override
            public void onStartup(ServletContext servletContext) throws Throwable {

                final SessionConfiguration sessionConfiguration = getSessionConfiguration();
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

                    final Set<SessionTrackingMode> collect = Arrays.asList(sessionConfiguration.getTrackingModes())
                            .stream()
                            .map(t -> t.name())
                            .map(SessionTrackingMode::valueOf)
                            .collect(Collectors.toSet());

                    servletContext.setSessionTrackingModes(collect);
                }
            }
        });

        return contextInitializers;
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
    @Override
    protected void prepareInitialize() throws Throwable {

        super.prepareInitialize();

        final WebServerApplicationContext applicationContext = getApplicationContext();

        final Class<?> startupClass = applicationContext.getStartupClass();

        MultipartConfig multipartConfig = startupClass.getAnnotation(MultipartConfig.class);
        if (multipartConfig != null) {

            if (applicationContext.containsBeanDefinition(MultipartConfigElement.class)) {
                log.info("Multiple: [{}] Overriding its bean definition", MultipartConfigElement.class.getName());
            }
            applicationContext.registerSingleton(new MultipartConfigElement(multipartConfig));
            applicationContext.registerBean("multipartConfigElement", MultipartConfigElement.class);
        }

        ServletSecurity servletSecurity = startupClass.getAnnotation(ServletSecurity.class);
        if (servletSecurity != null) {

            if (applicationContext.containsBeanDefinition(ServletSecurityElement.class)) {
                log.info("Multiple: [{}] Overriding its bean definition", ServletSecurityElement.class.getName());
            }

            applicationContext.registerSingleton(new ServletSecurityElement(servletSecurity));
            applicationContext.registerBean("servletSecurityElement", ServletSecurityElement.class);
        }

        addDefaultServlet();

        addJspServlet();
    }

}
