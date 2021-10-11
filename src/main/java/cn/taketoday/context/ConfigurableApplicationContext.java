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
import cn.taketoday.lang.Nullable;
import cn.taketoday.core.env.ConfigurableEnvironment;

/**
 * @author TODAY 2018-11-14 21:16
 */
public interface ConfigurableApplicationContext extends ApplicationContext {

  /**
   * Get configurable environment
   *
   * @return {@link ConfigurableEnvironment} never be null
   *
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
   * @param postProcessor
   *         the factory processor to register
   *
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
   * @param parent
   *         the parent context
   *
   * @see cn.taketoday.web.ConfigurableWebApplicationContext
   * @since 4.0
   */
  void setParent(@Nullable ApplicationContext parent);

  /**
   * Set the {@code Environment} for this application context.
   *
   * @param environment
   *         the new environment
   *
   * @since 4.0
   */
  void setEnvironment(ConfigurableEnvironment environment);

}
