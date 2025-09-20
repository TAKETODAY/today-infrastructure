/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.web;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import infra.beans.BeanUtils;
import infra.beans.factory.BeanNameAware;
import infra.context.ApplicationContext;
import infra.context.ApplicationContextAware;
import infra.context.ApplicationContextException;
import infra.context.ApplicationContextInitializer;
import infra.context.ApplicationListener;
import infra.context.ConfigurableApplicationContext;
import infra.context.EnvironmentAware;
import infra.context.SmartLifecycle;
import infra.context.event.ContextRefreshedEvent;
import infra.context.event.SourceFilteringListener;
import infra.context.support.AbstractRefreshableConfigApplicationContext;
import infra.context.support.ClassPathXmlApplicationContext;
import infra.core.Conventions;
import infra.core.annotation.AnnotationAwareOrderComparator;
import infra.core.env.ConfigurableEnvironment;
import infra.core.env.Environment;
import infra.core.env.EnvironmentCapable;
import infra.core.env.StandardEnvironment;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.CollectionUtils;
import infra.util.ObjectUtils;

/**
 * Infrastructure Handler
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/9/26 23:21
 */
public abstract class InfraHandler implements ApplicationContextAware, EnvironmentCapable, EnvironmentAware, BeanNameAware, SmartLifecycle {

  protected static final Logger log = LoggerFactory.getLogger(DispatcherHandler.class);

  /** If the ApplicationContext was injected via {@link #setApplicationContext}. */
  private boolean applicationContextInjected;

  /** ApplicationContext implementation class to create. */
  private Class<?> contextClass = ClassPathXmlApplicationContext.class;

  /** ApplicationContext id to assign. */
  @Nullable
  private String contextId;

  /** Explicit context config location. */
  @Nullable
  private String contextConfigLocation;

  /** Actual ApplicationContextInitializer instances to apply to the context. */
  private final ArrayList<ApplicationContextInitializer> contextInitializers = new ArrayList<>();

  /** Flag used to detect whether onRefresh has already been called. */
  private volatile boolean refreshEventReceived;

  /** Monitor for synchronized onRefresh execution. */
  private final Object onRefreshMonitor = new Object();

  protected String beanName = Conventions.getVariableName(this);

  @Nullable
  private ConfigurableEnvironment environment;

  @Nullable
  private ApplicationContext applicationContext;

  /** Whether to log potentially sensitive info (request params at DEBUG + headers at TRACE). */
  private boolean enableLoggingRequestDetails = false;

  protected final AtomicBoolean running = new AtomicBoolean(false);

  protected InfraHandler() {
  }

