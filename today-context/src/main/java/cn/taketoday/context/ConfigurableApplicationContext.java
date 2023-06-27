/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

import cn.taketoday.beans.factory.config.BeanFactoryPostProcessor;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.io.ProtocolResolver;
import cn.taketoday.lang.Experimental;
import cn.taketoday.lang.Nullable;

/**
 * SPI interface to be implemented by most if not all application contexts.
 * Provides facilities to configure an application context in addition
 * to the application context client methods in the
 * {@link cn.taketoday.context.ApplicationContext} interface.
 *
 * <p>Configuration and lifecycle methods are encapsulated here to avoid
 * making them obvious to ApplicationContext client code. The present
 * methods should only be used by startup and shutdown code.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Sam Brannen
 * @author TODAY 2018-11-14 21:16
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
   * Get configurable environment
   *
   * @return {@link ConfigurableEnvironment} never be null
   * @since 2.1.0
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
   * @see cn.taketoday.core.io.DefaultResourceLoader#DefaultResourceLoader(ClassLoader)
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
   * Add a new ApplicationListener that will be notified on context events
   * such as context refresh and context shutdown.
   * <p>Note that any ApplicationListener registered here will be applied
   * on refresh if the context is not active yet, or on the fly with the
   * current event multicaster in case of a context that is already active.
   *
   * @param listener the ApplicationListener to register
   * @see cn.taketoday.context.event.ContextRefreshedEvent
   * @see cn.taketoday.context.event.ContextClosedEvent
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
  @Experimental
  BootstrapContext getBootstrapContext();

}
