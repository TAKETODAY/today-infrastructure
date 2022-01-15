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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.beans.factory.SingletonBeanRegistry;
import cn.taketoday.context.event.ApplicationListener;
import cn.taketoday.core.GenericTypeResolver;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.TodayStrategies;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ExceptionUtils;
import cn.taketoday.web.WebApplicationFailedEvent;
import cn.taketoday.web.framework.server.WebServer;

/**
 * @author TODAY 2021/10/5 23:49
 * @since 4.0
 */
public class Application {
  private static final String SYSTEM_PROPERTY_JAVA_AWT_HEADLESS = "java.awt.headless";

  protected final Logger log = LoggerFactory.getLogger(getClass());
  private final String appBasePath = System.getProperty("user.dir");
  private final Class<?> mainApplicationClass;

  private boolean headless = true;

  private List<ApplicationListener<?>> listeners;

  private ConfigurableApplicationContext applicationContext;

  private List<ApplicationContextInitializer<?>> initializers;

  private ApplicationContextFactory applicationContextFactory = ApplicationContextFactory.DEFAULT;
  private ApplicationType applicationType;

  public Application() {
    setInitializers(TodayStrategies.getStrategies(ApplicationContextInitializer.class));
    this.mainApplicationClass = deduceMainApplicationClass();
  }

  private Class<?> deduceMainApplicationClass() {
    try {
      StackTraceElement[] stackTrace = new RuntimeException().getStackTrace();
      for (StackTraceElement stackTraceElement : stackTrace) {
        if ("main".equals(stackTraceElement.getMethodName())) {
          return Class.forName(stackTraceElement.getClassName());
        }
      }
    }
    catch (ClassNotFoundException ex) {
      // Swallow and continue
    }
    return null;
  }

  /**
   * Returns the type of application that is being run.
   *
   * @return the type of application
   */
  public ApplicationType getApplicationType() {
    return applicationType;
  }

  /**
   * Sets the type of web application to be run. If not explicitly set the type of web
   * application will be deduced based on the classpath.
   *
   * @param applicationType the application type
   */
  public void setApplicationType(ApplicationType applicationType) {
    Assert.notNull(applicationType, "ApplicationType is required");
    this.applicationType = applicationType;
  }

  /**
   * Run the Spring application, creating and refreshing a new
   * {@link ApplicationContext}.
   *
   * @param args the application arguments (usually passed from a Java main method)
   * @return a running {@link ApplicationContext}
   */
  public ConfigurableApplicationContext run(String[] args) {
    configureHeadlessProperty();
    ConfigurableApplicationContext context = getApplicationContext();
    context = createApplicationContext();
    preRun();

    try {
      SingletonBeanRegistry registry = context.unwrapFactory(SingletonBeanRegistry.class);
      registry.registerSingleton(this);

      context.unwrap(AnnotationConfigRegistry.class)
              .register(mainApplicationClass); // @since 1.0.2 import startup class
      context.refresh();

      WebServer webServer = context.getWebServer();
      Assert.state(webServer != null, "No Web server.");
      webServer.start();

      log.info("Your Application Started Successfully, It takes a total of [{}] ms.", //
              System.currentTimeMillis() - context.getStartupDate()//
      );
      return context;
    }
    catch (Throwable e) {
      context.close();
      try {
        context.publishEvent(new WebApplicationFailedEvent(context, e));
      }
      catch (Throwable ex) {
        log.warn("Exception thrown from publishEvent handling WebApplicationFailedEvent", ex);
      }
      throw ExceptionUtils.sneakyThrow(e);
    }
  }

  /**
   * Strategy method used to create the {@link ApplicationContext}. By default this
   * method will respect any explicitly set application context class or factory before
   * falling back to a suitable default.
   *
   * @return the application context (not yet refreshed)
   * @see #setApplicationContextFactory(ApplicationContextFactory)
   */
  protected ConfigurableApplicationContext createApplicationContext() {
    return this.applicationContextFactory.create(this.applicationType);
  }

  protected void preRun() {
    log.info("Starting Application at [{}]", getAppBasePath());
  }

  private void prepareContext(
          ConfigurableApplicationContext context, ConfigurableEnvironment environment) {

    applyInitializers(context);

  }