  /**
   * Create a new {@code InfraHandler} with the given application context.
   *
   * <p>Using this constructor indicates that the following properties / init-params
   * will be ignored:
   * <ul>
   * <li>{@link #setContextClass(Class)} / 'contextClass'</li>
   * <li>{@link #setContextConfigLocation(String)} / 'contextConfigLocation'</li>
   * </ul>
   * <p>The given application context may or may not yet be {@linkplain
   * ConfigurableApplicationContext#refresh() refreshed}. If it (a) is an implementation
   * of {@link ConfigurableApplicationContext} and (b) has <strong>not</strong>
   * already been refreshed (the recommended approach), then the following will occur:
   * <ul>
   * <li>If the given context does not already have a {@linkplain
   * ConfigurableApplicationContext#setParent parent}, the root application context
   * will be set as the parent.</li>
   * <li>If the given context has not already been assigned an {@linkplain
   * ConfigurableApplicationContext#setId id}, one will be assigned to it</li>
   * <li>{@code MockContext} and {@code ServletConfig} objects will be delegated to
   * the application context</li>
   * <li>{@link #postProcessApplicationContext} will be called</li>
   * <li>Any {@link ApplicationContextInitializer ApplicationContextInitializers} specified through the
   * "contextInitializerClasses" init-param or through the {@link
   * #addContextInitializers} property will be applied.</li>
   * <li>{@link ConfigurableApplicationContext#refresh refresh()} will be called</li>
   * </ul>
   * If the context has already been refreshed or does not implement
   * {@code ConfigurableApplicationContext}, none of the above will occur under the
   * assumption that the user has performed these actions (or not) per his or her
   * specific needs.
   *
   * @param applicationContext the context to use
   * @see #initApplicationContext
   * @see #configureAndRefreshApplicationContext
   */
  protected InfraHandler(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  @Override
  public void setBeanName(String name) {
    this.beanName = name;
  }

  /**
   * Set the {@code Environment} that this handler runs in.
   * <p>Any environment set here overrides the {@link StandardEnvironment}
   * provided by default.
   *
   * @throws IllegalArgumentException if environment is not assignable to
   * {@code ConfigurableEnvironment}
   */
  @Override
  public void setEnvironment(Environment environment) {
    Assert.isInstanceOf(ConfigurableEnvironment.class, environment, "ConfigurableEnvironment required");
    this.environment = (ConfigurableEnvironment) environment;
  }

  /**
   * Return the {@link Environment} associated with this handler.
   * <p>If none specified, a default environment will be initialized via
   * {@link #createEnvironment()}.
   */
  @Override
  public ConfigurableEnvironment getEnvironment() {
    if (this.environment == null) {
      this.environment = createEnvironment();
    }
    return this.environment;
  }

  /**
   * Create and return a new {@link StandardEnvironment}.
   * <p>Subclasses may override this in order to configure the environment or
   * specialize the environment type returned.
   */
  protected ConfigurableEnvironment createEnvironment() {
    return new StandardEnvironment();
  }

  /**
   * This method will be invoked after any bean properties have been set and
   * the ApplicationContext has been loaded. The default implementation is empty;
   * subclasses may override this method to perform any initialization they require.
   */
  protected void afterApplicationContextInit() {
  }

  /**
   * Initialize and publish the ApplicationContext for this handler.
   * <p>Delegates to {@link #createApplicationContext} for actual creation
   * of the context. Can be overridden in subclasses.
   *
   * @return the ApplicationContext instance
   * @see #InfraHandler(ApplicationContext)
   * @see #setContextClass
   * @see #setContextConfigLocation
   */
  protected ApplicationContext initApplicationContext() {
    ApplicationContext rootContext = getRootApplicationContext();

    ApplicationContext wac = applicationContext;
    if (wac != null) {
      // A context instance was injected at construction time -> use it
      if (wac instanceof ConfigurableApplicationContext cwac && !cwac.isActive()) {
        // The context has not yet been refreshed -> provide services such as
        // setting the parent context, setting the application context id, etc
        if (cwac.getParent() == null && rootContext != wac) {
          // The context instance was injected without an explicit parent -> set
          // the root application context (if any; may be null) as the parent
          cwac.setParent(rootContext);
        }
        configureAndRefreshApplicationContext(cwac);
      }
    }
    if (wac == null) {
      // No context instance was injected at construction time -> see if one
      // has been registered in the 'servlet' context. If one exists, it is assumed
      // that the parent context (if any) has already been set and that the
      // user has performed any initialization such as setting the context id
      wac = findApplicationContext();
    }
    if (wac == null) {
      // No context instance is defined for this handler -> create a local one
      wac = createApplicationContext(rootContext);
    }

    if (!refreshEventReceived) {
      // Either the context is not a ConfigurableApplicationContext with refresh
      // support or the context injected at construction time had already been
      // refreshed -> trigger initial onRefresh manually here.
      synchronized(onRefreshMonitor) {
        onRefresh(wac);
      }
    }

    return wac;
  }

  @Nullable
  protected ApplicationContext getRootApplicationContext() {
    return null;
  }

  protected void configureAndRefreshApplicationContext(ConfigurableApplicationContext context) {
    if (ObjectUtils.identityToString(context).equals(context.getId())) {
      // The application context id is still set to its original default value
      // -> assign a more useful id based on available information
      String contextId = getContextId();
      if (contextId != null) {
        context.setId(contextId);
      }
      else {
        // Generate default id...
        applyDefaultContextId(context);
      }
    }

    context.addApplicationListener(new SourceFilteringListener(context, new ContextRefreshListener()));
    postProcessApplicationContext(context);

    applyInitializers(context, contextInitializers);
    context.refresh();
  }

  protected void applyDefaultContextId(ConfigurableApplicationContext context) {
    context.setId("application");
  }

  /**
   * Retrieve a {@code ApplicationContext}
   *
   * @return the ApplicationContext for this handler, or {@code null} if not found
   */
  @Nullable
  protected ApplicationContext findApplicationContext() {
    return null;
  }

  /**
   * Instantiate the ApplicationContext for this handler, either a default
   * {@link ApplicationContext}
   * or a {@link #setContextClass custom context class}, if set.
   * <p>This implementation expects custom contexts to implement the
   * {@link infra.context.ConfigurableApplicationContext}
   * interface. Can be overridden in subclasses.
   * <p>Do not forget to register this handler instance as application listener on the
   * created context (for triggering its {@link #onRefresh callback}), and to call
   * {@link infra.context.ConfigurableApplicationContext#refresh()}
   * before returning the context instance.
   *
   * @param parent the parent ApplicationContext to use, or {@code null} if none
   * @return the ApplicationContext for this handler
   */
  protected ApplicationContext createApplicationContext(@Nullable ApplicationContext parent) {
    Class<?> contextClass = getContextClass();
    if (!ConfigurableApplicationContext.class.isAssignableFrom(contextClass)) {
      throw new ApplicationContextException(
              "Fatal initialization error: custom ApplicationContext class [%s] is not of type ConfigurableApplicationContext"
                      .formatted(contextClass.getName()));
    }
    ConfigurableApplicationContext context =
            (ConfigurableApplicationContext) BeanUtils.newInstance(contextClass);

    context.setEnvironment(getEnvironment());
    context.setParent(parent);

    String configLocation = getContextConfigLocation();
    if (configLocation != null && context instanceof AbstractRefreshableConfigApplicationContext cwac) {
      cwac.setConfigLocation(configLocation);
    }

    configureAndRefreshApplicationContext(context);

    return context;
  }

  /**
   * Post-process the given ApplicationContext before it is refreshed
   * and activated as context for this handler.
   * <p>The default implementation is empty. {@code refresh()} will
   * be called automatically after this method returns.
   * <p>Note that this method is designed to allow subclasses to modify the application
   * context, while {@link #initApplicationContext} is designed to allow
   * end-users to modify the context through the use of
   * {@link ApplicationContextInitializer ApplicationContextInitializers}.
   *
   * @param context the configured ApplicationContext (not refreshed yet)
   * @see #createApplicationContext
   * @see #initApplicationContext
   * @see ConfigurableApplicationContext#refresh()
   */
  protected void postProcessApplicationContext(ConfigurableApplicationContext context) {
  }

  /**
   * Delegate the ApplicationContext before it is refreshed to any
   * {@link ApplicationContextInitializer} instances.
   * <p>See also {@link #postProcessApplicationContext}, which is designed to allow
   * subclasses (as opposed to end-users) to modify the application context, and is
   * called immediately before this method.
   *
   * @param context the configured ApplicationContext (not refreshed yet)
   * @param initializers ApplicationContextInitializer list
   * @see #createApplicationContext
   * @see #postProcessApplicationContext
   * @see ConfigurableApplicationContext#refresh()
   */
  protected void applyInitializers(ConfigurableApplicationContext context, List<ApplicationContextInitializer> initializers) {
    AnnotationAwareOrderComparator.sort(initializers);
    for (ApplicationContextInitializer initializer : initializers) {
      initializer.initialize(context);
    }
  }

  /**
   * Refresh this handler's application context, as well as the
   * dependent state of the handler.
   *
   * @see #getApplicationContext()
   * @see infra.context.ConfigurableApplicationContext#refresh()
   */
  public void refresh() {
    ApplicationContext wac = getApplicationContext();
    if (!(wac instanceof ConfigurableApplicationContext cac)) {
      throw new IllegalStateException("ApplicationContext does not support refresh: " + wac);
    }
    cac.refresh();
  }

  /**
   * Template method which can be overridden to add infra-specific refresh work.
   * Called after successful context refresh.
   * <p>This implementation is empty.
   *
   * @param context the current ApplicationContext
   * @see #refresh()
   */
  protected void onRefresh(ApplicationContext context) {
    // For subclasses: do nothing by default.
  }

  /**
   * Destroy Application
   */
  public void destroy() {
    // Only call close() on ApplicationContext if locally managed...
    if (!this.applicationContextInjected) {
      var context = this.applicationContext;
      if (context != null) {
        ApplicationContext.State state = context.getState();
        if (state != ApplicationContext.State.CLOSING && state != ApplicationContext.State.CLOSED) {
          context.close();
          logInfo("Your application destroyed at: [%s] on startup date: [%s]"
                  .formatted(Instant.now(), context.getStartupDate()));
        }
      }
    }
  }

  /**
   * Log internal
   *
   * @param msg Log message
   */
  protected void logInfo(final String msg) {
    log.info(msg);
  }

  /**
   * Callback that receives refresh events from this handler's ApplicationContext.
   * <p>The default implementation calls {@link #onRefresh},
   * triggering a refresh of this handler's context-dependent state.
   *
   * @param event the incoming ApplicationContext event
   */
  public void onApplicationEvent(ContextRefreshedEvent event) {
    this.refreshEventReceived = true;
    synchronized(this.onRefreshMonitor) {
      onRefresh(event.getApplicationContext());
    }
  }

  /**
   * Called by Infra via {@link ApplicationContextAware} to inject the current
   * application context. This method allows DispatcherServlets to be registered as
   * Infra beans inside an existing {@link ApplicationContext} rather than
   * <p>Primarily added to support use in embedded handler containers.
   */
  @Override
  public void setApplicationContext(ApplicationContext applicationContext) {
    if (this.applicationContext == null) {
      this.applicationContext = applicationContext;
      this.applicationContextInjected = true;
    }
  }

  /**
   * Return this handler's ApplicationContext.
   */
  public final ApplicationContext getApplicationContext() {
    return this.applicationContext;
  }

  /**
   * Set a custom context class. This class must be of type
   * {@link ApplicationContext}.
   * <p>When using the default DispatcherHandler implementation,
   * the context class must also implement the
   * {@link ConfigurableApplicationContext} interface.
   *
   * @see #createApplicationContext
   */
  public void setContextClass(Class<?> contextClass) {
    this.contextClass = contextClass;
  }

  /**
   * Return the custom context class.
   */
  public Class<?> getContextClass() {
    return this.contextClass;
  }

  /**
   * Specify a custom ApplicationContext id,
   * to be used as serialization id for the underlying BeanFactory.
   */
  public void setContextId(@Nullable String contextId) {
    this.contextId = contextId;
  }

  /**
   * Return the custom ApplicationContext id, if any.
   */
  @Nullable
  public String getContextId() {
    return this.contextId;
  }

  /**
   * Set the context config location explicitly, instead of relying on the default
   * location built from the namespace. This location string can consist of
   * multiple locations separated by any number of commas and spaces.
   */
  public void setContextConfigLocation(@Nullable String contextConfigLocation) {
    this.contextConfigLocation = contextConfigLocation;
  }

  /**
   * Return the explicit context config location, if any.
   */
  @Nullable
  public String getContextConfigLocation() {
    return this.contextConfigLocation;
  }

  /**
   * Specify which {@link ApplicationContextInitializer} instances should be used
   * to initialize the application context used by this {@code InfraHandler}.
   *
   * @see #configureAndRefreshApplicationContext
   * @see #applyInitializers
   */
  public void addContextInitializers(@Nullable ApplicationContextInitializer... initializers) {
    CollectionUtils.addAll(contextInitializers, initializers);
  }

  /**
   * Whether to log request params at DEBUG level, and headers at TRACE level.
   * Both may contain sensitive information.
   * <p>By default, set to {@code false} so that request details are not shown.
   *
   * @param enable whether to enable or not
   */
  public void setEnableLoggingRequestDetails(boolean enable) {
    this.enableLoggingRequestDetails = enable;
  }

  /**
   * Whether logging of potentially sensitive, request details at DEBUG and
   * TRACE level is allowed.
   */
  public boolean isEnableLoggingRequestDetails() {
    return this.enableLoggingRequestDetails;
  }

  @Override
  public boolean isRunning() {
    return running.get();
  }

  @Override
  public void start() {
    if (running.compareAndSet(false, true)) {
      long startTime = System.currentTimeMillis();
      try {
        this.applicationContext = initApplicationContext();
        afterApplicationContextInit();
      }
      catch (Exception ex) {
        log.error("Context initialization failed", ex);
        throw ex;
      }

      if (log.isDebugEnabled()) {
        String value = isEnableLoggingRequestDetails()
                ? "shown which may lead to unsafe logging of potentially sensitive data"
                : "masked to prevent unsafe logging of potentially sensitive data";
        log.debug("enableLoggingRequestDetails='{}': request parameters and headers will be {}", isEnableLoggingRequestDetails(), value);
      }

      log.info("Completed initialization in {} ms", System.currentTimeMillis() - startTime);
    }
  }

  @Override
  public void stop() {
    destroy();
    running.set(false);
  }

  /**
   * ApplicationListener endpoint that receives events from this handler's ApplicationContext
   * only, delegating to {@code onApplicationEvent} on the DispatcherHandler instance.
   */
  private final class ContextRefreshListener implements ApplicationListener<ContextRefreshedEvent> {

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
      InfraHandler.this.onApplicationEvent(event);
    }
  }
}
