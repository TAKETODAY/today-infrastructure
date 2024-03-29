/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.framework.web.servlet.context;

import java.util.Collection;
import java.util.Collections;
import java.util.EventListener;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.config.Scope;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationContextException;
import cn.taketoday.core.io.Resource;
import cn.taketoday.framework.ApplicationType;
import cn.taketoday.framework.availability.AvailabilityChangeEvent;
import cn.taketoday.framework.availability.ReadinessState;
import cn.taketoday.framework.web.context.ConfigurableWebServerApplicationContext;
import cn.taketoday.framework.web.context.MissingWebServerFactoryBeanException;
import cn.taketoday.framework.web.context.WebServerGracefulShutdownLifecycle;
import cn.taketoday.framework.web.server.WebServer;
import cn.taketoday.framework.web.servlet.FilterRegistrationBean;
import cn.taketoday.framework.web.servlet.ServletContextInitializer;
import cn.taketoday.framework.web.servlet.ServletContextInitializerBeans;
import cn.taketoday.framework.web.servlet.ServletRegistrationBean;
import cn.taketoday.framework.web.servlet.server.ServletWebServerFactory;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.servlet.ContextLoaderListener;
import cn.taketoday.web.servlet.ServletContextAware;
import cn.taketoday.web.servlet.WebApplicationContext;
import cn.taketoday.web.servlet.support.GenericWebApplicationContext;
import cn.taketoday.web.servlet.support.ServletContextAwareProcessor;
import cn.taketoday.web.servlet.support.ServletContextResource;
import cn.taketoday.web.servlet.support.WebApplicationContextUtils;
import jakarta.servlet.Filter;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;

/**
 * A {@link WebApplicationContext} that can be used to bootstrap itself from a contained
 * {@link ServletWebServerFactory} bean.
 * <p>
 * This context will create, initialize and run an {@link WebServer} by searching for a
 * single {@link ServletWebServerFactory} bean within the {@link ApplicationContext}
 * itself. The {@link ServletWebServerFactory} is free to use standard Framework concepts
 * (such as dependency injection, lifecycle callbacks and property placeholder variables).
 * <p>
 * In addition, any {@link Servlet} or {@link Filter} beans defined in the context will be
 * automatically registered with the web server. In the case of a single Servlet bean, the
 * '/' mapping will be used. If multiple Servlet beans are found then the lowercase bean
 * name will be used as a mapping prefix. Any Servlet named 'dispatcherServlet' will
 * always be mapped to '/'. Filter beans will be mapped to all URLs ('/*').
 * <p>
 * For more advanced configuration, the context can instead define beans that implement
 * the {@link ServletContextInitializer} interface (most often
 * {@link ServletRegistrationBean}s and/or {@link FilterRegistrationBean}s). To prevent
 * double registration, the use of {@link ServletContextInitializer} beans will disable
 * automatic Servlet and Filter bean registration.
 * <p>
 * Although this context can be used directly, most developers should consider using the
 * {@link AnnotationConfigServletWebServerApplicationContext}
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see AnnotationConfigServletWebServerApplicationContext
 * @see ServletWebServerFactory
 * @since 4.0
 */
