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

package infra.context;

import org.jspecify.annotations.Nullable;

import java.util.concurrent.Executor;

import infra.beans.factory.config.BeanFactoryPostProcessor;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.context.event.ContextClosedEvent;
import infra.context.event.ContextRefreshedEvent;
import infra.core.conversion.ConversionService;
import infra.core.env.ConfigurableEnvironment;
import infra.core.io.DefaultResourceLoader;
import infra.core.io.ProtocolResolver;
import infra.core.task.TaskExecutor;

/**
 * SPI interface to be implemented by most if not all application contexts.
 * Provides facilities to configure an application context in addition
 * to the application context client methods in the
 * {@link infra.context.ApplicationContext} interface.
 *
 * <p>Configuration and lifecycle methods are encapsulated here to avoid
 * making them obvious to ApplicationContext client code. The present
 * methods should only be used by startup and shutdown code.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-11-14 21:16
 */
public interface ConfigurableApplicationContext extends ApplicationContext, Lifecycle, AutoCloseable {

  /**
   * Any number of these characters are considered delimiters between
   * multiple context config paths in a single String value.
   */
  String CONFIG_LOCATION_DELIMITERS = ",; \t\n";

  /**
   * Name of the ConversionService bean in the factory.
   * If none is supplied, default conversion rules apply.
   *
   * @see ConversionService
   * @since 4.0
   */
  String CONVERSION_SERVICE_BEAN_NAME = "conversionService";

  /**
   * Name of the LoadTimeWeaver bean in the factory. If such a bean is supplied,
   * the context will use a temporary ClassLoader for type matching, in order
   * to allow the LoadTimeWeaver to process all actual bean classes.
   *
   * @since 4.0
   */
  String LOAD_TIME_WEAVER_BEAN_NAME = "loadTimeWeaver";

  /**
   * {@link Thread#getName() Name} of the {@linkplain #registerShutdownHook()
   * shutdown hook} thread: {@value}.
   *
   * @see #registerShutdownHook()
   * @since 4.0
   */
  String SHUTDOWN_HOOK_THREAD_NAME = "ContextShutdownHook";

  /**
   * The name of the {@link Executor bootstrap executor} bean in the context.
   * If none is supplied, no background bootstrapping will be active.
   *
   * @see java.util.concurrent.Executor
   * @see TaskExecutor
   * @see ConfigurableBeanFactory#setBootstrapExecutor
   * @since 4.0
   */
  String BOOTSTRAP_EXECUTOR_BEAN_NAME = "bootstrapExecutor";

  /**
   * Get configurable environment
   *
   * @return {@link ConfigurableEnvironment} never be null
   * @since 2.1
   */
  @Override
  ConfigurableEnvironment getEnvironment();

  /**
   * Get AbstractBeanFactory
   *
   * @return A bean factory
   */
  @Override
  ConfigurableBeanFactory getBeanFactory();

  /**
   * Add a new BeanFactoryPostProcessor that will get applied to the internal bean
   * factory of this application context on refresh, before any of the bean
   * definitions get evaluated. To be invoked during context configuration.
   *
   * @param postProcessor the factory processor to register
   * @since 2.1.7
   */
  void addBeanFactoryPostProcessor(BeanFactoryPostProcessor postProcessor);

  /**
   * Set the unique id of this application context.
   *
   * @since 4.0
   */
  void setId(String id);

  /**
   * Set the parent of this application context.
   * <p>Note that the parent shouldn't be changed: It should only be set outside
   * a constructor if it isn't available when an object of this class is created,
   * for example in case of WebApplicationContext setup.
   *
   * @param parent the parent context
   * @since 4.0
   */
  void setParent(@Nullable ApplicationContext parent);

  /**
   * Set the {@code Environment} for this application context.
   *
   * @param environment the new environment
   * @since 4.0
   */
  void setEnvironment(ConfigurableEnvironment environment);

  /**
   * Register the given protocol resolver with this application context,
   * allowing for additional resource protocols to be handled.
   * <p>Any such resolver will be invoked ahead of this context's standard
   * resolution rules. It may therefore also override any default rules.
   *
   * @since 4.0
   */
  void addProtocolResolver(ProtocolResolver resolver);

