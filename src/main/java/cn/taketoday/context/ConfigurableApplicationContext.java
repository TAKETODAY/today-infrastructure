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
package cn.taketoday.context;

import cn.taketoday.beans.factory.BeanFactoryPostProcessor;
import cn.taketoday.beans.factory.ConfigurableBeanFactory;
import cn.taketoday.context.event.ApplicationEventPublisher;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.io.ProtocolResolver;
import cn.taketoday.lang.Nullable;

/**
 * @author TODAY 2018-11-14 21:16
 */
public interface ConfigurableApplicationContext extends ApplicationContext {

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
   * @since 4.0
   */
  void setEventPublisher(ApplicationEventPublisher eventPublisher);

  /**
   * Set this context can refresh again
   * <p>
   * default is false
   * </p>
   *
   * @see cn.taketoday.context.ApplicationContext.State#STARTED
   * @see cn.taketoday.context.ApplicationContext.State#STARTING
   * @see cn.taketoday.context.ApplicationContext.State#CLOSING
   * @since 4.0
   */
  void setRefreshable(boolean refreshable);

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

}
