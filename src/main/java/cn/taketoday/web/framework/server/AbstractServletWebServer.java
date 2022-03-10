/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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
package cn.taketoday.web.framework.server;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import cn.taketoday.beans.BeanUtils;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.factory.config.SingletonBeanRegistry;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.core.ConfigurationException;
import cn.taketoday.core.Ordered;
import cn.taketoday.lang.Constant;
import cn.taketoday.web.config.WebApplicationInitializer;
import cn.taketoday.web.framework.WebServerApplicationContext;
import cn.taketoday.web.framework.config.DefaultServletConfiguration;
import cn.taketoday.web.framework.config.JspServletConfiguration;
import cn.taketoday.web.servlet.initializer.OrderedServletContextInitializer;
import cn.taketoday.web.servlet.initializer.WebServletInitializer;
import cn.taketoday.web.session.SessionConfiguration;
import cn.taketoday.web.session.SessionCookieConfig;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletSecurityElement;
import jakarta.servlet.SessionTrackingMode;
import jakarta.servlet.annotation.ServletSecurity;
import lombok.Getter;
import lombok.Setter;

/**
 * For servlet web server
 *
 * @author TODAY 2019-01-26 11:08
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
    JspServletConfiguration jspServletConfiguration = this.jspServletConfiguration;
    if (jspServletConfiguration != null) {
      // config jsp servlet
      getWebApplicationConfiguration().configureJspServlet(jspServletConfiguration);
      if (jspServletConfiguration.isEnabled()) {
        try {
          Servlet jspServlet = BeanUtils.newInstance(jspServletConfiguration.getClassName());
          log.info("Jsp is enabled, use jsp servlet: [{}]", jspServlet.getServletInfo());

          WebServletInitializer<Servlet> initializer = new WebServletInitializer<>(jspServlet);
          initializer.setName(jspServletConfiguration.getName());
          initializer.setOrder(Ordered.HIGHEST_PRECEDENCE);
          initializer.addUrlMappings(jspServletConfiguration.getUrlMappings());
          initializer.setInitParameters(jspServletConfiguration.getInitParameters());

          getContextInitializers().add(initializer);
        }
        catch (ClassNotFoundException e) {
          throw new ConfigurationException("jsp servlet class not found", e);
        }
      }
    }
  }

  /**
   * Add default servlet
   */
  protected void addDefaultServlet() {
    DefaultServletConfiguration servletConfiguration = this.defaultServletConfiguration;
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
    List<WebApplicationInitializer> contextInitializers = getContextInitializers();
    contextInitializers.add((OrderedServletContextInitializer) servletContext -> getContextInitParameters().forEach(servletContext::setInitParameter));

    SessionConfiguration sessionConfig = getSessionConfig();
    if (sessionConfig != null && sessionConfig.isEnableHttpSession()) {

      contextInitializers.add((OrderedServletContextInitializer) servletContext -> {
        getWebApplicationConfiguration().configureSession(sessionConfig);
        SessionCookieConfig cookie = sessionConfig.getCookieConfig();

        if (cookie != null) {
          jakarta.servlet.SessionCookieConfig config = servletContext.getSessionCookieConfig();
          config.setName(cookie.getName());
          config.setPath(cookie.getPath());
          config.setSecure(cookie.isSecure());
          config.setDomain(cookie.getDomain());
          config.setHttpOnly(cookie.isHttpOnly());
          config.setMaxAge((int) cookie.getMaxAge().getSeconds());
        }
        if (sessionConfig.getTrackingModes() != null) {
          Set<SessionTrackingMode> collect = Arrays.stream(sessionConfig.getTrackingModes())
                  .map(Enum::name)
                  .map(SessionTrackingMode::valueOf)
                  .collect(Collectors.toSet());

          servletContext.setSessionTrackingModes(collect);
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
    WebServerApplicationContext context = obtainApplicationContext();
    Class<?> mainApplicationClass = getMainApplicationClass();
    if (mainApplicationClass != null) {
      ServletSecurity servletSecurity = mainApplicationClass.getAnnotation(ServletSecurity.class);
      if (servletSecurity != null) {
        BeanDefinitionRegistry registry = context.unwrapFactory(BeanDefinitionRegistry.class);
        if (registry.containsBeanDefinition(ServletSecurityElement.class)) {
          log.info("Multiple: [{}] Overriding its bean definition",
                  ServletSecurityElement.class.getName());
        }
        context.unwrapFactory(SingletonBeanRegistry.class)
                .registerSingleton(new ServletSecurityElement(servletSecurity));
      }
    }

    addDefaultServlet();
    addJspServlet();
  }

}
