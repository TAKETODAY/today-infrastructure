/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright ©  TODAY & 2017 - 2021 All Rights Reserved.
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
import java.util.Set;

import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.support.BeanDefinitionBuilder;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.util.ExceptionUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.config.WebApplicationInitializer;
import cn.taketoday.web.config.WebApplicationLoader;
import cn.taketoday.web.config.WebMvcConfiguration;
import cn.taketoday.web.handler.DispatcherHandler;
import cn.taketoday.web.resolver.ParameterResolvingStrategy;
import cn.taketoday.web.resolver.ServletParameterResolvers;
import cn.taketoday.web.servlet.initializer.DispatcherServletInitializer;
import cn.taketoday.web.servlet.initializer.WebFilterInitializer;
import cn.taketoday.web.servlet.initializer.WebListenerInitializer;
import cn.taketoday.web.servlet.initializer.WebServletInitializer;
import jakarta.servlet.Filter;
import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletSecurityElement;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.annotation.WebInitParam;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.annotation.WebServlet;

/**
 * Initialize Web application in a server like tomcat, jetty, undertow
 *
 * @author TODAY <br>
 * 2019-01-12 17:28
 */
public class WebServletApplicationLoader
        extends WebApplicationLoader implements ServletContainerInitializer {

  /** @since 3.0 */
  private String requestCharacterEncoding = Constant.DEFAULT_ENCODING;
  /** @since 3.0 */
  private String responseCharacterEncoding = Constant.DEFAULT_ENCODING;

  @Override
  protected ServletWebMvcConfiguration getWebMvcConfiguration(ApplicationContext applicationContext) {
    return new ServletCompositeWebMvcConfiguration(applicationContext.getBeans(WebMvcConfiguration.class));
  }

  @Override
  public WebServletApplicationContext obtainApplicationContext() {
    return (WebServletApplicationContext) super.obtainApplicationContext();
  }

  @Override
  protected String getWebMvcConfigLocation() {
    String webMvcConfigLocation = super.getWebMvcConfigLocation();
    if (StringUtils.isEmpty(webMvcConfigLocation)) {
      webMvcConfigLocation = getServletContext().getInitParameter(WEB_MVC_CONFIG_LOCATION);
    }
    if (StringUtils.isEmpty(webMvcConfigLocation)) { // scan from '/'
      String rootPath = getServletContext().getRealPath("/");
      HashSet<String> paths = new HashSet<>();
      File dir = new File(rootPath);
      if (dir.exists()) {
        log.trace("Finding Configuration File From Root Path: [{}]", rootPath);
        scanConfigLocation(dir, paths, pathname -> pathname.isDirectory() || pathname.getName().endsWith(".xml"));
        return StringUtils.collectionToString(paths);
      }
      return null;
    }
    return webMvcConfigLocation;
  }

  /**
   * @return {@link ServletContext} or null if {@link ApplicationContext} not
   * initialize
   */
  protected ServletContext getServletContext() {
    return obtainApplicationContext().getServletContext();
  }

  /**
   * Find configuration file.
   *
   * @param dir directory
   */
  protected void scanConfigLocation(File dir, Set<String> files, FileFilter filter) {
    if (log.isTraceEnabled()) {
      log.trace("Enter [{}]", dir.getAbsolutePath());
    }
    File[] listFiles = dir.listFiles(filter);
    if (listFiles == null) {
      log.error("File: [{}] Does not exist", dir);
      return;
    }
    for (File file : listFiles) {
      if (file.isDirectory()) { // recursive
        scanConfigLocation(file, files, filter);
      }
      else {
        files.add(file.getAbsolutePath());
      }
    }
  }

  /**
   * Prepare {@link WebServletApplicationContext}
   *
   * @param servletContext {@link ServletContext}
   * @return {@link WebServletApplicationContext}
   */
  protected WebServletApplicationContext prepareApplicationContext(ServletContext servletContext) {
    WebServletApplicationContext context = getWebServletApplicationContext();
    if (context == null) {
      long startupDate = System.currentTimeMillis();
      log.info("Your application starts to be initialized at: [{}].",
              new SimpleDateFormat(Constant.DEFAULT_DATE_FORMAT).format(startupDate));
      context = createContext();
      context.setServletContext(servletContext);
      setApplicationContext(context);
      context.refresh();
    }
    else if (context.getServletContext() == null) {
      context.setServletContext(servletContext);
      log.info("ServletContext: [{}] configure successfully.", servletContext);
    }
    return context;
  }

  /**
   * create a {@link WebServletApplicationContext},
   * subclasses can override this method to create user customize context
   */
  protected WebServletApplicationContext createContext() {
    return new StandardWebServletApplicationContext();
  }

  private WebServletApplicationContext getWebServletApplicationContext() {
    return (WebServletApplicationContext) getApplicationContext();
  }

  @Override
  public void onStartup(Set<Class<?>> classes, ServletContext servletContext) {
    Assert.notNull(servletContext, "ServletContext can't be null");
    WebApplicationContext context = prepareApplicationContext(servletContext);
    try {
      try {
        servletContext.setRequestCharacterEncoding(getRequestCharacterEncoding());
        servletContext.setResponseCharacterEncoding(getResponseCharacterEncoding());
      }
      catch (Throwable ignored) { }
      onStartup(context);
    }
    catch (Throwable ex) {
      throw ExceptionUtils.sneakyThrow(ex);
    }
  }

  @Override
  protected void configureParameterResolving(
          List<ParameterResolvingStrategy> customizedStrategies, WebMvcConfiguration mvcConfiguration) {
    // register servlet env resolvers
    ServletParameterResolvers.register(customizedStrategies, getServletContext());
    super.configureParameterResolving(customizedStrategies, mvcConfiguration);
  }

  @Override
  protected DispatcherHandler createDispatcher(WebApplicationContext ctx) {
    Assert.isInstanceOf(WebServletApplicationContext.class, ctx, "context must be a WebServletApplicationContext");
    WebServletApplicationContext context = (WebServletApplicationContext) ctx;
    DispatcherServletInitializer initializer = context.getBean(DispatcherServletInitializer.class);
    if (initializer != null) {
      DispatcherServlet ret = initializer.getServlet();
      if (ret == null) {
        ret = doCreateDispatcherServlet(context);
        initializer.setServlet(ret);
      }
      return ret;
    }
    return doCreateDispatcherServlet(context);
  }

  protected DispatcherServlet doCreateDispatcherServlet(WebServletApplicationContext context) {
    return new DispatcherServlet(context);
  }

  @Override
  protected void configureInitializer(List<WebApplicationInitializer> initializers, WebMvcConfiguration config) {
    WebServletApplicationContext ctx = obtainApplicationContext();

    configureFilterInitializers(ctx, initializers);
    configureServletInitializers(ctx, initializers);
    configureListenerInitializers(ctx, initializers);
    BeanDefinitionRegistry registry = ctx.unwrapFactory(BeanDefinitionRegistry.class);
    // DispatcherServlet Initializer
    if (!registry.containsBeanDefinition(DispatcherServletInitializer.class)) {
      initializers.add(new DispatcherServletInitializer(ctx, obtainDispatcher()));
    }

    super.configureInitializer(initializers, config);
  }

  @Override
  public DispatcherServlet obtainDispatcher() {
    return (DispatcherServlet) super.obtainDispatcher();
  }

  /**
   * Configure {@link Filter}
   *
   * @param applicationContext {@link ApplicationContext}
   * @param contextInitializers {@link WebApplicationInitializer}s
   */
  protected void configureFilterInitializers(
          WebApplicationContext applicationContext, List<WebApplicationInitializer> contextInitializers) {

    List<Filter> filters = applicationContext.getAnnotatedBeans(WebFilter.class);
    for (Filter filter : filters) {
      Class<?> beanClass = filter.getClass();
      WebFilterInitializer<Filter> webFilterInitializer = new WebFilterInitializer<>(filter);
      WebFilter webFilter = beanClass.getAnnotation(WebFilter.class);
      Set<String> urlPatterns = new HashSet<>();
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
        String displayName = webFilter.displayName();
        if (StringUtils.isEmpty(displayName)) {
          name = BeanDefinitionBuilder.defaultBeanName(beanClass);
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
   * @param applicationContext {@link ApplicationContext}
   * @param contextInitializers {@link WebApplicationInitializer}s
   */
  protected void configureServletInitializers(
          WebApplicationContext applicationContext, List<WebApplicationInitializer> contextInitializers) {

    Collection<Servlet> servlets = applicationContext.getAnnotatedBeans(WebServlet.class);
    for (Servlet servlet : servlets) {
      Class<?> beanClass = servlet.getClass();
      WebServletInitializer<Servlet> webServletInitializer = new WebServletInitializer<>(servlet);
      WebServlet webServlet = beanClass.getAnnotation(WebServlet.class);
      String[] urlPatterns = webServlet.urlPatterns();
      if (ObjectUtils.isEmpty(urlPatterns)) {
        urlPatterns = new String[] { BeanDefinitionBuilder.defaultBeanName(beanClass) };
      }
      webServletInitializer.addUrlMappings(urlPatterns);
      webServletInitializer.setLoadOnStartup(webServlet.loadOnStartup());
      webServletInitializer.setAsyncSupported(webServlet.asyncSupported());

      for (WebInitParam initParam : webServlet.initParams()) {
        webServletInitializer.addInitParameter(initParam.name(), initParam.value());
      }

      MultipartConfig multipartConfig = beanClass.getAnnotation(MultipartConfig.class);
      if (multipartConfig != null) {
        webServletInitializer.setMultipartConfig(new MultipartConfigElement(multipartConfig));
      }
      ServletSecurity servletSecurity = beanClass.getAnnotation(ServletSecurity.class);
      if (servletSecurity != null) {
        webServletInitializer.setServletSecurity(new ServletSecurityElement(servletSecurity));
      }

      String name = webServlet.name();
      if (StringUtils.isEmpty(name)) {
        String displayName = webServlet.displayName();
        if (StringUtils.isEmpty(displayName)) {
          name = BeanDefinitionBuilder.defaultBeanName(beanClass);
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
   * @param context {@link ApplicationContext}
   * @param contextInitializers {@link WebApplicationInitializer}s
   */
  protected void configureListenerInitializers(
          WebApplicationContext context, List<WebApplicationInitializer> contextInitializers) {
    Collection<EventListener> eventListeners = context.getAnnotatedBeans(WebListener.class);
    for (EventListener eventListener : eventListeners) {
      contextInitializers.add(new WebListenerInitializer<>(eventListener));
    }
  }

  //

  /**
   * Sets the request character encoding for this ServletContext.
   *
   * @param encoding request character encoding
   * @since 3.0
   */
  public void setRequestCharacterEncoding(String encoding) {
    this.requestCharacterEncoding = encoding;
  }

  /**
   * Sets the response character encoding for this ServletContext.
   *
   * @param encoding response character encoding
   * @since 3.0
   */
  public void setResponseCharacterEncoding(String encoding) {
    this.responseCharacterEncoding = encoding;
  }

  /**
   * Gets the request character encoding that are supported by default for
   * this <tt>ServletContext</tt>. This method returns null if no request
   * encoding character encoding has been specified in deployment descriptor
   * or container specific configuration (for all web applications in the
   * container).
   *
   * @return the request character encoding that are supported by default for
   * this <tt>ServletContext</tt>
   */
  public String getRequestCharacterEncoding() {
    return requestCharacterEncoding;
  }

  /**
   * Gets the response character encoding that are supported by default for
   * this <tt>ServletContext</tt>. This method returns null if no response
   * encoding character encoding has been specified in deployment descriptor
   * or container specific configuration (for all web applications in the
   * container).
   *
   * @return the request character encoding that are supported by default for
   * this <tt>ServletContext</tt>
   */
  public String getResponseCharacterEncoding() {
    return responseCharacterEncoding;
  }

}
