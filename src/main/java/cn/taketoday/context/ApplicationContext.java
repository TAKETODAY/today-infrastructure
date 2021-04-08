/**
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

import cn.taketoday.context.env.Environment;
import cn.taketoday.context.event.ApplicationEventPublisher;
import cn.taketoday.context.exception.ContextException;
import cn.taketoday.context.factory.AutowireCapableBeanFactory;
import cn.taketoday.context.factory.BeanDefinitionRegistry;
import cn.taketoday.context.factory.BeanFactory;
import cn.taketoday.context.factory.ConfigurableBeanFactory;
import cn.taketoday.context.loader.CandidateComponentScannerCapable;

/**
 * @author TODAY <br>
 * 2018-06-23 16:39:36
 */
public interface ApplicationContext
        extends Closeable, ConfigurableBeanFactory, ApplicationEventPublisher, AutowireCapableBeanFactory,
                CandidateComponentScannerCapable {

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
   * Refresh factory, initialize singleton
   *
   * @since 2.0.1
   */
  void refresh() throws ContextException;

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
   * @deprecated use {@link #load(String...)}
   */
  @Deprecated
  void loadContext(String... locations);

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
   * @since 2.1.2
   * @deprecated use {@link #load(Collection)}
   */
  @Deprecated
  void loadContext(Collection<Class<?>> candidates);

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
