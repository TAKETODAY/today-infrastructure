/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

import java.io.Closeable;
import java.util.Collection;

import cn.taketoday.beans.factory.AutowireCapableBeanFactory;
import cn.taketoday.beans.factory.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.HierarchicalBeanFactory;
import cn.taketoday.context.event.ApplicationEventPublisher;
import cn.taketoday.context.loader.CandidateComponentScannerCapable;
import cn.taketoday.core.NonNull;
import cn.taketoday.core.Nullable;

/**
 * @author TODAY <br>
 * 2018-06-23 16:39:36
 */
public interface ApplicationContext
        extends Closeable, HierarchicalBeanFactory, ApplicationEventPublisher, CandidateComponentScannerCapable {

  /**
   * Get {@link Environment}
   *
   * @return {@link Environment}
   */
  Environment getEnvironment();

  /**
   * Get AbstractBeanFactory
   *
   * @return A bean factory
   *
   * @since 3.0
   */
  BeanFactory getBeanFactory();

  /**
   * unwrap bean-factory to {@code requiredType}
   *
   * @throws IllegalArgumentException
   *         not a requiredType
   * @see #getBeanFactory()
   * @since 4.0
   */
  @NonNull
  <T> T unwrapFactory(Class<T> requiredType);

  /**
   * unwrap this ApplicationContext to {@code requiredType}
   *
   * @throws IllegalArgumentException
   *         not a requiredType
   * @since 4.0
   */
  @NonNull
  <T> T unwrap(Class<T> requiredType);

  /**
   * Refresh factory, initialize singleton
   *
   * @since 2.0.1
   */
  void refresh() throws ApplicationContextException;

  /**
   * Load Application Context.
   *
   * <p>
   * First of all, it will load all the properties files in the given path. If you
   * use <b>""</b> instead of a exact path like <b>/config</b> ,it will load all
   * the properties files in the application.
   * </p>
   * <p>
   * And then locations parameter decided where to load the beans.
   * </p>
   * <p>
   * when all the bean definition stores in the {@link BeanDefinitionRegistry}.
   * then resolve dependency
   * </p>
   * <p>
   * Then It will find all the bean post processor,and initialize it. Last refresh
   * context.
   * </p>
   *
   * @param locations
   *         packages to scan
   *
   * @since 3.0
   */
  void load(String... locations);

  /**
   * load context from given classes
   *
   * @param candidates
   *         class set
   *
   * @since 3.0
   */
  void load(Collection<Class<?>> candidates);

  /**
   * Close context and destroy all singletons
   */
  @Override
  void close();

  /**
   * Context has started
   *
   * @return is started
   */
  boolean hasStarted();

  /**
   * Get the context startup time stamp
   *
   * @return startup timestamp
   */
  long getStartupDate();

  /**
   * Get context's state
   *
   * @return context's state
   *
   * @since 2.1.5
   */
  State getState();

  /**
   * Return the unique id of this application context.
   *
   * @return the unique id of the context, or {@code null} if none
   *
   * @since 4.0
   */
  @Nullable
  String getId();

  /**
   * Return a name for the deployed application that this context belongs to.
   *
   * @return a name for the deployed application, or the empty String by default
   *
   * @since 4.0
   */
  String getApplicationName();

  /**
   * Return a friendly name for this context.
   *
   * @return a display name for this context (never {@code null})
   *
   * @since 4.0
   */
  String getDisplayName();

  /**
   * Return the parent context, or {@code null} if there is no parent
   * and this is the root of the context hierarchy.
   *
   * @return the parent context, or {@code null} if there is no parent
   *
   * @since 4.0
   */
  @Nullable
  ApplicationContext getParent();

  /**
   * Expose AutowireCapableBeanFactory functionality for this context.
   * <p>This is not typically used by application code, except for the purpose of
   * initializing bean instances that live outside of the application context,
   * applying the Spring bean lifecycle (fully or partly) to them.
   * <p>Alternatively, the internal BeanFactory exposed by the
   * {@link ConfigurableApplicationContext} interface offers access to the
   * {@link AutowireCapableBeanFactory} interface too. The present method mainly
   * serves as a convenient, specific facility on the ApplicationContext interface.
   * <p><b>NOTE: this method will consistently throw IllegalStateException
   * after the application context has been closed.</b> In current Spring Framework
   * versions, only refreshable application contexts behave that way; as of 4.2,
   * all application context implementations will be required to comply.
   *
   * @return the AutowireCapableBeanFactory for this context
   *
   * @throws IllegalStateException
   *         if the context does not support the
   *         {@link AutowireCapableBeanFactory} interface, or does not hold an
   *         autowire-capable bean factory yet (e.g. if {@code refresh()} has
   *         never been called), or if the context has been closed already
   * @see ConfigurableApplicationContext#refresh()
   * @see ConfigurableApplicationContext#getBeanFactory()
   */
  AutowireCapableBeanFactory getAutowireCapableBeanFactory() throws IllegalStateException;

  enum State {

    /** context instantiated */
    NONE,
    /** context is loading */
    STARTING,
    /** context is started */
    STARTED,
    /** context failed to start */
    FAILED,
    /** context is closing */
    CLOSING,
    /** context is closed */
    CLOSED;
  }

}