public class ServletWebServerApplicationContext extends GenericWebApplicationContext
        implements ConfigurableWebServerApplicationContext, ServletContextInitializer {

  /**
   * Constant value for the DispatcherServlet bean name. A Servlet bean with this name
   * is deemed to be the "main" servlet and is automatically given a mapping of "/" by
   * default. To change the default behavior you can use a
   * {@link ServletRegistrationBean} or a different bean name.
   */
  public static final String DISPATCHER_SERVLET_NAME = "dispatcherServlet";

  @Nullable
  private volatile WebServer webServer;

  @Nullable
  private ServletConfig servletConfig;

  @Nullable
  private String serverNamespace;

  /**
   * Create a new {@link ServletWebServerApplicationContext}.
   */
  public ServletWebServerApplicationContext() { }

  /**
   * Create a new {@link ServletWebServerApplicationContext} with the given
   * {@code StandardBeanFactory}.
   *
   * @param beanFactory the StandardBeanFactory instance to use for this context
   */
  public ServletWebServerApplicationContext(StandardBeanFactory beanFactory) {
    super(beanFactory);
  }

  /**
   * Register ServletContextAwareProcessor.
   *
   * @see ServletContextAwareProcessor
   */
  @Override
  protected void postProcessBeanFactory(ConfigurableBeanFactory beanFactory) {
    beanFactory.addBeanPostProcessor(new WebApplicationContextServletContextAwareProcessor(this));
    beanFactory.ignoreDependencyInterface(ServletContextAware.class);
    registerWebApplicationScopes();
  }

  @Override
  public final void refresh() throws BeansException, IllegalStateException {
    try {
      super.refresh();
    }
    catch (RuntimeException ex) {
      WebServer webServer = this.webServer;
      if (webServer != null) {
        webServer.stop();
        webServer.destroy();
      }
      throw ex;
    }
  }

  @Override
  protected void onRefresh() {
    super.onRefresh();
    try {
      createWebServer();
    }
    catch (Throwable ex) {
      throw new ApplicationContextException("Unable to start web server", ex);
    }
  }

  @Override
  protected void doClose() {
    if (isActive()) {
      AvailabilityChangeEvent.publish(this, ReadinessState.REFUSING_TRAFFIC);
    }
    super.doClose();

    WebServer webServer = this.webServer;
    if (webServer != null) {
      webServer.destroy();
    }
  }

  private void createWebServer() {
    WebServer webServer = this.webServer;
    ServletContext servletContext = getServletContext();
    if (webServer == null && servletContext == null) {
      ServletWebServerFactory factory = getWebServerFactory();
      webServer = factory.getWebServer(this);

      StandardBeanFactory beanFactory = getBeanFactory();
      beanFactory.registerSingleton("webServerStartStop", new WebServerStartStopLifecycle(this, webServer));
      beanFactory.registerSingleton("webServerGracefulShutdown", new WebServerGracefulShutdownLifecycle(webServer));

      this.webServer = webServer;
    }
    else if (servletContext != null) {
      try {
        onStartup(servletContext);
      }
      catch (ServletException ex) {
        throw new ApplicationContextException("Cannot initialize servlet context", ex);
      }
    }
    initPropertySources();
  }

  /**
   * Returns the {@link ServletWebServerFactory} that should be used to create the
   * embedded {@link WebServer}. By default this method searches for a suitable bean in
   * the context itself.
   *
   * @return a {@link ServletWebServerFactory} (never {@code null})
   */
  protected ServletWebServerFactory getWebServerFactory() {
    // Use bean names so that we don't consider the hierarchy
    StandardBeanFactory beanFactory = getBeanFactory();
    Set<String> beanNames = beanFactory.getBeanNamesForType(ServletWebServerFactory.class);
    if (beanNames.isEmpty()) {
      throw new MissingWebServerFactoryBeanException(
              getClass(), ServletWebServerFactory.class, ApplicationType.SERVLET_WEB);
    }
    if (beanNames.size() > 1) {
      throw new ApplicationContextException("Unable to start ServletWebServerApplicationContext due to multiple "
              + "ServletWebServerFactory beans : " + StringUtils.collectionToCommaDelimitedString(beanNames));
    }
    return beanFactory.getBean(CollectionUtils.firstElement(beanNames), ServletWebServerFactory.class);
  }

  /**
   * The self initializer
   * <p>
   * the {@link ServletContextInitializer} that will be used to complete the
   * setup of this {@link WebApplicationContext}.
   *
   * @see #prepareWebApplicationContext(ServletContext)
   */
  @Override
  public void onStartup(ServletContext servletContext) throws ServletException {
    prepareWebApplicationContext(servletContext);
    WebApplicationContextUtils.registerEnvironmentBeans(getBeanFactory(), servletContext);
    for (ServletContextInitializer beans : getServletContextInitializerBeans()) {
      beans.onStartup(servletContext);
    }
  }

  private void registerWebApplicationScopes() {
    var existingScopes = new ExistingWebApplicationScopes(getBeanFactory());
    WebApplicationContextUtils.registerWebApplicationScopes(getBeanFactory());
    existingScopes.restore();
  }

  /**
   * Returns {@link ServletContextInitializer}s that should be used with the embedded
   * web server. By default this method will first attempt to find
   * {@link ServletContextInitializer}, {@link Servlet}, {@link Filter} and certain
   * {@link EventListener} beans.
   *
   * @return the servlet initializer beans
   */
  protected Collection<ServletContextInitializer> getServletContextInitializerBeans() {
    return new ServletContextInitializerBeans(getBeanFactory());
  }

  /**
   * Prepare the {@link WebApplicationContext} with the given fully loaded
   * {@link ServletContext}. This method is usually called from
   * {@link ServletContextInitializer#onStartup(ServletContext)} and is similar to the
   * functionality usually provided by a {@link ContextLoaderListener}.
   *
   * @param servletContext the operational servlet context
   */
  protected void prepareWebApplicationContext(ServletContext servletContext) {
    Object rootContext = servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
    if (rootContext != null) {
      if (rootContext == this) {
        throw new IllegalStateException(
                "Cannot initialize context because there is already a root application context present - "
                        + "check whether you have multiple ServletContextInitializers!");
      }
      return;
    }
    servletContext.log("Initializing embedded WebApplicationContext");
    try {
      servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, this);
      if (logger.isDebugEnabled()) {
        logger.debug("Published root WebApplicationContext as ServletContext attribute with name [{}]",
                WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
      }
      setServletContext(servletContext);
      if (logger.isInfoEnabled()) {
        long elapsedTime = System.currentTimeMillis() - getStartupDate();
        logger.info("Root WebApplicationContext: initialization completed in {} ms", elapsedTime);
      }
    }
    catch (RuntimeException | Error ex) {
      logger.error("Context initialization failed", ex);
      servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, ex);
      throw ex;
    }
  }

  @Override
  protected Resource getResourceByPath(String path) {
    if (getServletContext() == null) {
      return new ClassPathContextResource(path, getClassLoader());
    }
    return new ServletContextResource(getServletContext(), path);
  }

  @Nullable
  @Override
  public String getServerNamespace() {
    return this.serverNamespace;
  }

  @Override
  public void setServerNamespace(@Nullable String serverNamespace) {
    this.serverNamespace = serverNamespace;
  }

  @Override
  public void setServletConfig(@Nullable ServletConfig servletConfig) {
    this.servletConfig = servletConfig;
  }

  @Nullable
  @Override
  public ServletConfig getServletConfig() {
    return this.servletConfig;
  }

  /**
   * Returns the {@link WebServer} that was created by the context or {@code null} if
   * the server has not yet been created.
   *
   * @return the embedded web server
   */
  @Nullable
  @Override
  public WebServer getWebServer() {
    return this.webServer;
  }

  /**
   * Utility class to store and restore any user defined scopes. This allows scopes to
   * be registered in an ApplicationContextInitializer in the same way as they would in
   * a classic non-embedded web application context.
   */
  public class ExistingWebApplicationScopes {

    private static final Set<String> SCOPES;

    static {
      Set<String> scopes = new LinkedHashSet<>();
      scopes.add(RequestContext.SCOPE_REQUEST);
      scopes.add(RequestContext.SCOPE_SESSION);
      SCOPES = Collections.unmodifiableSet(scopes);
    }

    private final ConfigurableBeanFactory beanFactory;

    private final Map<String, Scope> scopes = new HashMap<>();

    public ExistingWebApplicationScopes(ConfigurableBeanFactory beanFactory) {
      this.beanFactory = beanFactory;
      for (String scopeName : SCOPES) {
        Scope scope = beanFactory.getRegisteredScope(scopeName);
        if (scope != null) {
          this.scopes.put(scopeName, scope);
        }
      }
    }

    public void restore() {
      for (Map.Entry<String, Scope> entry : scopes.entrySet()) {
        String key = entry.getKey();
        Scope value = entry.getValue();
        logger.info("Restoring user defined scope {}", key);
        beanFactory.registerScope(key, value);
      }

    }

  }

}