  /**
   * Sets the factory that will be called to create the application context. If not set,
   * defaults to a factory that will create
   * {@link AnnotationConfigServletWebServerApplicationContext} for servlet web
   * applications, {@link AnnotationConfigReactiveWebServerApplicationContext} for
   * reactive web applications, and {@link AnnotationConfigApplicationContext} for
   * non-web applications.
   *
   * @param applicationContextFactory the factory for the context
   */
  public void setApplicationContextFactory(
          ApplicationContextFactory applicationContextFactory) {
    this.applicationContextFactory =
            (applicationContextFactory != null) ? applicationContextFactory
                                                : ApplicationContextFactory.DEFAULT;
  }

  /**
   * Sets the {@link ApplicationContextInitializer} that will be applied to the Spring
   * {@link ApplicationContext}.
   *
   * @param initializers the initializers to set
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void setInitializers(Collection<ApplicationContextInitializer> initializers) {
    this.initializers = new ArrayList<>(initializers);
  }

  /**
   * Add {@link ApplicationContextInitializer}s to be applied to the Spring
   * {@link ApplicationContext}.
   *
   * @param initializers the initializers to add
   */
  public void addInitializers(ApplicationContextInitializer<?>... initializers) {
    this.initializers.addAll(Arrays.asList(initializers));
  }

  /**
   * Returns read-only ordered Set of the {@link ApplicationContextInitializer}s that
   * will be applied to the Spring {@link ApplicationContext}.
   *
   * @return the initializers
   */
  public Set<ApplicationContextInitializer<?>> getInitializers() {
    return asUnmodifiableOrderedSet(this.initializers);
  }

  /**
   * Apply any {@link ApplicationContextInitializer}s to the context before it is
   * refreshed.
   *
   * @param context the configured ApplicationContext (not refreshed yet)
   * @see ConfigurableApplicationContext#refresh()
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  protected void applyInitializers(ConfigurableApplicationContext context) {
    for (ApplicationContextInitializer initializer : getInitializers()) {
      Class<?> requiredType = GenericTypeResolver.resolveTypeArgument(initializer.getClass(),
              ApplicationContextInitializer.class);
      Assert.isInstanceOf(requiredType, context, "Unable to call initializer.");
      initializer.initialize(context);
    }
  }

  /**
   * Sets the {@link ApplicationListener}s that will be applied to the SpringApplication
   * and registered with the {@link ApplicationContext}.
   *
   * @param listeners the listeners to set
   */
  public void setListeners(Collection<? extends ApplicationListener<?>> listeners) {
    this.listeners = new ArrayList<>(listeners);
  }

  /**
   * Add {@link ApplicationListener}s to be applied to the SpringApplication and
   * registered with the {@link ApplicationContext}.
   *
   * @param listeners the listeners to add
   */
  public void addListeners(ApplicationListener<?>... listeners) {
    this.listeners.addAll(Arrays.asList(listeners));
  }

  /**
   * Returns read-only ordered Set of the {@link ApplicationListener}s that will be
   * applied to the SpringApplication and registered with the {@link ApplicationContext}
   *
   * @return the listeners
   */
  public Set<ApplicationListener<?>> getListeners() {
    return asUnmodifiableOrderedSet(this.listeners);
  }

  /**
   * Sets if the application is headless and should not instantiate AWT. Defaults to
   * {@code true} to prevent java icons appearing.
   *
   * @param headless if the application is headless
   */
  public void setHeadless(boolean headless) {
    this.headless = headless;
  }

  private void configureHeadlessProperty() {
    System.setProperty(SYSTEM_PROPERTY_JAVA_AWT_HEADLESS, System.getProperty(
            SYSTEM_PROPERTY_JAVA_AWT_HEADLESS, Boolean.toString(this.headless)));
  }

  public ConfigurableApplicationContext getApplicationContext() {
    return applicationContext;
  }

  public String getAppBasePath() {
    return appBasePath;
  }

  private static <E> Set<E> asUnmodifiableOrderedSet(Collection<E> elements) {
    ArrayList<E> list = new ArrayList<>(elements);
    list.sort(AnnotationAwareOrderComparator.INSTANCE);
    return new LinkedHashSet<>(list);
  }

}
