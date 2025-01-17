/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.web.mock;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import infra.beans.BeanUtils;
import infra.context.ApplicationContext;
import infra.context.ApplicationContextException;
import infra.context.ApplicationContextInitializer;
import infra.context.ConfigurableApplicationContext;
import infra.core.Ordered;
import infra.core.annotation.AnnotationAwareOrderComparator;
import infra.core.annotation.Order;
import infra.core.env.ConfigurableEnvironment;
import infra.core.io.ClassPathResource;
import infra.core.io.PropertiesUtils;
import infra.lang.Nullable;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.mock.api.MockContext;
import infra.util.ClassUtils;
import infra.util.ObjectUtils;
import infra.util.StringUtils;
import infra.web.mock.support.XmlWebApplicationContext;

/**
 * Performs the actual initialization work for the root application context.
 *
 * <p>Looks for a {@link #CONTEXT_CLASS_PARAM "contextClass"} parameter at the
 * {@code web.xml} context-param level to specify the context class type, falling
 * back to {@link XmlWebApplicationContext}
 * if not found. With the default ContextLoader implementation, any context class
 * specified needs to implement the {@link ConfigurableWebApplicationContext} interface.
 *
 * <p>Processes a {@link #CONFIG_LOCATION_PARAM "contextConfigLocation"} context-param
 * and passes its value to the context instance, parsing it into potentially multiple
 * file paths which can be separated by any number of commas and spaces, e.g.
 * "WEB-INF/applicationContext1.xml, WEB-INF/applicationContext2.xml".
 * Ant-style path patterns are supported as well, e.g.
 * "WEB-INF/*Context.xml,WEB-INF/spring*.xml" or "WEB-INF/&#42;&#42;/*Context.xml".
 * If not explicitly specified, the context implementation is supposed to use a
 * default location (with XmlWebApplicationContext: "/WEB-INF/applicationContext.xml").
 *
 * <p>Note: In case of multiple config locations, later bean definitions will
 * override ones defined in previously loaded files, at least when using one of
 * Framework's default ApplicationContext implementations. This can be leveraged
 * to deliberately override certain bean definitions via an extra XML file.
 *
 * <p>Above and beyond loading the root application context, this class can optionally
 * load or obtain and hook up a shared parent context to the root application context.
 * See the {@link #loadParentContext(MockContext)} method for more information.
 *
 * <p>{@code ContextLoader} supports injecting the root web
 * application context via the {@link #ContextLoader(WebApplicationContext)}
 * constructor, allowing for programmatic configuration in Servlet initializers.
 * See {@link infra.context.ApplicationContextInitializer} for usage examples.
 *
 * @author Juergen Hoeller
 * @author Colin Sampaleanu
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ContextLoader {

  /**
   * Config param for the root WebApplicationContext id,
   * to be used as serialization id for the underlying BeanFactory: {@value}.
   */
  public static final String CONTEXT_ID_PARAM = "contextId";

  /**
   * Name of servlet context parameter (i.e., {@value}) that can specify the
   * config location for the root context, falling back to the implementation's
   * default otherwise.
   *
   * @see XmlWebApplicationContext#DEFAULT_CONFIG_LOCATION
   */
  public static final String CONFIG_LOCATION_PARAM = "contextConfigLocation";

  /**
   * Config param for the root WebApplicationContext implementation class to use: {@value}.
   *
   * @see #determineContextClass(MockContext)
   */
  public static final String CONTEXT_CLASS_PARAM = "contextClass";

  /**
   * Config param for {@link ApplicationContextInitializer} classes to use
   * for initializing the root web application context: {@value}.
   *
   * @see #customizeContext(MockContext, ConfigurableWebApplicationContext)
   */
  public static final String CONTEXT_INITIALIZER_CLASSES_PARAM = "contextInitializerClasses";

  /**
   * Config param for global {@link ApplicationContextInitializer} classes to use
   * for initializing all web application contexts in the current application: {@value}.
   *
   * @see #customizeContext(MockContext, ConfigurableWebApplicationContext)
   */
  public static final String GLOBAL_INITIALIZER_CLASSES_PARAM = "globalInitializerClasses";

  /**
   * Any number of these characters are considered delimiters between
   * multiple values in a single init-param String value.
   */
  private static final String INIT_PARAM_DELIMITERS = ",; \t\n";

  /**
   * Name of the class path resource (relative to the ContextLoader class)
   * that defines ContextLoader's default strategy names.
   */
  private static final String DEFAULT_STRATEGIES_PATH = "ContextLoader.properties";

  @Nullable
  private static Properties defaultStrategies;

  /**
   * Map from (thread context) ClassLoader to corresponding 'current' WebApplicationContext.
   */
  private static final Map<ClassLoader, WebApplicationContext> currentContextPerThread =
          new ConcurrentHashMap<>(1);

  /**
   * The 'current' WebApplicationContext, if the ContextLoader class is
   * deployed in the web app ClassLoader itself.
   */
  @Nullable
  private static volatile WebApplicationContext currentContext;

  /**
   * The root WebApplicationContext instance that this loader manages.
   */
  @Nullable
  private WebApplicationContext context;

  /** Actual ApplicationContextInitializer instances to apply to the context. */
  private final List<ApplicationContextInitializer> contextInitializers =
          new ArrayList<>();

  /**
   * Create a new {@code ContextLoader} that will create a web application context
   * based on the "contextClass" and "contextConfigLocation" servlet context-params.
   * See class-level documentation for details on default values for each.
   * <p>This constructor is typically used when declaring the {@code
   * ContextLoaderListener} subclass as a {@code <listener>} within {@code web.xml}, as
   * a no-arg constructor is required.
   * <p>The created application context will be registered into the MockContext under
   * the attribute name {@link WebApplicationContext#ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE}
   * and subclasses are free to call the {@link #closeWebApplicationContext} method on
   * container shutdown to close the application context.
   *
   * @see #ContextLoader(WebApplicationContext)
   * @see #initWebApplicationContext(MockContext)
   * @see #closeWebApplicationContext(MockContext)
   */
  public ContextLoader() { }

  /**
   * Create a new {@code ContextLoader} with the given application context. This
   * constructor is useful in Servlet initializers where instance-based registration
   * of listeners is possible through the  MockContext#addListener API.
   * <p>The context may or may not yet be {@linkplain
   * ConfigurableApplicationContext#refresh() refreshed}. If it (a) is an implementation
   * of {@link ConfigurableWebApplicationContext} and (b) has <strong>not</strong>
   * already been refreshed (the recommended approach), then the following will occur:
   * <ul>
   * <li>If the given context has not already been assigned an {@linkplain
   * ConfigurableApplicationContext#setId id}, one will be assigned to it</li>
   * <li>{@code MockContext} and {@code ServletConfig} objects will be delegated to
   * the application context</li>
   * <li>{@link #customizeContext} will be called</li>
   * <li>Any {@link ApplicationContextInitializer ApplicationContextInitializers} specified through the
   * "contextInitializerClasses" init-param will be applied.</li>
   * <li>{@link ConfigurableApplicationContext#refresh refresh()} will be called</li>
   * </ul>
   * If the context has already been refreshed or does not implement
   * {@code ConfigurableWebApplicationContext}, none of the above will occur under the
   * assumption that the user has performed these actions (or not) per his or her
   * specific needs.
   * <p>See {@link infra.context.ApplicationContextInitializer} for usage examples.
   * <p>In any case, the given application context will be registered into the
   * MockContext under the attribute name {@link
   * WebApplicationContext#ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE} and subclasses are
   * free to call the {@link #closeWebApplicationContext} method on container shutdown
   * to close the application context.
   *
   * @param context the application context to manage
   * @see #initWebApplicationContext(MockContext)
   * @see #closeWebApplicationContext(MockContext)
   */
  public ContextLoader(@Nullable WebApplicationContext context) {
    this.context = context;
  }

  /**
   * Specify which {@link ApplicationContextInitializer} instances should be used
   * to initialize the application context used by this {@code ContextLoader}.
   *
   * @see #configureAndRefreshWebApplicationContext
   * @see #customizeContext
   */
  public void setContextInitializers(@Nullable ApplicationContextInitializer... initializers) {
    if (initializers != null) {
      Collections.addAll(this.contextInitializers, initializers);
    }
  }

  /**
   * Initialize Framework's web application context for the given servlet context,
   * using the application context provided at construction time, or creating a new one
   * according to the "{@link #CONTEXT_CLASS_PARAM contextClass}" and
   * "{@link #CONFIG_LOCATION_PARAM contextConfigLocation}" context-params.
   *
   * @param mockContext current servlet context
   * @return the new WebApplicationContext
   * @see #ContextLoader(WebApplicationContext)
   * @see #CONTEXT_CLASS_PARAM
   * @see #CONFIG_LOCATION_PARAM
   */
  public WebApplicationContext initWebApplicationContext(MockContext mockContext) {
    if (mockContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE) != null) {
      throw new IllegalStateException(
              "Cannot initialize context because there is already a root application context present - " +
                      "check whether you have multiple ContextLoader* definitions in your web.xml!");
    }

    mockContext.log("Initializing Infra root WebApplicationContext");
    Logger logger = LoggerFactory.getLogger(ContextLoader.class);
    if (logger.isInfoEnabled()) {
      logger.info("Root WebApplicationContext: initialization started");
    }
    long startTime = System.currentTimeMillis();

    try {
      // Store context in local instance variable, to guarantee that
      // it is available on MockContext shutdown.
      if (this.context == null) {
        this.context = createWebApplicationContext(mockContext);
      }
      if (this.context instanceof ConfigurableWebApplicationContext cwac && !cwac.isActive()) {
        // The context has not yet been refreshed -> provide services such as
        // setting the parent context, setting the application context id, etc
        if (cwac.getParent() == null) {
          // The context instance was injected without an explicit parent ->
          // determine parent for root web application context, if any.
          ApplicationContext parent = loadParentContext(mockContext);
          cwac.setParent(parent);
        }
        configureAndRefreshWebApplicationContext(cwac, mockContext);
      }
      mockContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, this.context);

      ClassLoader ccl = Thread.currentThread().getContextClassLoader();
      if (ccl == ContextLoader.class.getClassLoader()) {
        currentContext = this.context;
      }
      else if (ccl != null) {
        currentContextPerThread.put(ccl, this.context);
      }

      if (logger.isInfoEnabled()) {
        long elapsedTime = System.currentTimeMillis() - startTime;
        logger.info("Root WebApplicationContext initialized in {} ms", elapsedTime);
      }

      return this.context;
    }
    catch (RuntimeException | Error ex) {
      logger.error("Context initialization failed", ex);
      mockContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, ex);
      throw ex;
    }
  }

  /**
   * Instantiate the root WebApplicationContext for this loader, either the
   * default context class or a custom context class if specified.
   * <p>This implementation expects custom contexts to implement the
   * {@link ConfigurableWebApplicationContext} interface.
   * Can be overridden in subclasses.
   * <p>In addition, {@link #customizeContext} gets called prior to refreshing the
   * context, allowing subclasses to perform custom modifications to the context.
   *
   * @param sc current servlet context
   * @return the root WebApplicationContext
   * @see ConfigurableWebApplicationContext
   */
  protected WebApplicationContext createWebApplicationContext(MockContext sc) {
    Class<?> contextClass = determineContextClass(sc);
    if (!ConfigurableWebApplicationContext.class.isAssignableFrom(contextClass)) {
      throw new ApplicationContextException("Custom context class [" + contextClass.getName() +
              "] is not of type [" + ConfigurableWebApplicationContext.class.getName() + "]");
    }
    return (ConfigurableWebApplicationContext) BeanUtils.newInstance(contextClass);
  }

  /**
   * Return the WebApplicationContext implementation class to use, either the
   * default XmlWebApplicationContext or a custom context class if specified.
   *
   * @param mockContext current servlet context
   * @return the WebApplicationContext implementation class to use
   * @see #CONTEXT_CLASS_PARAM
   */
  protected Class<?> determineContextClass(MockContext mockContext) {
    String contextClassName = mockContext.getInitParameter(CONTEXT_CLASS_PARAM);
    if (contextClassName != null) {
      try {
        return ClassUtils.forName(contextClassName, ClassUtils.getDefaultClassLoader());
      }
      catch (ClassNotFoundException ex) {
        throw new ApplicationContextException(
                "Failed to load custom context class [" + contextClassName + "]", ex);
      }
    }
    else {
      if (defaultStrategies == null) {
        // Load default strategy implementations from properties file.
        // This is currently strictly internal and not meant to be customized
        // by application developers.
        try {
          ClassPathResource resource = new ClassPathResource(DEFAULT_STRATEGIES_PATH, ContextLoader.class);
          defaultStrategies = PropertiesUtils.loadProperties(resource);
        }
        catch (IOException ex) {
          throw new IllegalStateException("Could not load 'ContextLoader.properties': " + ex.getMessage());
        }
      }
      contextClassName = defaultStrategies.getProperty(WebApplicationContext.class.getName());
      try {
        return ClassUtils.forName(contextClassName, ContextLoader.class.getClassLoader());
      }
      catch (ClassNotFoundException ex) {
        throw new ApplicationContextException(
                "Failed to load default context class [" + contextClassName + "]", ex);
      }
    }
  }

  protected void configureAndRefreshWebApplicationContext(ConfigurableWebApplicationContext wac, MockContext sc) {
    if (ObjectUtils.identityToString(wac).equals(wac.getId())) {
      // The application context id is still set to its original default value
      // -> assign a more useful id based on available information
      String idParam = sc.getInitParameter(CONTEXT_ID_PARAM);
      if (idParam != null) {
        wac.setId(idParam);
      }
      else {
        // Generate default id...
        wac.setId(MockDispatcher.APPLICATION_CONTEXT_ID_PREFIX + ObjectUtils.getDisplayString(sc));
      }
    }

    wac.setMockContext(sc);
    String configLocationParam = sc.getInitParameter(CONFIG_LOCATION_PARAM);
    if (configLocationParam != null) {
      wac.setConfigLocation(configLocationParam);
    }

    // The wac environment's #initPropertySources will be called in any case when the context
    // is refreshed; do it eagerly here to ensure servlet property sources are in place for
    // use in any post-processing or initialization that occurs below prior to #refresh
    ConfigurableEnvironment env = wac.getEnvironment();
    if (env instanceof ConfigurableWebEnvironment cwe) {
      cwe.initPropertySources(sc, null);
    }

    customizeContext(sc, wac);
    wac.refresh();
  }

  /**
   * Customize the {@link ConfigurableWebApplicationContext} created by this
   * ContextLoader after config locations have been supplied to the context
   * but before the context is <em>refreshed</em>.
   * <p>The default implementation {@linkplain #determineContextInitializerClasses(MockContext)
   * determines} what (if any) context initializer classes have been specified through
   * {@linkplain #CONTEXT_INITIALIZER_CLASSES_PARAM context init parameters} and
   * {@linkplain ApplicationContextInitializer#initialize invokes each} with the
   * given web application context.
   * <p>Any {@code ApplicationContextInitializers} implementing
   * {@link Ordered Ordered} or marked with @{@link
   * Order Order} will be sorted appropriately.
   *
   * @param sc the current servlet context
   * @param wac the newly created application context
   * @see #CONTEXT_INITIALIZER_CLASSES_PARAM
   * @see ApplicationContextInitializer#initialize(ConfigurableApplicationContext)
   */
  protected void customizeContext(MockContext sc, ConfigurableWebApplicationContext wac) {
    List<Class<ApplicationContextInitializer>> initializerClasses =
            determineContextInitializerClasses(sc);

    for (Class<ApplicationContextInitializer> initializerClass : initializerClasses) {
      contextInitializers.add(BeanUtils.newInstance(initializerClass));
    }

    AnnotationAwareOrderComparator.sort(contextInitializers);
    for (ApplicationContextInitializer initializer : contextInitializers) {
      initializer.initialize(wac);
    }
  }

  /**
   * Return the {@link ApplicationContextInitializer} implementation classes to use
   * if any have been specified by {@link #CONTEXT_INITIALIZER_CLASSES_PARAM}.
   *
   * @param mockContext current servlet context
   */
  protected List<Class<ApplicationContextInitializer>> determineContextInitializerClasses(MockContext mockContext) {

    List<Class<ApplicationContextInitializer>> classes =
            new ArrayList<>();

    String globalClassNames = mockContext.getInitParameter(GLOBAL_INITIALIZER_CLASSES_PARAM);
    if (globalClassNames != null) {
      for (String className : StringUtils.tokenizeToStringArray(globalClassNames, INIT_PARAM_DELIMITERS)) {
        classes.add(loadInitializerClass(className));
      }
    }

    String localClassNames = mockContext.getInitParameter(CONTEXT_INITIALIZER_CLASSES_PARAM);
    if (localClassNames != null) {
      for (String className : StringUtils.tokenizeToStringArray(localClassNames, INIT_PARAM_DELIMITERS)) {
        classes.add(loadInitializerClass(className));
      }
    }

    return classes;
  }

  @SuppressWarnings("unchecked")
  private Class<ApplicationContextInitializer> loadInitializerClass(String className) {
    try {
      Class<?> clazz = ClassUtils.forName(className, ClassUtils.getDefaultClassLoader());
      if (!ApplicationContextInitializer.class.isAssignableFrom(clazz)) {
        throw new ApplicationContextException(
                "Initializer class does not implement ApplicationContextInitializer interface: " + clazz);
      }
      return (Class<ApplicationContextInitializer>) clazz;
    }
    catch (ClassNotFoundException ex) {
      throw new ApplicationContextException("Failed to load context initializer class [" + className + "]", ex);
    }
  }

  /**
   * Template method with default implementation (which may be overridden by a
   * subclass), to load or obtain an ApplicationContext instance which will be
   * used as the parent context of the root WebApplicationContext. If the
   * return value from the method is null, no parent context is set.
   * <p>The main reason to load a parent context here is to allow multiple root
   * web application contexts to all be children of a shared EAR context, or
   * alternately to also share the same parent context that is visible to
   * EJBs. For pure web applications, there is usually no need to worry about
   * having a parent context to the root web application context.
   * <p>The default implementation simply returns {@code null}, as of 5.0.
   *
   * @param mockContext current servlet context
   * @return the parent application context, or {@code null} if none
   */
  @Nullable
  protected ApplicationContext loadParentContext(MockContext mockContext) {
    return null;
  }

  /**
   * Close Framework's web application context for the given servlet context.
   * <p>If overriding {@link #loadParentContext(MockContext)}, you may have
   * to override this method as well.
   *
   * @param mockContext the MockContext that the WebApplicationContext runs in
   */
  public void closeWebApplicationContext(MockContext mockContext) {
    mockContext.log("Closing Framework root WebApplicationContext");
    try {
      if (this.context instanceof ConfigurableWebApplicationContext cwac) {
        cwac.close();
      }
    }
    finally {
      ClassLoader ccl = Thread.currentThread().getContextClassLoader();
      if (ccl == ContextLoader.class.getClassLoader()) {
        currentContext = null;
      }
      else if (ccl != null) {
        currentContextPerThread.remove(ccl);
      }
      mockContext.removeAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
    }
  }

  /**
   * Obtain the Framework root web application context for the current thread
   * (i.e. for the current thread's context ClassLoader, which needs to be
   * the web application's ClassLoader).
   *
   * @return the current root web application context, or {@code null}
   * if none found
   */
  @Nullable
  public static WebApplicationContext getCurrentWebApplicationContext() {
    ClassLoader ccl = Thread.currentThread().getContextClassLoader();
    if (ccl != null) {
      WebApplicationContext ccpt = currentContextPerThread.get(ccl);
      if (ccpt != null) {
        return ccpt;
      }
    }
    return currentContext;
  }

}
