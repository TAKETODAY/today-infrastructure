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

import cn.taketoday.context.env.ConfigurableEnvironment;
import cn.taketoday.context.env.Environment;
import cn.taketoday.context.factory.BeanFactoryPostProcessor;
import cn.taketoday.context.factory.ConfigurableBeanFactory;
import cn.taketoday.context.listener.ApplicationListener;
import cn.taketoday.context.loader.CandidateComponentScanner;

/**
 * @author TODAY <br>
 *         2018-11-14 21:16
 */
public interface ConfigurableApplicationContext extends ApplicationContext {

  /**
   * Setting the {@link Environment}
   *
   * @param environment
   *            {@link Environment} instance
   * @since 2.1.0
   */
  @Deprecated
  void setEnvironment(ConfigurableEnvironment environment);

  /**
   * Get configurable environment
   *
   * @since 2.1.0
   * @return {@link ConfigurableEnvironment} never be null
   */
  @Override
  ConfigurableEnvironment getEnvironment();

  /**
   * Get AbstractBeanFactory
   *
   * @return A bean factory
   */
  ConfigurableBeanFactory getBeanFactory();

  /**
   * Add an {@link ApplicationListener} that will be notified on context events
   * such as context refresh and context shutdown.
   * <p>
   *
   * @param listener
   *            the {@link ApplicationListener}
   * @since 2.1.6
   */
  void addApplicationListener(ApplicationListener<?> listener);

  /**
   * Apply {@link CandidateComponentScanner} to scan classes
   *
   * @param scanner
   *            CandidateComponentScanner
   * @since 2.1.7
   */
  void setCandidateComponentScanner(CandidateComponentScanner scanner);

  /**
   * Add a new BeanFactoryPostProcessor that will get applied to the internal bean
   * factory of this application context on refresh, before any of the bean
   * definitions get evaluated. To be invoked during context configuration.
   *
   * @param postProcessor
   *            the factory processor to register
   * @since 2.1.7
   */
  void addBeanFactoryPostProcessor(BeanFactoryPostProcessor postProcessor);

}
