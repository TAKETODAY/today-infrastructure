/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletSecurityElement;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.annotation.ServletSecurity;

import cn.taketoday.context.Ordered;
import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.framework.Constant;
import cn.taketoday.framework.WebServerApplicationContext;
import cn.taketoday.framework.config.DefaultServletConfiguration;
import cn.taketoday.framework.config.JspServletConfiguration;
import cn.taketoday.web.config.WebApplicationInitializer;
import cn.taketoday.web.servlet.initializer.OrderedServletContextInitializer;
import cn.taketoday.web.servlet.initializer.WebServletInitializer;
import cn.taketoday.web.session.SessionConfiguration;
import cn.taketoday.web.session.SessionCookieConfiguration;
import lombok.Getter;
import lombok.Setter;

/**
 * @author TODAY <br>
 * 2019-01-26 11:08
 */
@Getter
@Setter
public abstract class AbstractServletWebServer
        extends AbstractWebServer implements ConfigurableWebServer {

  @Autowired(required = false)
  private JspServletConfiguration jspServletConfiguration;
  @Autowired(required = false)
  private DefaultServletConfiguration defaultServletConfiguration;

  private Map<Locale, Charset> localeCharsetMappings = new HashMap<>();

  /**
   * Context init parameters
   */
  private final Map<String, String> contextInitParameters = new HashMap<>();

  /**
   * Add jsp to context
   */
  protected void addJspServlet() {
    final JspServletConfiguration jspServletConfiguration = this.jspServletConfiguration;
    if (jspServletConfiguration != null) {
      // config jsp servlet
      getWebApplicationConfiguration().configureJspServlet(jspServletConfiguration);
      if (jspServletConfiguration.isEnable()) {
        try {
          Servlet jspServlet = ClassUtils.newInstance(jspServletConfiguration.getClassName());
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
        catch (ClassNotFoundException e) {
          throw new ConfigurationException(e);
        }
      }
    }
  }

  /**
   * Add default servlet
   */
  protected void addDefaultServlet() {
    final DefaultServletConfiguration servletConfiguration = this.defaultServletConfiguration;
    if (servletConfiguration != null) {

      // config default servlet
      getWebApplicationConfiguration().configureDefaultServlet(servletConfiguration);
      if (servletConfiguration.isEnable()) {
        Servlet defaultServlet = getDefaultServlet(servletConfiguration);
        if (defaultServlet != null) {
          log.info("Default servlet is enabled, use servlet: [{}]", defaultServlet.getServletInfo());

          WebServletInitializer<Servlet> initializer = new WebServletInitializer<>(defaultServlet);
          initializer.setName(Constant.DEFAULT);
          initializer.setOrder(Ordered.HIGHEST_PRECEDENCE);
          initializer.addUrlMappings(servletConfiguration.getUrlMappings());
          initializer.setInitParameters(servletConfiguration.getInitParameters());

          getContextInitializers().add(initializer);
        }
      }
    }
  }

  protected Servlet getDefaultServlet(DefaultServletConfiguration servletConfiguration) {
    Servlet defaultServlet = servletConfiguration.getDefaultServlet();
    if (defaultServlet != null) {
      defaultServlet = createDefaultServlet();
    }
    return defaultServlet;
  }

  @Override
  protected List<WebApplicationInitializer> getMergedInitializers() {
    final List<WebApplicationInitializer> contextInitializers = getContextInitializers();
    contextInitializers.add(new OrderedServletContextInitializer() {
      @Override
      public void onStartup(ServletContext servletContext) {
        getContextInitParameters().forEach(servletContext::setInitParameter);
      }
    });

    final SessionConfiguration sessionConfig = getSessionConfig();
    if (sessionConfig != null && sessionConfig.isEnableHttpSession()) {
      contextInitializers.add(new OrderedServletContextInitializer() {

        @Override
        public void onStartup(ServletContext servletContext) {
          final SessionConfiguration sessionConfig = getSessionConfig();
          getWebApplicationConfiguration().configureSession(sessionConfig);
          final SessionCookieConfiguration cookie = sessionConfig.getCookieConfig();

          if (cookie != null) {
            final SessionCookieConfig config = servletContext.getSessionCookieConfig();
            config.setName(cookie.getName());
            config.setPath(cookie.getPath());
            config.setSecure(cookie.isSecure());
            config.setDomain(cookie.getDomain());
            config.setComment(cookie.getComment());
            config.setHttpOnly(cookie.isHttpOnly());
            config.setMaxAge((int) cookie.getMaxAge().getSeconds());
          }
          if (sessionConfig.getTrackingModes() != null) {
            final Set<SessionTrackingMode> collect = Arrays.stream(sessionConfig.getTrackingModes())
                    .map(Enum::name)
                    .map(SessionTrackingMode::valueOf)
                    .collect(Collectors.toSet());

            servletContext.setSessionTrackingModes(collect);
          }
        }
      });
    }
    return contextInitializers;
  }

  /**
   * Get Default Servlet Instance
   */
  protected abstract Servlet createDefaultServlet();

  @Override
  protected void prepareInitialize() {
    super.prepareInitialize();
    final WebServerApplicationContext context = obtainApplicationContext();

    final Class<?> startupClass = context.getStartupClass();
    if (startupClass != null) {
      ServletSecurity servletSecurity = startupClass.getAnnotation(ServletSecurity.class);
      if (servletSecurity != null) {
        if (context.containsBeanDefinition(ServletSecurityElement.class)) {
          log.info("Multiple: [{}] Overriding its bean definition", ServletSecurityElement.class.getName());
        }
        context.registerSingleton(new ServletSecurityElement(servletSecurity));
        context.registerBean("servletSecurityElement", ServletSecurityElement.class);
      }
    }

    addDefaultServlet();
    addJspServlet();
  }

}