  /**
   * Specify the ClassLoader to load class path resources and bean classes with.
   * <p>This context class loader will be passed to the internal bean factory.
   *
   * @see DefaultResourceLoader#DefaultResourceLoader(ClassLoader)
   * @since 4.0
   */
  void setClassLoader(ClassLoader classLoader);

  /**
   * Load or refresh the persistent representation of the configuration, which
   * might be from Java-based configuration or some other format.
   * <p>As this is a startup method, it should destroy already created singletons
   * if it fails, to avoid dangling resources. In other words, after invocation
   * of this method, either all or no singletons at all should be instantiated.
   *
   * @throws ApplicationContextException if the bean factory could not be initialized
   * @throws IllegalStateException if already initialized and multiple refresh
   * attempts are not supported
   * @since 2.0.1
   */
  void refresh() throws ApplicationContextException;

  /**
   * Pause all beans in this application context if necessary, and subsequently
   * restart all auto-startup beans, effectively restoring the lifecycle state
   * after {@link #refresh()} (typically after a preceding {@link #pause()} call
   * when a full {@link #start()} of even lazy-starting beans is to be avoided).
   *
   * @see #pause()
   * @see #start()
   * @see SmartLifecycle#isAutoStartup()
   * @since 5.0
   */
  void restart();

  /**
   * Stop all beans in this application context unless they explicitly opt out of
   * pausing through {@link SmartLifecycle#isPausable()} returning {@code false}.
   *
   * @see #restart()
   * @see #stop()
   * @see SmartLifecycle#isPausable()
   * @since 5.0
   */
  void pause();

  /**
   * Register a shutdown hook with the JVM runtime, closing this context
   * on JVM shutdown unless it has already been closed at that time.
   * <p>This method can be called multiple times. Only one shutdown hook
   * (at max) will be registered for each context instance.
   * <p>The {@linkplain Thread#getName() name} of the shutdown hook thread
   * should be {@link #SHUTDOWN_HOOK_THREAD_NAME}.
   *
   * @see java.lang.Runtime#addShutdownHook
   * @see #close()
   */
  void registerShutdownHook();

  /**
   * Close this application context, releasing all resources and locks that the
   * implementation might hold. This includes destroying all cached singleton beans.
   * <p>Note: Does <i>not</i> invoke {@code close} on a parent context;
   * parent contexts have their own, independent lifecycle.
   * <p>This method can be called multiple times without side effects: Subsequent
   * {@code close} calls on an already closed context will be ignored.
   */
  @Override
  void close();

  /**
   * Return whether this context has been closed already, that is,
   * whether {@link #close()} has been called on an active context
   * in order to initiate its shutdown.
   * <p>Note: This does not indicate whether context shutdown has completed.
   * Use {@link #isActive()} for differentiating between those scenarios:
   * a context becomes inactive once it has been fully shut down and the
   * original {@code close()} call has returned.
   *
   * @since 5.0
   */
  boolean isClosed();

  /**
   * Add a new ApplicationListener that will be notified on context events
   * such as context refresh and context shutdown.
   * <p>Note that any ApplicationListener registered here will be applied
   * on refresh if the context is not active yet, or on the fly with the
   * current event multicaster in case of a context that is already active.
   *
   * @param listener the ApplicationListener to register
   * @see ContextRefreshedEvent
   * @see ContextClosedEvent
   * @since 4.0
   */
  void addApplicationListener(ApplicationListener<?> listener);

  /**
   * Remove the given ApplicationListener from this context's set of listeners,
   * assuming it got registered via {@link #addApplicationListener} before.
   *
   * @param listener the ApplicationListener to deregister
   * @since 4.0
   */
  void removeApplicationListener(ApplicationListener<?> listener);

  /**
   * Determine whether this application context is active, that is,
   * whether it has been refreshed at least once and has not been closed yet.
   *
   * @return whether the context is still active
   * @see #refresh()
   * @see #close()
   * @see #getBeanFactory()
   * @since 4.0
   */
  boolean isActive();

  /**
   * Returns BootstrapContext
   *
   * @return Returns BootstrapContext
   * @since 4.0
   */
  BootstrapContext getBootstrapContext();

}
