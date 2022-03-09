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
package cn.taketoday.web.servlet.initializer;

import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.web.multipart.MultipartConfiguration;
import cn.taketoday.web.servlet.DispatcherServlet;
import cn.taketoday.web.servlet.WebServletApplicationContext;
import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletRegistration.Dynamic;
import jakarta.servlet.ServletSecurityElement;

/**
 * @author TODAY <br>
 * 2019-02-03 14:08
 */
public class DispatcherServletInitializer extends WebServletInitializer<DispatcherServlet> {
  public static final String DISPATCHER_SERVLET = "dispatcherServlet";

  private static final Logger log = LoggerFactory.getLogger(DispatcherServletInitializer.class);

  private boolean autoCreateDispatcher;
  private final WebServletApplicationContext applicationContext;

  public DispatcherServletInitializer(WebServletApplicationContext context) {
    this(context, null);
  }

  public DispatcherServletInitializer(WebServletApplicationContext context, DispatcherServlet dispatcherServlet) {
    super(dispatcherServlet);
    this.applicationContext = context;
    setOrder(HIGHEST_PRECEDENCE + 100);
    setName(DISPATCHER_SERVLET);
    addUrlMappings(DEFAULT_MAPPING);
  }

  @Override
  public DispatcherServlet getServlet() {
    DispatcherServlet dispatcherServlet = super.getServlet();
    if (dispatcherServlet == null && isAutoCreateDispatcher()) {
      WebServletApplicationContext context = getApplicationContext();
      BeanDefinitionRegistry registry = context.unwrapFactory(BeanDefinitionRegistry.class);
      dispatcherServlet = context.getBean(DispatcherServlet.class);
      setServlet(dispatcherServlet);
    }
    return dispatcherServlet;
  }

  @Override
  protected void configureRegistration(Dynamic registration) {
    super.configureRegistration(registration);
    if (log.isInfoEnabled()) {
      log.info("Registering dispatcher-servlet: [{}] With Url Mappings: {}", getServlet(), getUrlMappings());
    }
  }

  @Override
  protected void configureMultipart(Dynamic registration) {
    MultipartConfigElement multipartConfig = getMultipartConfig();
    if (multipartConfig == null) {
      MultipartConfiguration configuration = getApplicationContext().getBean(MultipartConfiguration.class);
      if (configuration != null) {
        multipartConfig = new MultipartConfigElement(
                configuration.getLocation(),
                configuration.getMaxFileSize().toBytes(),
                configuration.getMaxRequestSize().toBytes(),
                (int) configuration.getFileSizeThreshold().toBytes());
        log.info("configure dispatcher-servlet multipart: {}", configuration);
      }
    }

    setMultipartConfig(multipartConfig);
    super.configureMultipart(registration);
  }

  @Override
  protected void configureServletSecurity(Dynamic registration) {
    ServletSecurityElement servletSecurity = getServletSecurity();
    if (servletSecurity == null) {
      servletSecurity = getApplicationContext().getBean(ServletSecurityElement.class);
    }
    if (servletSecurity != null) {
      setServletSecurity(servletSecurity);
      super.configureServletSecurity(registration);
    }
  }

  public boolean isAutoCreateDispatcher() {
    return autoCreateDispatcher;
  }

  public WebServletApplicationContext getApplicationContext() {
    return applicationContext;
  }

  public void setAutoCreateDispatcher(boolean autoCreateDispatcher) {
    this.autoCreateDispatcher = autoCreateDispatcher;
  }

}
